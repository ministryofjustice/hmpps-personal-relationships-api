package uk.gov.justice.digital.hmpps.personalrelationships.integration.resource.sync

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.personalrelationships.client.manage.users.UserDetails
import uk.gov.justice.digital.hmpps.personalrelationships.config.User
import uk.gov.justice.digital.hmpps.personalrelationships.integration.PostgresIntegrationTestBase
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.sync.SyncCreatePrisonerContactRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.sync.SyncUpdatePrisonerContactRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.sync.SyncPrisonerContact
import uk.gov.justice.digital.hmpps.personalrelationships.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.personalrelationships.service.events.PersonReference
import uk.gov.justice.digital.hmpps.personalrelationships.service.events.PrisonerContactInfo
import uk.gov.justice.digital.hmpps.personalrelationships.service.events.Source
import uk.gov.justice.digital.hmpps.personalrelationships.util.StubUser
import java.time.LocalDate
import java.time.LocalDateTime

class SyncPrisonerContactIntegrationTest : PostgresIntegrationTestBase() {

  @BeforeEach
  fun setUp() {
    setCurrentUser(StubUser.SYNC_AND_MIGRATE_USER)
    stubGetUserByUsername(UserDetails("adminUser", "Create", "KMI"))
    stubGetUserByUsername(UserDetails("UpdatedUser", "Update", "BXI"))
  }

  @Nested
  inner class PrisonerContactSyncTests {

    @Test
    fun `Sync endpoints should return unauthorized if no token provided`() {
      webTestClient.get()
        .uri("/sync/prisoner-contact/1")
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isUnauthorized

      webTestClient.put()
        .uri("/sync/prisoner-contact")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(createPrisonerContactRequest())
        .exchange()
        .expectStatus()
        .isUnauthorized

      webTestClient.post()
        .uri("/sync/prisoner-contact/1")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(updatePrisonerContactRequest())
        .exchange()
        .expectStatus()
        .isUnauthorized

      webTestClient.delete()
        .uri("/sync/prisoner-contact/1")
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `Sync endpoints should return forbidden without an authorised role on the token`() {
      setCurrentUser(StubUser.USER_WITH_WRONG_ROLES)
      webTestClient.get()
        .uri("/sync/prisoner-contact/1")
        .accept(MediaType.APPLICATION_JSON)
        .headers(setAuthorisationUsingCurrentUser())
        .exchange()
        .expectStatus()
        .isForbidden

      webTestClient.post()
        .uri("/sync/prisoner-contact")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(createPrisonerContactRequest())
        .headers(setAuthorisationUsingCurrentUser())
        .exchange()
        .expectStatus()
        .isForbidden

      webTestClient.put()
        .uri("/sync/prisoner-contact/1")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(updatePrisonerContactRequest())
        .headers(setAuthorisationUsingCurrentUser())
        .exchange()
        .expectStatus()
        .isForbidden

      webTestClient.delete()
        .uri("/sync/prisoner-contact/1")
        .accept(MediaType.APPLICATION_JSON)
        .headers(setAuthorisationUsingCurrentUser())
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `should get an existing prisoner contact`() {
      // From base data
      val contactId = 15L
      val prisonerContact = webTestClient.get()
        .uri("/sync/prisoner-contact/{prisonerContactId}", contactId)
        .accept(MediaType.APPLICATION_JSON)
        .headers(setAuthorisationUsingCurrentUser())
        .exchange()
        .expectStatus()
        .isOk
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBody(SyncPrisonerContact::class.java)
        .returnResult().responseBody!!

      with(prisonerContact) {
        assertThat(contactId).isEqualTo(15L)
        assertThat(prisonerNumber).isEqualTo("G4793VF")
        assertThat(contactType).isEqualTo("S")
        assertThat(relationshipType).isEqualTo("UN")
        assertThat(nextOfKin).isFalse
        assertThat(emergencyContact).isFalse
        assertThat(active).isTrue
        assertThat(approvedVisitor).isFalse
        assertThat(currentTerm).isTrue
        assertThat(expiryDate).isNull()
        assertThat(createdAtPrison).isEqualTo("MDI")
        assertThat(comments).isEqualTo("Comment")
        assertThat(createdBy).isEqualTo("TIM")
        assertThat(createdTime).isAfter(LocalDateTime.now().minusYears(1))
        assertThat(updatedBy).isNull()
        assertThat(updatedTime).isNull()
      }
    }

    @Test
    fun `should create a new prisoner contact`() {
      val prisonerContact = webTestClient.post()
        .uri("/sync/prisoner-contact")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .headers(setAuthorisationUsingCurrentUser())
        .bodyValue(createPrisonerContactRequest())
        .exchange()
        .expectStatus()
        .isOk
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBody(SyncPrisonerContact::class.java)
        .returnResult().responseBody!!

      // The created is returned
      with(prisonerContact) {
        assertThat(id).isGreaterThan(29L)
        assertThat(contactId).isEqualTo(1L)
        assertThat(prisonerNumber).isEqualTo("A1234BC")
        assertThat(contactType).isEqualTo("S")
        assertThat(relationshipType).isEqualTo("FRI")
        assertThat(nextOfKin).isTrue
        assertThat(emergencyContact).isFalse
        assertThat(comments).isEqualTo("Create relationship")
        assertThat(active).isTrue
        assertThat(approvedVisitor).isTrue
        assertThat(currentTerm).isTrue
        assertThat(expiryDate).isEqualTo(LocalDate.of(2025, 12, 31))
        assertThat(createdAtPrison).isEqualTo("LONDN")
        assertThat(updatedBy).isNull()
        assertThat(updatedTime).isNull()
        assertThat(createdBy).isEqualTo("adminUser")
        assertThat(createdTime).isAfter(LocalDateTime.now().minusMinutes(5))
      }
      stubEvents.assertHasEvent(
        event = OutboundEvent.PRISONER_CONTACT_CREATED,
        additionalInfo = PrisonerContactInfo(prisonerContact.id, Source.NOMIS, "adminUser", "KMI"),
        personReference = PersonReference(dpsContactId = prisonerContact.contactId, nomsNumber = prisonerContact.prisonerNumber),
      )

      assertCustomCreatedEvent(prisonerContact, Source.NOMIS, User("adminUser", "KMI"))
    }

    @Test
    fun `should create and then update a prisoner contact`() {
      val prisonerContact = webTestClient.post()
        .uri("/sync/prisoner-contact")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .headers(setAuthorisationUsingCurrentUser())
        .bodyValue(createPrisonerContactRequest())
        .exchange()
        .expectStatus()
        .isOk
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBody(SyncPrisonerContact::class.java)
        .returnResult().responseBody!!

      with(prisonerContact) {
        assertThat(id).isGreaterThan(29L)
        assertThat(contactId).isEqualTo(1L)
        assertThat(prisonerNumber).isEqualTo("A1234BC")
        assertThat(contactType).isEqualTo("S")
        assertThat(relationshipType).isEqualTo("FRI")
        assertThat(nextOfKin).isTrue
        assertThat(emergencyContact).isFalse
        assertThat(comments).isEqualTo("Create relationship")
        assertThat(active).isTrue
        assertThat(approvedVisitor).isTrue
        assertThat(currentTerm).isTrue
        assertThat(expiryDate).isEqualTo(LocalDate.of(2025, 12, 31))
        assertThat(createdAtPrison).isEqualTo("LONDN")
        assertThat(updatedBy).isNull()
        assertThat(updatedTime).isNull()
        assertThat(createdBy).isEqualTo("adminUser")
        assertThat(createdTime).isAfter(LocalDateTime.now().minusMinutes(5))
      }

      val updatedPrisonerContact = webTestClient.put()
        .uri("/sync/prisoner-contact/{prisonerContactId}", prisonerContact.id)
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .headers(setAuthorisationUsingCurrentUser())
        .bodyValue(updatePrisonerContactRequest())
        .exchange()
        .expectStatus()
        .isOk
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBody(SyncPrisonerContact::class.java)
        .returnResult().responseBody!!

      // Check the updated copy
      with(updatedPrisonerContact) {
        assertThat(id).isGreaterThan(29L)
        assertThat(contactId).isEqualTo(1L)
        assertThat(prisonerNumber).isEqualTo("A1234BC")
        assertThat(contactType).isEqualTo("O")
        assertThat(relationshipType).isEqualTo("LAW")
        assertThat(nextOfKin).isTrue
        assertThat(emergencyContact).isFalse
        assertThat(comments).isEqualTo("Updated relationship type to family")
        assertThat(active).isTrue
        assertThat(approvedVisitor).isTrue
        assertThat(currentTerm).isTrue
        assertThat(expiryDate).isEqualTo(LocalDate.of(2025, 12, 31))
        assertThat(createdAtPrison).isEqualTo("LONDN")
        assertThat(updatedBy).isEqualTo("UpdatedUser")
        assertThat(updatedTime).isNotNull
      }
      stubEvents.assertHasEvent(
        event = OutboundEvent.PRISONER_CONTACT_UPDATED,
        additionalInfo = PrisonerContactInfo(updatedPrisonerContact.id, Source.NOMIS, "UpdatedUser", "BXI"),
        personReference = PersonReference(dpsContactId = updatedPrisonerContact.contactId, nomsNumber = updatedPrisonerContact.prisonerNumber),
      )

      assertCustomUpdatedEvent(updatedPrisonerContact, Source.NOMIS, User("UpdatedUser", "BXI"))
    }

    @Test
    fun `when contact exists but next of kin is false and update changes next of kin to true then a create next of kin event is sent`() {
      val prisonerContact = webTestClient.post()
        .uri("/sync/prisoner-contact")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .headers(setAuthorisationUsingCurrentUser())
        .bodyValue(createPrisonerContactRequest(prisonerNumber = "A1234AB", nextOfKin = false))
        .exchange()
        .expectStatus()
        .isOk
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBody(SyncPrisonerContact::class.java)
        .returnResult().responseBody!!

      with(prisonerContact) {
        assertThat(nextOfKin).isFalse
      }

      val updatedPrisonerContact = webTestClient.put()
        .uri("/sync/prisoner-contact/{prisonerContactId}", prisonerContact.id)
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .headers(setAuthorisationUsingCurrentUser())
        .bodyValue(updatePrisonerContactRequest(prisonerNumber = "A1234AB", nextOfKin = true))
        .exchange()
        .expectStatus()
        .isOk
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBody(SyncPrisonerContact::class.java)
        .returnResult().responseBody!!

      // Check the updated copy
      with(updatedPrisonerContact) {
        assertThat(nextOfKin).isTrue
      }
      stubEvents.assertHasEvent(
        event = OutboundEvent.PRISONER_CONTACT_UPDATED,
        additionalInfo = PrisonerContactInfo(updatedPrisonerContact.id, Source.NOMIS, "UpdatedUser", "BXI"),
        personReference = PersonReference(dpsContactId = updatedPrisonerContact.contactId, nomsNumber = updatedPrisonerContact.prisonerNumber),
      )

      assertNextOfKinCustomCreatedEvent(updatedPrisonerContact, Source.NOMIS, User("UpdatedUser", "BXI"))
    }

    @Test
    fun `when contact exists but next of kin is true and update changes next of kin to false then a delete next of kin event is sent`() {
      val prisonerContact = webTestClient.post()
        .uri("/sync/prisoner-contact")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .headers(setAuthorisationUsingCurrentUser())
        .bodyValue(createPrisonerContactRequest("A9999AB", nextOfKin = true))
        .exchange()
        .expectStatus()
        .isOk
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBody(SyncPrisonerContact::class.java)
        .returnResult().responseBody!!

      with(prisonerContact) {
        assertThat(nextOfKin).isTrue
      }

      val updatedPrisonerContact = webTestClient.put()
        .uri("/sync/prisoner-contact/{prisonerContactId}", prisonerContact.id)
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .headers(setAuthorisationUsingCurrentUser())
        .bodyValue(updatePrisonerContactRequest("A9999AB", nextOfKin = false))
        .exchange()
        .expectStatus()
        .isOk
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBody(SyncPrisonerContact::class.java)
        .returnResult().responseBody!!

      // Check the updated copy
      with(updatedPrisonerContact) {
        assertThat(nextOfKin).isFalse
      }

      stubEvents.assertHasEvent(
        event = OutboundEvent.PRISONER_CONTACT_UPDATED,
        additionalInfo = PrisonerContactInfo(updatedPrisonerContact.id, Source.NOMIS, "UpdatedUser", "BXI"),
        personReference = PersonReference(dpsContactId = updatedPrisonerContact.contactId, nomsNumber = updatedPrisonerContact.prisonerNumber),
      )

      verify(telemetryClient, times(1)).trackEvent(
        "contact-next-of-kin-deleted",
        mapOf(
          "description" to "A contact next of kin has been deleted",
          "source" to "NOMIS",
          "username" to "UpdatedUser",
          "contactId" to updatedPrisonerContact.contactId.toString(),
          "active_caseload_id" to "BXI",
          "prisoner_contact_id" to updatedPrisonerContact.id.toString(),
        ),
        null,
      )
    }

    @Test
    fun `should delete an existing prisoner contact`() {
      val prisonerContact = webTestClient.post()
        .uri("/sync/prisoner-contact")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .headers(setAuthorisationUsingCurrentUser())
        .bodyValue(createPrisonerContactRequest("A1234BD", nextOfKin = true))
        .exchange()
        .expectStatus()
        .isOk
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBody(SyncPrisonerContact::class.java)
        .returnResult().responseBody!!

      webTestClient.delete()
        .uri("/sync/prisoner-contact/{prisonerContactId}", prisonerContact.id)
        .accept(MediaType.APPLICATION_JSON)
        .headers(setAuthorisationUsingCurrentUser())
        .exchange()
        .expectStatus()
        .isOk

      webTestClient.get()
        .uri("/sync/prisoner-contact/{prisonerContactId}", prisonerContact.id)
        .accept(MediaType.APPLICATION_JSON)
        .headers(setAuthorisationUsingCurrentUser())
        .exchange()
        .expectStatus()
        .isNotFound
      stubEvents.assertHasEvent(
        event = OutboundEvent.PRISONER_CONTACT_DELETED,
        additionalInfo = PrisonerContactInfo(prisonerContact.id, Source.NOMIS, "SYS", null),
        personReference = PersonReference(dpsContactId = prisonerContact.contactId, nomsNumber = prisonerContact.prisonerNumber),
      )

      assertCustomDeletedEvent(prisonerContact, Source.NOMIS, User("SYS"))
      assertNextOfKinCustomDeletedEvent(prisonerContact, Source.NOMIS, User("SYS"))
    }

    @Test
    fun `should prevent create duplicate relationship`() {
      val prisonerNumber = "A1234BE"
      webTestClient.post()
        .uri("/sync/prisoner-contact")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .headers(setAuthorisationUsingCurrentUser())
        .bodyValue(createPrisonerContactRequest(prisonerNumber))
        .exchange()
        .expectStatus()
        .isOk
      webTestClient.post()
        .uri("/sync/prisoner-contact")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .headers(setAuthorisationUsingCurrentUser())
        .bodyValue(createPrisonerContactRequest(prisonerNumber))
        .exchange()
        .expectStatus()
        .isEqualTo(HttpStatus.CONFLICT)
    }

    @Test
    fun `should allow create duplicate relationship if not current term`() {
      val prisonerNumber = "A1234BF"
      val request = createPrisonerContactRequest(prisonerNumber).copy(currentTerm = false)
      webTestClient.post()
        .uri("/sync/prisoner-contact")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .headers(setAuthorisationUsingCurrentUser())
        .bodyValue(request)
        .exchange()
        .expectStatus()
        .isOk

      webTestClient.post()
        .uri("/sync/prisoner-contact")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .headers(setAuthorisationUsingCurrentUser())
        .bodyValue(request)
        .exchange()
        .expectStatus()
        .isOk
    }

    private fun updatePrisonerContactRequest(prisonerNumber: String = "A1234BC", nextOfKin: Boolean = true) = SyncUpdatePrisonerContactRequest(
      contactId = 1L,
      prisonerNumber = prisonerNumber,
      contactType = "O",
      relationshipType = "LAW",
      nextOfKin = nextOfKin,
      emergencyContact = false,
      comments = "Updated relationship type to family",
      active = true,
      approvedVisitor = true,
      currentTerm = true,
      expiryDate = LocalDate.of(2025, 12, 31),
      createdAtPrison = "LONDN",
      updatedBy = "UpdatedUser",
      updatedTime = LocalDateTime.now(),
    )

    private fun createPrisonerContactRequest(prisonerNumber: String = "A1234BC", nextOfKin: Boolean = true) = SyncCreatePrisonerContactRequest(
      contactId = 1L,
      prisonerNumber = prisonerNumber,
      contactType = "S",
      relationshipType = "FRI",
      nextOfKin = nextOfKin,
      emergencyContact = false,
      comments = "Create relationship",
      active = true,
      approvedVisitor = true,
      currentTerm = true,
      expiryDate = LocalDate.of(2025, 12, 31),
      createdAtPrison = "LONDN",
      createdBy = "adminUser",
      createdTime = LocalDateTime.now(),
    )

    private fun assertCustomCreatedEvent(syncPrisonerContact: SyncPrisonerContact, source: Source, user: User) {
      verify(telemetryContactCustomEventService, times(1)).trackCreatePrisonerContactEvent(syncPrisonerContact, source, user)

      verify(telemetryClient, times(1)).trackEvent(
        "prisoner-contact-created",
        mapOf(
          "description" to "A prisoner contact has been created",
          "source" to source.name,
          "username" to user.username,
          "contactId" to syncPrisonerContact.contactId.toString(),
          "active_caseload_id" to user.activeCaseLoadId,
          "prisoner_contact_id" to syncPrisonerContact.id.toString(),
          "prisoner_number" to syncPrisonerContact.prisonerNumber,
        ),
        null,
      )
    }

    private fun assertCustomUpdatedEvent(syncPrisonerContact: SyncPrisonerContact, source: Source, user: User) {
      verify(telemetryContactCustomEventService, times(1)).trackUpdatePrisonerContactEvent(syncPrisonerContact, null, source, user)

      verify(telemetryClient, times(1)).trackEvent(
        "prisoner-contact-updated",
        mapOf(
          "description" to "A prisoner contact has been updated",
          "source" to source.name,
          "username" to user.username,
          "contactId" to syncPrisonerContact.contactId.toString(),
          "active_caseload_id" to user.activeCaseLoadId,
          "prisoner_contact_id" to syncPrisonerContact.id.toString(),
          "prisoner_number" to syncPrisonerContact.prisonerNumber,
        ),
        null,
      )
    }

    private fun assertCustomDeletedEvent(syncPrisonerContact: SyncPrisonerContact, source: Source, user: User) {
      verify(telemetryContactCustomEventService, times(1)).trackDeletePrisonerContactEvent(any<SyncPrisonerContact>(), any<Source>(), any<User>())

      verify(telemetryClient, times(1)).trackEvent(
        "prisoner-contact-deleted",
        mapOf(
          "description" to "A prisoner contact has been deleted",
          "source" to source.name,
          "username" to user.username,
          "contactId" to syncPrisonerContact.contactId.toString(),
          "prisoner_contact_id" to syncPrisonerContact.id.toString(),
          "prisoner_number" to syncPrisonerContact.prisonerNumber,
        ),
        null,
      )
    }

    private fun assertNextOfKinCustomCreatedEvent(syncPrisonerContact: SyncPrisonerContact, source: Source, user: User) {
      verify(telemetryClient, times(1)).trackEvent(
        "contact-next-of-kin-created",
        mapOf(
          "description" to "A contact next of kin has been created",
          "source" to source.name,
          "username" to user.username,
          "contactId" to syncPrisonerContact.contactId.toString(),
          "active_caseload_id" to user.activeCaseLoadId,
          "prisoner_contact_id" to syncPrisonerContact.id.toString(),
        ),
        null,
      )
    }

    private fun assertNextOfKinCustomDeletedEvent(syncPrisonerContact: SyncPrisonerContact, source: Source, user: User) {
      verify(telemetryClient, times(1)).trackEvent(
        "contact-next-of-kin-deleted",
        mapOf(
          "description" to "A contact next of kin has been deleted",
          "source" to source.name,
          "username" to user.username,
          "contactId" to syncPrisonerContact.contactId.toString(),
          "prisoner_contact_id" to syncPrisonerContact.id.toString(),
        ),
        null,
      )
    }
  }
}
