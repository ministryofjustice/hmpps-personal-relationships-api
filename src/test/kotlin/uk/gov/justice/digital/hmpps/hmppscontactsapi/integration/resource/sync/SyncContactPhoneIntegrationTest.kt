package uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.resource.sync

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.PostgresIntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.CreateContactRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync.SyncCreateContactPhoneRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync.SyncUpdateContactPhoneRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.sync.SyncContactPhone
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.ContactPhoneInfo
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.PersonReference
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.Source
import uk.gov.justice.digital.hmpps.hmppscontactsapi.util.StubUser
import java.time.LocalDateTime

class SyncContactPhoneIntegrationTest : PostgresIntegrationTestBase() {

  private var savedContactId = 0L

  @BeforeEach
  fun initialiseData() {
    setCurrentUser(StubUser.READ_WRITE_USER)
    savedContactId = testAPIClient.createAContact(aMinimalCreateContactRequest()).id
    setCurrentUser(StubUser.SYNC_AND_MIGRATE_USER)
  }

  @Test
  fun `Sync endpoints should return unauthorized if no token provided`() {
    webTestClient.get()
      .uri("/sync/contact-phone/1")
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus()
      .isUnauthorized

    webTestClient.put()
      .uri("/sync/contact-phone")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(createContactPhoneRequest(savedContactId))
      .exchange()
      .expectStatus()
      .isUnauthorized

    webTestClient.post()
      .uri("/sync/contact-phone/1")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(updateContactPhoneRequest(savedContactId))
      .exchange()
      .expectStatus()
      .isUnauthorized

    webTestClient.delete()
      .uri("/sync/contact-phone/1")
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
      .uri("/sync/contact-phone/1")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisationUsingCurrentUser())
      .exchange()
      .expectStatus()
      .isForbidden

    webTestClient.post()
      .uri("/sync/contact-phone")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(createContactPhoneRequest(savedContactId))
      .headers(setAuthorisationUsingCurrentUser())
      .exchange()
      .expectStatus()
      .isForbidden

    webTestClient.put()
      .uri("/sync/contact-phone/1")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(updateContactPhoneRequest(savedContactId))
      .headers(setAuthorisationUsingCurrentUser())
      .exchange()
      .expectStatus()
      .isForbidden

    webTestClient.delete()
      .uri("/sync/contact-phone/1")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisationUsingCurrentUser())
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `should get an existing contact phone`() {
    // From base data
    val contactPhoneId = 3L
    val contactPhone = webTestClient.get()
      .uri("/sync/contact-phone/{contactPhoneId}", contactPhoneId)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisationUsingCurrentUser())
      .exchange()
      .expectStatus()
      .isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(SyncContactPhone::class.java)
      .returnResult().responseBody!!

    with(contactPhone) {
      assertThat(phoneType).isEqualTo("MOB")
      assertThat(phoneNumber).isEqualTo("07878 222222")
    }
  }

  @Test
  fun `should create a new contact phone`() {
    val contactPhone = webTestClient.post()
      .uri("/sync/contact-phone")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .headers(setAuthorisationUsingCurrentUser())
      .bodyValue(createContactPhoneRequest(savedContactId))
      .exchange()
      .expectStatus()
      .isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(SyncContactPhone::class.java)
      .returnResult().responseBody!!

    // The created phone is returned
    with(contactPhone) {
      assertThat(contactPhoneId).isGreaterThan(10L)
      assertThat(contactId).isEqualTo(savedContactId)
      assertThat(phoneType).isEqualTo("Mobile")
      assertThat(phoneNumber).isEqualTo("555-1234")
      assertThat(extNumber).isEqualTo("101")
      assertThat(createdBy).isEqualTo("CREATE")
      assertThat(createdTime).isAfter(LocalDateTime.now().minusMinutes(5))
    }
    stubEvents.assertHasEvent(
      event = OutboundEvent.CONTACT_PHONE_CREATED,
      additionalInfo = ContactPhoneInfo(contactPhone.contactPhoneId, Source.NOMIS, "SYS"),
      personReference = PersonReference(dpsContactId = contactPhone.contactId),
    )
  }

  @Test
  fun `should create and then update a contact phone`() {
    val contactPhone = webTestClient.post()
      .uri("/sync/contact-phone")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .headers(setAuthorisationUsingCurrentUser())
      .bodyValue(createContactPhoneRequest(savedContactId))
      .exchange()
      .expectStatus()
      .isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(SyncContactPhone::class.java)
      .returnResult().responseBody!!

    with(contactPhone) {
      assertThat(phoneType).isEqualTo("Mobile")
      assertThat(phoneNumber).isEqualTo("555-1234")
      assertThat(extNumber).isEqualTo("101")
      assertThat(createdBy).isEqualTo("CREATE")
      assertThat(createdTime).isAfter(LocalDateTime.now().minusMinutes(5))
    }

    val updatedPhone = webTestClient.put()
      .uri("/sync/contact-phone/{contactPhoneId}", contactPhone.contactPhoneId)
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .headers(setAuthorisationUsingCurrentUser())
      .bodyValue(updateContactPhoneRequest(savedContactId))
      .exchange()
      .expectStatus()
      .isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(SyncContactPhone::class.java)
      .returnResult().responseBody!!

    // Check the updated copy
    with(updatedPhone) {
      assertThat(contactPhoneId).isGreaterThan(10)
      assertThat(contactId).isEqualTo(savedContactId)
      assertThat(phoneType).isEqualTo("Mobile")
      assertThat(phoneNumber).isEqualTo("555-1234")
      assertThat(updatedBy).isEqualTo("UPDATE")
      assertThat(updatedTime).isAfter(LocalDateTime.now().minusMinutes(5))
      assertThat(createdBy).isEqualTo("CREATE")
      assertThat(createdTime).isNotNull()
    }
    stubEvents.assertHasEvent(
      event = OutboundEvent.CONTACT_PHONE_UPDATED,
      additionalInfo = ContactPhoneInfo(contactPhone.contactPhoneId, Source.NOMIS, "SYS"),
      personReference = PersonReference(dpsContactId = contactPhone.contactId),
    )
  }

  @Test
  fun `should delete an existing contact phone`() {
    val contactPhoneId = 11L

    webTestClient.delete()
      .uri("/sync/contact-phone/{contactPhoneId}", contactPhoneId)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisationUsingCurrentUser())
      .exchange()
      .expectStatus()
      .isOk

    webTestClient.get()
      .uri("/sync/contact-phone/{contactPhoneId}", contactPhoneId)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisationUsingCurrentUser())
      .exchange()
      .expectStatus()
      .isNotFound
    stubEvents.assertHasEvent(
      event = OutboundEvent.CONTACT_PHONE_DELETED,
      additionalInfo = ContactPhoneInfo(contactPhoneId, Source.NOMIS, "SYS"),
      personReference = PersonReference(dpsContactId = 10),
    )
  }

  private fun updateContactPhoneRequest(contactId: Long) = SyncUpdateContactPhoneRequest(
    contactId = contactId,
    phoneType = "Mobile",
    phoneNumber = "555-1234",
    extNumber = "101",
    updatedBy = "UPDATE",
    updatedTime = LocalDateTime.now(),
  )

  private fun createContactPhoneRequest(contactId: Long) = SyncCreateContactPhoneRequest(
    contactId = contactId,
    phoneType = "Mobile",
    phoneNumber = "555-1234",
    extNumber = "101",
    createdBy = "CREATE",
  )

  private fun aMinimalCreateContactRequest() = CreateContactRequest(
    lastName = "last",
    firstName = "first",
  )
}
