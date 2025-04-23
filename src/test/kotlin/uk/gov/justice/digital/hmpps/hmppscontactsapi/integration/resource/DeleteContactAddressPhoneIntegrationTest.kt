package uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.SecureAPIIntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.CreateContactRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.address.CreateContactAddressRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.phone.CreateContactAddressPhoneRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.ContactAddressPhoneInfo
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.PersonReference
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.Source
import uk.gov.justice.digital.hmpps.hmppscontactsapi.util.StubUser
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

class DeleteContactAddressPhoneIntegrationTest : SecureAPIIntegrationTestBase() {
  private var savedContactId = 0L
  private var savedAddressId = 0L
  private var savedAddressPhoneId = 0L

  override val allowedRoles: Set<String> = setOf("ROLE_CONTACTS_ADMIN", "ROLE_CONTACTS__RW")

  @BeforeEach
  fun initialiseData() {
    setCurrentUser(StubUser.READ_WRITE_USER)
    savedContactId = testAPIClient.createAContact(
      CreateContactRequest(
        lastName = "address-phone",
        firstName = "has",
      ),
    ).id

    savedAddressId = testAPIClient.createAContactAddress(
      savedContactId,
      CreateContactAddressRequest(
        addressType = "HOME",
        primaryAddress = true,
        property = "27",
        street = "Hello Road",
        countryCode = "ENG",
        createdBy = "created",
      ),
    ).contactAddressId

    savedAddressPhoneId = testAPIClient.createAContactAddressPhone(
      savedContactId,
      savedAddressId,
      CreateContactAddressPhoneRequest(
        contactAddressId = savedAddressId,
        phoneType = "HOME",
        phoneNumber = "123456",
        extNumber = "2",
        createdBy = "CREATED",
      ),
    ).contactAddressPhoneId
  }

  override fun baseRequestBuilder(): WebTestClient.RequestHeadersSpec<*> = webTestClient.delete()
    .uri("/contact/$savedContactId/address/$savedAddressId/phone/$savedAddressPhoneId")
    .accept(MediaType.APPLICATION_JSON)

  @Test
  fun `should not delete if the contact is not found`() {
    val errors = webTestClient.delete()
      .uri("/contact/-321/address/$savedAddressId/phone/$savedAddressPhoneId")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisationUsingCurrentUser())
      .exchange()
      .expectStatus()
      .isNotFound
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody!!

    assertThat(errors.userMessage).isEqualTo("Entity not found : Contact (-321) not found")
    stubEvents.assertHasNoEvents(event = OutboundEvent.CONTACT_ADDRESS_PHONE_DELETED)
  }

  @Test
  fun `should not delete if the address-specific phone is not found`() {
    val errors = webTestClient.delete()
      .uri("/contact/$savedContactId/address/$savedAddressId/phone/-400")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisationUsingCurrentUser())
      .exchange()
      .expectStatus()
      .isNotFound
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody!!

    assertThat(errors.userMessage).isEqualTo("Entity not found : Contact address phone (-400) not found")
    stubEvents.assertHasNoEvents(event = OutboundEvent.CONTACT_ADDRESS_PHONE_DELETED)
  }

  @ParameterizedTest
  @ValueSource(strings = ["ROLE_CONTACTS_ADMIN", "ROLE_CONTACTS__RW"])
  fun `should delete the address-specific phone number`(role: String) {
    setCurrentUser(StubUser.DELETING_USER.copy(roles = listOf(role)))
    webTestClient.delete()
      .uri("/contact/$savedContactId/address/$savedAddressId/phone/$savedAddressPhoneId")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisationUsingCurrentUser())
      .exchange()
      .expectStatus()
      .isNoContent

    webTestClient.get()
      .uri("/contact/$savedContactId/address/$savedAddressId/phone/$savedAddressPhoneId")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisationUsingCurrentUser())
      .exchange()
      .expectStatus()
      .isNotFound

    stubEvents.assertHasEvent(
      event = OutboundEvent.CONTACT_ADDRESS_PHONE_DELETED,
      additionalInfo = ContactAddressPhoneInfo(savedAddressPhoneId, savedAddressId, Source.DPS),
      personReference = PersonReference(dpsContactId = savedContactId),
    )
  }
}
