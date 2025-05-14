package uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.resource.sync

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.PostgresIntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.CreateContactRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync.SyncCreateContactAddressRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync.SyncUpdateContactAddressRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.sync.SyncContactAddress
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.ContactAddressRepository
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.ContactAddressInfo
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.PersonReference
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.Source
import uk.gov.justice.digital.hmpps.hmppscontactsapi.util.StubUser
import java.time.LocalDateTime

class SyncContactAddressIntegrationTest : PostgresIntegrationTestBase() {
  @Autowired
  private lateinit var contactAddressRepository: ContactAddressRepository

  private var contactId = 0L

  @BeforeEach
  fun initialiseData() {
    setCurrentUser(StubUser.READ_WRITE_USER)
    contactId = testAPIClient.createAContact(aMinimalCreateContactRequest()).id
    setCurrentUser(StubUser.SYNC_AND_MIGRATE_USER)
  }

  @Test
  fun `Sync endpoints should return unauthorized if no token provided`() {
    webTestClient.get()
      .uri("/sync/contact-address/1")
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus()
      .isUnauthorized

    webTestClient.put()
      .uri("/sync/contact-address")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(createContactAddressRequest(contactId))
      .exchange()
      .expectStatus()
      .isUnauthorized

    webTestClient.post()
      .uri("/sync/contact-address/1")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(updateContactAddressRequest(contactId))
      .exchange()
      .expectStatus()
      .isUnauthorized

    webTestClient.delete()
      .uri("/sync/contact-address/1")
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @ParameterizedTest
  @ValueSource(strings = ["ROLE_CONTACTS_ADMIN", "ROLE_CONTACTS__R", "ROLE_CONTACTS__RW"])
  fun `Sync endpoints should return forbidden without an authorised role on the token`(role: String) {
    setCurrentUser(StubUser.SYNC_AND_MIGRATE_USER.copy(roles = listOf(role)))
    webTestClient.get()
      .uri("/sync/contact-address/1")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisationUsingCurrentUser())
      .exchange()
      .expectStatus()
      .isForbidden

    webTestClient.post()
      .uri("/sync/contact-address")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(createContactAddressRequest(contactId))
      .headers(setAuthorisationUsingCurrentUser())
      .exchange()
      .expectStatus()
      .isForbidden

    webTestClient.put()
      .uri("/sync/contact-address/1")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(updateContactAddressRequest(contactId))
      .headers(setAuthorisationUsingCurrentUser())
      .exchange()
      .expectStatus()
      .isForbidden

    webTestClient.delete()
      .uri("/sync/contact-address/1")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisationUsingCurrentUser())
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `should get an existing contact address`() {
    // From base data
    val contactAddressId = 1L
    val contactAddress = webTestClient.get()
      .uri("/sync/contact-address/{contactAddressId}", contactAddressId)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisationUsingCurrentUser())
      .exchange()
      .expectStatus()
      .isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(SyncContactAddress::class.java)
      .returnResult().responseBody!!

    with(contactAddress) {
      assertThat(addressType).isEqualTo("HOME")
      assertThat(property).isEqualTo("24")
      assertThat(street).isEqualTo("Acacia Avenue")
      assertThat(primaryAddress).isTrue()
    }
  }

  @Test
  fun `should create a new contact address`() {
    val contactAddress = webTestClient.post()
      .uri("/sync/contact-address")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .headers(setAuthorisationUsingCurrentUser())
      .bodyValue(createContactAddressRequest(contactId))
      .exchange()
      .expectStatus()
      .isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(SyncContactAddress::class.java)
      .returnResult().responseBody!!

    // The created address is returned
    with(contactAddress) {
      assertThat(flat).isEqualTo("1B")
      assertThat(addressType).isEqualTo("HOME")
      assertThat(comments).isEqualTo("Some comments")
      assertThat(updatedBy).isNullOrEmpty()
      assertThat(updatedTime).isNull()
      assertThat(createdBy).isEqualTo("CREATE")
      assertThat(createdTime).isAfter(LocalDateTime.now().minusMinutes(5))
    }
    stubEvents.assertHasEvent(
      event = OutboundEvent.CONTACT_ADDRESS_CREATED,
      additionalInfo = ContactAddressInfo(contactAddress.contactAddressId, Source.NOMIS, "CREATE"),
      personReference = PersonReference(dpsContactId = contactId),
    )
  }

  @Test
  fun `should create and then update a contact address`() {
    val contactAddress = webTestClient.post()
      .uri("/sync/contact-address")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .headers(setAuthorisationUsingCurrentUser())
      .bodyValue(createContactAddressRequest(contactId))
      .exchange()
      .expectStatus()
      .isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(SyncContactAddress::class.java)
      .returnResult().responseBody!!

    with(contactAddress) {
      assertThat(addressType).isEqualTo("HOME")
      assertThat(flat).isEqualTo("1B")
      assertThat(property).isEqualTo("13")
      assertThat(street).isEqualTo("Main Street")
      assertThat(createdBy).isEqualTo("CREATE")
      assertThat(createdTime).isAfter(LocalDateTime.now().minusMinutes(5))
    }

    val updatedAddress = webTestClient.put()
      .uri("/sync/contact-address/{contactAddressId}", contactAddress.contactAddressId)
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .headers(setAuthorisationUsingCurrentUser())
      .bodyValue(updateContactAddressRequest(contactId))
      .exchange()
      .expectStatus()
      .isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(SyncContactAddress::class.java)
      .returnResult().responseBody!!

    // Check the updated copy
    with(updatedAddress) {
      assertThat(flat).isEqualTo("2B")
      assertThat(addressType).isEqualTo("WORK")
      assertThat(comments).isEqualTo("Updated comments")
      assertThat(updatedBy).isEqualTo("UPDATE")
      assertThat(updatedTime).isAfter(LocalDateTime.now().minusMinutes(5))
      assertThat(createdBy).isEqualTo("CREATE")
      assertThat(createdTime).isNotNull()
    }

    stubEvents.assertHasEvent(
      event = OutboundEvent.CONTACT_ADDRESS_UPDATED,
      additionalInfo = ContactAddressInfo(contactAddress.contactAddressId, Source.NOMIS, "UPDATE"),
      personReference = PersonReference(dpsContactId = contactId),
    )
  }

  @Test
  fun `should set the address verified details if verified on a contact address update`() {
    // Create a contact address with default - verified = false
    val contactAddress = webTestClient.post()
      .uri("/sync/contact-address")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .headers(setAuthorisationUsingCurrentUser())
      .bodyValue(createContactAddressRequest(contactId))
      .exchange()
      .expectStatus()
      .isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(SyncContactAddress::class.java)
      .returnResult().responseBody!!

    with(contactAddress) {
      assertThat(verified).isFalse()
      assertThat(verifiedBy).isNull()
      assertThat(verifiedTime).isNull()
      assertThat(createdBy).isEqualTo("CREATE")
      assertThat(createdTime).isAfter(LocalDateTime.now().minusMinutes(5))
    }

    val updatedAddress = webTestClient.put()
      .uri("/sync/contact-address/{contactAddressId}", contactAddress.contactAddressId)
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .headers(setAuthorisationUsingCurrentUser())
      .bodyValue(updateContactAddressRequest(contactId, verified = true))
      .exchange()
      .expectStatus()
      .isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(SyncContactAddress::class.java)
      .returnResult().responseBody!!

    // Check the updated address is now verified (with who and when)
    with(updatedAddress) {
      assertThat(verified).isTrue()
      assertThat(verifiedBy).isEqualTo("UPDATE")
      assertThat(verifiedTime).isAfter(LocalDateTime.now().minusMinutes(5))
      assertThat(updatedBy).isEqualTo("UPDATE")
      assertThat(updatedTime).isAfter(LocalDateTime.now().minusMinutes(5))
      assertThat(createdBy).isEqualTo("CREATE")
      assertThat(createdTime).isNotNull()
    }
  }

  @Test
  fun `should delete an existing contact address`() {
    val beforeCount = contactAddressRepository.count()

    webTestClient.delete()
      .uri("/sync/contact-address/5")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisationUsingCurrentUser())
      .exchange()
      .expectStatus()
      .isOk

    val afterCount = contactAddressRepository.count()
    assertThat(beforeCount).isEqualTo((afterCount + 1))
    stubEvents.assertHasEvent(
      event = OutboundEvent.CONTACT_ADDRESS_DELETED,
      additionalInfo = ContactAddressInfo(5, Source.NOMIS, "SYS"),
      personReference = PersonReference(dpsContactId = 4),
    )
  }

  private fun updateContactAddressRequest(contactId: Long, verified: Boolean = false) = SyncUpdateContactAddressRequest(
    contactId = contactId,
    addressType = "WORK",
    primaryAddress = false,
    flat = "2B",
    property = "14",
    street = "Main Street",
    area = "Dodworth",
    cityCode = "CVNTRY",
    countyCode = "WARWKS",
    postcode = "CV4 9NJ",
    countryCode = "UK",
    comments = "Updated comments",
    updatedBy = "UPDATE",
    updatedTime = LocalDateTime.now(),
    verified = verified,
  )

  private fun createContactAddressRequest(contactId: Long) = SyncCreateContactAddressRequest(
    contactId = contactId,
    addressType = "HOME",
    primaryAddress = false,
    flat = "1B",
    property = "13",
    street = "Main Street",
    area = "Dodworth",
    cityCode = "CVNTRY",
    countyCode = "WARWKS",
    postcode = "CV4 9NJ",
    countryCode = "UK",
    comments = "Some comments",
    createdBy = "CREATE",
  )

  private fun aMinimalCreateContactRequest() = CreateContactRequest(
    lastName = "last",
    firstName = "first",
  )
}
