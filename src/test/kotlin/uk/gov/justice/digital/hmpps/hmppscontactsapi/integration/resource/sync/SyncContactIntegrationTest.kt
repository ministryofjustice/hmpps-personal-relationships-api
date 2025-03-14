package uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.resource.sync

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.PostgresIntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync.SyncCreateContactRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync.SyncUpdateContactRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.sync.SyncContact
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.ContactInfo
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.PersonReference
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.Source
import java.time.LocalDate
import java.time.LocalDateTime

class SyncContactIntegrationTest : PostgresIntegrationTestBase() {

  @Nested
  inner class ContactSyncTests {

    @BeforeEach
    fun resetEvents() {
      stubEvents.reset()
    }

    @Test
    fun `Sync endpoints should return unauthorized if no token provided`() {
      webTestClient.get()
        .uri("/sync/contact/1")
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isUnauthorized

      webTestClient.post()
        .uri("/sync/contact")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(createSyncContactRequest(5000L))
        .exchange()
        .expectStatus()
        .isUnauthorized

      webTestClient.put()
        .uri("/sync/contact/1")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(updateContactRequest())
        .exchange()
        .expectStatus()
        .isUnauthorized

      webTestClient.delete()
        .uri("/sync/contact/1")
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `Sync endpoints should return forbidden without an authorised role on the token`() {
      webTestClient.get()
        .uri("/sync/contact/1")
        .accept(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf("ROLE_WRONG")))
        .exchange()
        .expectStatus()
        .isForbidden

      webTestClient.post()
        .uri("/sync/contact")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(createSyncContactRequest(5000L))
        .headers(setAuthorisation(roles = listOf("ROLE_WRONG")))
        .exchange()
        .expectStatus()
        .isForbidden

      webTestClient.put()
        .uri("/sync/contact/1")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(updateContactRequest())
        .headers(setAuthorisation(roles = listOf("ROLE_WRONG")))
        .exchange()
        .expectStatus()
        .isForbidden

      webTestClient.delete()
        .uri("/sync/contact/1")
        .accept(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf("ROLE_WRONG")))
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `should get an existing contact`() {
      // From base data
      val contactId = 15L
      val contact = webTestClient.get()
        .uri("/sync/contact/{contactId}", contactId)
        .accept(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf("PERSONAL_RELATIONSHIPS_MIGRATION")))
        .exchange()
        .expectStatus()
        .isOk
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBody(SyncContact::class.java)
        .returnResult().responseBody!!

      with(contact) {
        assertThat(id).isEqualTo(15)
        assertThat(title).isEqualTo("MRS")
        assertThat(firstName).isEqualTo("Carl")
        assertThat(lastName).isEqualTo("Fifteen")
        assertThat(middleName).isEqualTo("Middle")
        assertThat(dateOfBirth).isEqualTo(LocalDate.of(2000, 11, 26))
        assertThat(isStaff).isFalse
        assertThat(deceasedFlag).isFalse
        assertThat(deceasedDate).isEqualTo("2024-01-26")
        assertThat(gender).isEqualTo("F")
        assertThat(domesticStatus).isEqualTo("S")
        assertThat(languageCode).isEqualTo("ENG")
        assertThat(interpreterRequired).isFalse
        assertThat(createdBy).isEqualTo("TIM")
        assertThat(createdTime).isAfter(LocalDateTime.now().minusMinutes(5))
        assertThat(updatedBy).isNull()
        assertThat(updatedTime).isNull()
      }
    }

    @Test
    fun `should create a new contact`() {
      val contact = testAPIClient.syncCreateAnContact(createSyncContactRequest(5000L))

      // The created is returned
      with(contact) {
        assertThat(id).isEqualTo(5000L)
        assertThat(title).isEqualTo("MR")
        assertThat(firstName).isEqualTo("John")
        assertThat(lastName).isEqualTo("Doe")
        assertThat(middleName).isEqualTo("William")
        assertThat(dateOfBirth).isEqualTo(LocalDate.of(1980, 1, 1))
        assertThat(isStaff).isFalse
        assertThat(deceasedFlag).isFalse
        assertThat(deceasedDate).isNull()
        assertThat(gender).isEqualTo("M")
        assertThat(domesticStatus).isEqualTo("S")
        assertThat(languageCode).isEqualTo("EN")
        assertThat(interpreterRequired).isFalse
        assertThat(createdBy).isEqualTo("JD000001")
        assertThat(createdTime).isAfter(LocalDateTime.now().minusMinutes(5))
      }

      stubEvents.assertHasEvent(
        event = OutboundEvent.CONTACT_CREATED,
        additionalInfo = ContactInfo(contact.id, Source.NOMIS),
        personReference = PersonReference(dpsContactId = contact.id),
      )
    }

    @Test
    fun `should create and then update a contact`() {
      val contact = testAPIClient.syncCreateAnContact(createSyncContactRequest(5001L))

      with(contact) {
        assertThat(id).isEqualTo(5001L)
        assertThat(title).isEqualTo("MR")
        assertThat(firstName).isEqualTo("John")
        assertThat(lastName).isEqualTo("Doe")
        assertThat(middleName).isEqualTo("William")
        assertThat(createdBy).isEqualTo("JD000001")
        assertThat(createdTime).isAfter(LocalDateTime.now().minusMinutes(5))
      }

      stubEvents.assertHasEvent(
        event = OutboundEvent.CONTACT_CREATED,
        additionalInfo = ContactInfo(contact.id, Source.NOMIS),
        personReference = PersonReference(dpsContactId = contact.id),
      )

      val updatedContact = webTestClient.put()
        .uri("/sync/contact/{contactId}", contact.id)
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf("PERSONAL_RELATIONSHIPS_MIGRATION")))
        .bodyValue(updateContactRequest())
        .exchange()
        .expectStatus()
        .isOk
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBody(SyncContact::class.java)
        .returnResult().responseBody!!

      // Check the updated copy
      with(updatedContact) {
        assertThat(id).isEqualTo(5001L)
        assertThat(title).isEqualTo("MR")
        assertThat(firstName).isEqualTo("John")
        assertThat(lastName).isEqualTo("Doe")
        assertThat(middleName).isEqualTo("William")
        assertThat(dateOfBirth).isEqualTo(LocalDate.of(1980, 1, 1))
        assertThat(createdBy).isEqualTo("JD000001")
        assertThat(isStaff).isFalse
        assertThat(deceasedFlag).isFalse
        assertThat(deceasedDate).isNull()
        assertThat(gender).isEqualTo("M")
        assertThat(domesticStatus).isEqualTo("S")
        assertThat(languageCode).isEqualTo("EN")
        assertThat(interpreterRequired).isTrue()
        assertThat(updatedBy).isEqualTo("UPDATE")
        assertThat(updatedTime).isAfter(LocalDateTime.now().minusMinutes(5))
      }

      stubEvents.assertHasEvent(
        event = OutboundEvent.CONTACT_UPDATED,
        additionalInfo = ContactInfo(contact.id, Source.NOMIS),
        personReference = PersonReference(dpsContactId = contact.id),
      )
    }

    @Test
    fun `should delete an existing contact`() {
      val contact = testAPIClient.syncCreateAnContact(createSyncContactRequest(5002L))

      webTestClient.delete()
        .uri("/sync/contact/{contactId}", contact.id)
        .accept(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf("PERSONAL_RELATIONSHIPS_MIGRATION")))
        .exchange()
        .expectStatus()
        .isOk

      webTestClient.get()
        .uri("/sync/contact/{contactId}", contact.id)
        .accept(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf("PERSONAL_RELATIONSHIPS_MIGRATION")))
        .exchange()
        .expectStatus()
        .isNotFound

      stubEvents.assertHasEvent(
        event = OutboundEvent.CONTACT_CREATED,
        additionalInfo = ContactInfo(contact.id, Source.NOMIS),
        personReference = PersonReference(dpsContactId = contact.id),
      )

      stubEvents.assertHasEvent(
        event = OutboundEvent.CONTACT_DELETED,
        additionalInfo = ContactInfo(contact.id, Source.NOMIS),
        personReference = PersonReference(dpsContactId = contact.id),
      )
    }

    @Test
    fun `should support pageable contact IDs for reconciliation`() {
      testAPIClient.syncCreateAnContact(createSyncContactRequest(5005L))
      testAPIClient.syncCreateAnContact(createSyncContactRequest(5006L))
      testAPIClient.syncCreateAnContact(createSyncContactRequest(5007L))

      val firstPage = testAPIClient.syncReconcileContacts(0, 2)
      with(firstPage) {
        assertThat(totalElements).isGreaterThanOrEqualTo(3)
        assertThat(content).hasSize(2)
        assertThat(content).extracting("contactId").hasSize(2)
      }

      val secondPage = testAPIClient.syncReconcileContacts(1, 2)
      with(secondPage) {
        assertThat(totalElements).isGreaterThanOrEqualTo(3)
        assertThat(content.size).isGreaterThanOrEqualTo(1)
      }

      val bigPage = testAPIClient.syncReconcileContacts(0, 100)
      with(bigPage) {
        assertThat(totalElements).isGreaterThanOrEqualTo(3)
        assertThat(content.size).isGreaterThanOrEqualTo(3)
        assertThat(content).extracting("contactId").containsAll(listOf(5005L, 5006L, 5007L))
      }
    }

    private fun updateContactRequest() = SyncUpdateContactRequest(
      title = "MR",
      firstName = "John",
      lastName = "Doe",
      middleName = "William",
      dateOfBirth = LocalDate.of(1980, 1, 1),
      isStaff = false,
      deceasedFlag = false,
      deceasedDate = null,
      gender = "M",
      domesticStatus = "S",
      languageCode = "EN",
      interpreterRequired = true,
      updatedBy = "UPDATE",
      updatedTime = LocalDateTime.now(),
    )

    private fun createSyncContactRequest(personId: Long = 0L) = SyncCreateContactRequest(
      personId = personId,
      firstName = "John",
      title = "MR",
      lastName = "Doe",
      middleName = "William",
      dateOfBirth = LocalDate.of(1980, 1, 1),
      createdBy = "JD000001",
      isStaff = false,
      deceasedFlag = false,
      deceasedDate = null,
      gender = "M",
      domesticStatus = "S",
      languageCode = "EN",
      interpreterRequired = false,
    )
  }
}
