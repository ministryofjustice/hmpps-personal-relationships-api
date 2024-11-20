package uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppscontactsapi.client.prisonersearchapi.model.ErrorResponse
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.ContactAddressEntity
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.ContactAddressPhoneEntity
import uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.H2IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.CreateContactRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.CreatePhoneRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.ContactAddressPhoneRepository
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.ContactAddressRepository
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.ContactPhoneInfo
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.PersonReference
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.Source

class DeleteContactPhoneIntegrationTest : H2IntegrationTestBase() {
  private var savedContactId = 0L
  private var savedContactPhoneId = 0L

  @Autowired
  protected lateinit var addressPhoneRepository: ContactAddressPhoneRepository

  @Autowired
  protected lateinit var addressRepository: ContactAddressRepository

  @BeforeEach
  fun initialiseData() {
    savedContactId = testAPIClient.createAContact(
      CreateContactRequest(
        lastName = "phone",
        firstName = "has",
        createdBy = "created",
      ),
    ).id
    savedContactPhoneId = testAPIClient.createAContactPhone(
      savedContactId,
      CreatePhoneRequest(
        phoneType = "MOB",
        phoneNumber = "07777777777",
        extNumber = "123456",
        createdBy = "USER1",
      ),
    ).contactPhoneId
  }

  @Test
  fun `should return unauthorized if no token`() {
    webTestClient.delete()
      .uri("/contact/$savedContactId/phone/$savedContactPhoneId")
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `should return forbidden if no role`() {
    webTestClient.delete()
      .uri("/contact/$savedContactId/phone/$savedContactPhoneId")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation())
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `should return forbidden if wrong role`() {
    webTestClient.delete()
      .uri("/contact/$savedContactId/phone/$savedContactPhoneId")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_WRONG")))
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `should not update the phone if the contact is not found`() {
    val errors = webTestClient.delete()
      .uri("/contact/-321/phone/$savedContactPhoneId")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CONTACTS_ADMIN")))
      .exchange()
      .expectStatus()
      .isNotFound
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody!!

    assertThat(errors.userMessage).isEqualTo("Entity not found : Contact (-321) not found")
    stubEvents.assertHasNoEvents(OutboundEvent.CONTACT_PHONE_DELETED, ContactPhoneInfo(savedContactPhoneId))
  }

  @Test
  fun `should not update the phone if the phone is not found`() {
    val errors = webTestClient.delete()
      .uri("/contact/$savedContactId/phone/-99")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CONTACTS_ADMIN")))
      .exchange()
      .expectStatus()
      .isNotFound
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody!!

    assertThat(errors.userMessage).isEqualTo("Entity not found : Contact phone (-99) not found")
    stubEvents.assertHasNoEvents(OutboundEvent.CONTACT_PHONE_DELETED, ContactPhoneInfo(-99))
  }

  @Test
  fun `should delete the contacts phone number`() {
    webTestClient.delete()
      .uri("/contact/$savedContactId/phone/$savedContactPhoneId")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CONTACTS_ADMIN")))
      .exchange()
      .expectStatus()
      .isNoContent

    webTestClient.get()
      .uri("/contact/$savedContactId/phone/$savedContactPhoneId")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CONTACTS_ADMIN")))
      .exchange()
      .expectStatus()
      .isNotFound

    stubEvents.assertHasEvent(
      event = OutboundEvent.CONTACT_PHONE_DELETED,
      additionalInfo = ContactPhoneInfo(savedContactPhoneId, Source.DPS),
      personReference = PersonReference(dpsContactId = savedContactId),
    )
  }

  @Test
  fun `should delete the contacts phone number even if associated with an address`() {
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
      ),
    )
    val addressPhone = addressPhoneRepository.saveAndFlush(
      ContactAddressPhoneEntity(
        contactAddressPhoneId = 0,
        contactId = savedContactId,
        contactPhoneId = savedContactPhoneId,
        contactAddressId = address.contactAddressId,
        createdBy = "USER1",
      ),
    )

    webTestClient.delete()
      .uri("/contact/$savedContactId/phone/$savedContactPhoneId")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CONTACTS_ADMIN")))
      .exchange()
      .expectStatus()
      .isNoContent

    webTestClient.get()
      .uri("/contact/$savedContactId/phone/$savedContactPhoneId")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CONTACTS_ADMIN")))
      .exchange()
      .expectStatus()
      .isNotFound

    stubEvents.assertHasEvent(
      event = OutboundEvent.CONTACT_PHONE_DELETED,
      additionalInfo = ContactPhoneInfo(savedContactPhoneId, Source.DPS),
      personReference = PersonReference(dpsContactId = savedContactId),
    )

    assertThat(addressPhoneRepository.findById(addressPhone.contactPhoneId)).isNotPresent
    assertThat(addressRepository.findById(address.contactAddressId)).isPresent
  }
}
