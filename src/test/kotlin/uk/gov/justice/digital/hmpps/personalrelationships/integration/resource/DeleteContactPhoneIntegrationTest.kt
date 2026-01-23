package uk.gov.justice.digital.hmpps.personalrelationships.integration.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.personalrelationships.entity.ContactAddressEntity
import uk.gov.justice.digital.hmpps.personalrelationships.entity.ContactAddressPhoneEntity
import uk.gov.justice.digital.hmpps.personalrelationships.integration.SecureAPIIntegrationTestBase
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.CreateContactRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.phone.CreatePhoneRequest
import uk.gov.justice.digital.hmpps.personalrelationships.repository.ContactAddressPhoneRepository
import uk.gov.justice.digital.hmpps.personalrelationships.repository.ContactAddressRepository
import uk.gov.justice.digital.hmpps.personalrelationships.service.events.ContactPhoneInfo
import uk.gov.justice.digital.hmpps.personalrelationships.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.personalrelationships.service.events.PersonReference
import uk.gov.justice.digital.hmpps.personalrelationships.service.events.Source
import uk.gov.justice.digital.hmpps.personalrelationships.util.StubUser
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.time.LocalDateTime

class DeleteContactPhoneIntegrationTest : SecureAPIIntegrationTestBase() {
  private var savedContactId = 0L
  private var savedContactPhoneId = 0L

  override val allowedRoles: Set<String> = setOf("ROLE_CONTACTS_ADMIN", "ROLE_CONTACTS__RW")

  @Autowired
  protected lateinit var addressPhoneRepository: ContactAddressPhoneRepository

  @Autowired
  protected lateinit var addressRepository: ContactAddressRepository

  @BeforeEach
  fun initialiseData() {
    setCurrentUser(StubUser.CREATING_USER)
    savedContactId = testAPIClient.createAContact(
      CreateContactRequest(
        lastName = "phone",
        firstName = "has",
      ),

    ).id
    savedContactPhoneId = testAPIClient.createAContactPhone(
      savedContactId,
      CreatePhoneRequest(
        phoneType = "MOB",
        phoneNumber = "07777777777",
        extNumber = "123456",
      ),

    ).contactPhoneId
    setCurrentUser(StubUser.DELETING_USER)
  }

  override fun baseRequestBuilder(): WebTestClient.RequestHeadersSpec<*> = webTestClient.delete()
    .uri("/contact/$savedContactId/phone/$savedContactPhoneId")
    .accept(MediaType.APPLICATION_JSON)

  @Test
  fun `should not update the phone if the contact is not found`() {
    val errors = webTestClient.delete()
      .uri("/contact/-321/phone/$savedContactPhoneId")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisationUsingCurrentUser())
      .exchange()
      .expectStatus()
      .isNotFound
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody!!

    assertThat(errors.userMessage).isEqualTo("Entity not found : Contact (-321) not found")
    stubEvents.assertHasNoEvents(OutboundEvent.CONTACT_PHONE_DELETED)
  }

  @Test
  fun `should not update the phone if the phone is not found`() {
    val errors = webTestClient.delete()
      .uri("/contact/$savedContactId/phone/-99")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisationUsingCurrentUser())
      .exchange()
      .expectStatus()
      .isNotFound
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody!!

    assertThat(errors.userMessage).isEqualTo("Entity not found : Contact phone (-99) not found")
    stubEvents.assertHasNoEvents(OutboundEvent.CONTACT_PHONE_DELETED)
  }

  @Test
  fun `should delete the contacts phone number`() {
    webTestClient.delete()
      .uri("/contact/$savedContactId/phone/$savedContactPhoneId")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisationUsingCurrentUser())
      .exchange()
      .expectStatus()
      .isNoContent

    webTestClient.get()
      .uri("/contact/$savedContactId/phone/$savedContactPhoneId")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisationUsingCurrentUser())
      .exchange()
      .expectStatus()
      .isNotFound

    stubEvents.assertHasEvent(
      event = OutboundEvent.CONTACT_PHONE_DELETED,
      additionalInfo = ContactPhoneInfo(savedContactPhoneId, Source.DPS, "deleted", "BXI"),
      personReference = PersonReference(dpsContactId = savedContactId),
    )
  }

  @ParameterizedTest
  @ValueSource(strings = ["ROLE_CONTACTS_ADMIN", "ROLE_CONTACTS__RW"])
  fun `should delete the contacts phone number even if associated with an address`(role: String) {
    setCurrentUser(StubUser.DELETING_USER.copy(roles = listOf(role)))
    val address = addressRepository.saveAndFlush(
      ContactAddressEntity(
        contactAddressId = 0L,
        contactId = savedContactId,
        addressType = "HOME",
        primaryAddress = false,
        flat = "1B",
        property = "13",
        street = "Main Street",
        area = "Dodworth",
        cityCode = "CVNTRY",
        countyCode = "WARWKS",
        countryCode = "UK",
        comments = "Some comments",
        createdBy = "CREATE",
        createdTime = LocalDateTime.now(),
      ),
    )
    val addressPhone = addressPhoneRepository.saveAndFlush(
      ContactAddressPhoneEntity(
        contactAddressPhoneId = 0,
        contactId = savedContactId,
        contactPhoneId = savedContactPhoneId,
        contactAddressId = address.contactAddressId,
        createdBy = "USER1",
        createdTime = LocalDateTime.now(),
      ),
    )

    webTestClient.delete()
      .uri("/contact/$savedContactId/phone/$savedContactPhoneId")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisationUsingCurrentUser())
      .exchange()
      .expectStatus()
      .isNoContent

    webTestClient.get()
      .uri("/contact/$savedContactId/phone/$savedContactPhoneId")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisationUsingCurrentUser())
      .exchange()
      .expectStatus()
      .isNotFound

    stubEvents.assertHasEvent(
      event = OutboundEvent.CONTACT_PHONE_DELETED,
      additionalInfo = ContactPhoneInfo(savedContactPhoneId, Source.DPS, "deleted", "BXI"),
      personReference = PersonReference(dpsContactId = savedContactId),
    )

    assertThat(addressPhoneRepository.findById(addressPhone.contactPhoneId)).isNotPresent
    assertThat(addressRepository.findById(address.contactAddressId)).isPresent
  }
}
