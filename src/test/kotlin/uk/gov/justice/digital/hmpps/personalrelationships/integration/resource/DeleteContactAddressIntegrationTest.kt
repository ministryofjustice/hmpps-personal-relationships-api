package uk.gov.justice.digital.hmpps.personalrelationships.integration.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.personalrelationships.config.User
import uk.gov.justice.digital.hmpps.personalrelationships.integration.SecureAPIIntegrationTestBase
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.CreateContactRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.address.CreateContactAddressRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.ContactAddressResponse
import uk.gov.justice.digital.hmpps.personalrelationships.service.events.ContactAddressInfo
import uk.gov.justice.digital.hmpps.personalrelationships.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.personalrelationships.service.events.PersonReference
import uk.gov.justice.digital.hmpps.personalrelationships.service.events.Source
import uk.gov.justice.digital.hmpps.personalrelationships.util.StubUser
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

class DeleteContactAddressIntegrationTest : SecureAPIIntegrationTestBase() {
  private var savedContactId = 0L
  private var savedContactAddressId = 0L

  override val allowedRoles: Set<String> = setOf("ROLE_CONTACTS_ADMIN", "ROLE_CONTACTS__RW")

  @BeforeEach
  fun initialiseData() {
    setCurrentUser(StubUser.READ_WRITE_USER)
    savedContactId = testAPIClient.createAContact(
      CreateContactRequest(
        lastName = "address",
        firstName = "has",
      ),
    ).id

    savedContactAddressId = testAPIClient.createAContactAddress(
      savedContactId,
      CreateContactAddressRequest(
        primaryAddress = false,
        addressType = "HOME",
        flat = "1A",
        property = "27",
        street = "Acacia Avenue",
        area = "Hoggs Bottom",
        postcode = "HB10 2NB",
        countryCode = "ENG",
      ),
    ).contactAddressId
    setCurrentUser(StubUser.DELETING_USER)
  }

  override fun baseRequestBuilder(): WebTestClient.RequestHeadersSpec<*> = webTestClient.delete()
    .uri("/contact/$savedContactId/address/$savedContactAddressId")
    .accept(MediaType.APPLICATION_JSON)

  @Test
  fun `should not delete the address if the contact is not found`() {
    val errors = webTestClient.delete()
      .uri("/contact/-321/address/$savedContactAddressId")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisationUsingCurrentUser())
      .exchange()
      .expectStatus()
      .isNotFound
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody!!

    assertThat(errors.userMessage).isEqualTo("Entity not found : Contact (-321) not found")

    stubEvents.assertHasNoEvents(
      event = OutboundEvent.CONTACT_ADDRESS_DELETED,
      additionalInfo = ContactAddressInfo(savedContactAddressId, Source.DPS, "deleted", "BXI"),
    )
  }

  @Test
  fun `should not update the address if the address is not found`() {
    val errors = webTestClient.delete()
      .uri("/contact/$savedContactId/address/-99")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisationUsingCurrentUser())
      .exchange()
      .expectStatus()
      .isNotFound
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody!!

    assertThat(errors.userMessage).isEqualTo("Entity not found : Contact address (-99) not found")

    stubEvents.assertHasNoEvents(event = OutboundEvent.CONTACT_ADDRESS_DELETED)
  }

  @ParameterizedTest
  @ValueSource(strings = ["ROLE_CONTACTS_ADMIN", "ROLE_CONTACTS__RW"])
  fun `should delete the contact address`(role: String) {
    setCurrentUser(StubUser.DELETING_USER.copy(roles = listOf(role)))
    webTestClient.delete()
      .uri("/contact/$savedContactId/address/$savedContactAddressId")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisationUsingCurrentUser())
      .exchange()
      .expectStatus()
      .isNoContent

    webTestClient.get()
      .uri("/contact/$savedContactId/address/$savedContactAddressId")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisationUsingCurrentUser())
      .exchange()
      .expectStatus()
      .isNotFound

    stubEvents.assertHasEvent(
      event = OutboundEvent.CONTACT_ADDRESS_DELETED,
      additionalInfo = ContactAddressInfo(savedContactAddressId, Source.DPS, "deleted", "BXI"),
      personReference = PersonReference(dpsContactId = savedContactId),
    )

    assertCustomEvent(savedContactId, savedContactAddressId, Source.DPS, User("deleted", "BXI"))
  }

  private fun assertCustomEvent(contactId: Long, contactAddressId: Long, source: Source, user: User) {
    verify(telemetryContactCustomEventService, times(1)).trackDeleteContactAddressEvent(any<ContactAddressResponse>(), any<Source>(), any<User>())
    verify(telemetryClient, times(1)).trackEvent(
      "contact-address-deleted",
      mapOf(
        "description" to "A contact address has been deleted",
        "source" to source.name,
        "username" to user.username,
        "active_caseload_id" to user.activeCaseLoadId,
        "contact_id" to contactId.toString(),
        "contact_address_id" to contactAddressId.toString(),
      ),
      null,
    )
  }
}
