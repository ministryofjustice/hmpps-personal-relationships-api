package uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.resource.sync

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.PostgresIntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync.SyncCreatePrisonerContactRestrictionRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync.SyncUpdatePrisonerContactRestrictionRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.sync.SyncPrisonerContactRestriction
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.PersonReference
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.PrisonerContactRestrictionInfo
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.Source
import uk.gov.justice.digital.hmpps.hmppscontactsapi.util.StubUser
import java.time.LocalDate
import java.time.LocalDateTime

class SyncPrisonerContactRestrictionIntegrationTest : PostgresIntegrationTestBase() {

  @BeforeEach
  fun setUp() {
    setCurrentUser(StubUser.SYNC_AND_MIGRATE_USER)
  }

  @Nested
  inner class PrisonerContactRestrictionEntitySyncTests {

    @Test
    fun `Sync endpoints should return unauthorized if no token provided`() {
      webTestClient.get()
        .uri("/sync/prisoner-contact-restriction/1")
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isUnauthorized

      webTestClient.put()
        .uri("/sync/prisoner-contact-restriction")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(createPrisonerContactRestrictionRequest())
        .exchange()
        .expectStatus()
        .isUnauthorized

      webTestClient.post()
        .uri("/sync/prisoner-contact-restriction/1")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(updatePrisonerContactRestrictionRequest())
        .exchange()
        .expectStatus()
        .isUnauthorized

      webTestClient.delete()
        .uri("/sync/prisoner-contact-restriction/1")
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `Sync endpoints should return forbidden without an authorised role on the token`() {
      setCurrentUser(StubUser.SYNC_AND_MIGRATE_USER.copy(roles = listOf("ROLE_WRONG")))
      webTestClient.get()
        .uri("/sync/prisoner-contact-restriction/1")
        .accept(MediaType.APPLICATION_JSON)
        .headers(setAuthorisationUsingCurrentUser())
        .exchange()
        .expectStatus()
        .isForbidden

      webTestClient.post()
        .uri("/sync/prisoner-contact-restriction")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(createPrisonerContactRestrictionRequest())
        .headers(setAuthorisationUsingCurrentUser())
        .exchange()
        .expectStatus()
        .isForbidden

      webTestClient.put()
        .uri("/sync/prisoner-contact-restriction/1")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(updatePrisonerContactRestrictionRequest())
        .headers(setAuthorisationUsingCurrentUser())
        .exchange()
        .expectStatus()
        .isForbidden

      webTestClient.delete()
        .uri("/sync/prisoner-contact-restriction/1")
        .accept(MediaType.APPLICATION_JSON)
        .headers(setAuthorisationUsingCurrentUser())
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `should get an existing prisoner contact restriction`() {
      // From base data
      val contactId = 1L
      val prisonerContactRestriction = webTestClient.get()
        .uri("/sync/prisoner-contact-restriction/{id}", contactId)
        .accept(MediaType.APPLICATION_JSON)
        .headers(setAuthorisationUsingCurrentUser())
        .exchange()
        .expectStatus()
        .isOk
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBody(SyncPrisonerContactRestriction::class.java)
        .returnResult().responseBody!!

      with(prisonerContactRestriction) {
        assertThat(prisonerContactRestrictionId).isEqualTo(1L)
        assertThat(contactId).isEqualTo(1L)
        assertThat(restrictionType).isEqualTo("PREINF")
        assertThat(startDate).isEqualTo(LocalDate.of(2024, 1, 1))
        assertThat(expiryDate).isEqualTo(LocalDate.of(2024, 12, 31))
        assertThat(comments).isEqualTo("Restriction due to ongoing investigation")
        assertThat(createdBy).isEqualTo("admin")
        assertThat(createdTime).isEqualTo(LocalDateTime.of(2024, 10, 1, 12, 0, 0))
        assertThat(updatedBy).isEqualTo("editor")
        assertThat(updatedTime).isEqualTo(LocalDateTime.of(2024, 10, 2, 15, 30, 0))
      }
    }

    @Test
    fun `should create a new prisoner contact restriction`() {
      val prisonerContactRestriction = webTestClient.post()
        .uri("/sync/prisoner-contact-restriction")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .headers(setAuthorisationUsingCurrentUser())
        .bodyValue(createPrisonerContactRestrictionRequest())
        .exchange()
        .expectStatus()
        .isOk
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBody(SyncPrisonerContactRestriction::class.java)
        .returnResult().responseBody!!

      // The created is returned
      with(prisonerContactRestriction) {
        assertThat(prisonerContactRestrictionId).isGreaterThan(0)
        assertThat(contactId).isEqualTo(1L)
        assertThat(restrictionType).isEqualTo("PREINF")
        assertThat(startDate).isEqualTo(LocalDate.of(2024, 1, 1))
        assertThat(expiryDate).isEqualTo(LocalDate.of(2024, 12, 31))
        assertThat(comments).isEqualTo("Restriction due to ongoing investigation")
        assertThat(createdBy).isEqualTo("admin")
        assertThat(createdTime).isAfter(LocalDateTime.now().minusMinutes(5))
        assertThat(updatedBy).isNull()
        assertThat(updatedTime).isNull()
      }
      stubEvents.assertHasEvent(
        event = OutboundEvent.PRISONER_CONTACT_RESTRICTION_CREATED,
        additionalInfo = PrisonerContactRestrictionInfo(prisonerContactRestriction.prisonerContactRestrictionId, Source.NOMIS, "admin"),
        personReference = PersonReference(dpsContactId = prisonerContactRestriction.contactId, nomsNumber = prisonerContactRestriction.prisonerNumber),
      )
    }

    @Test
    fun `should create and then update a prisoner contact restriction`() {
      val prisonerContactRestriction = webTestClient.post()
        .uri("/sync/prisoner-contact-restriction")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .headers(setAuthorisationUsingCurrentUser())
        .bodyValue(createPrisonerContactRestrictionRequest())
        .exchange()
        .expectStatus()
        .isOk
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBody(SyncPrisonerContactRestriction::class.java)
        .returnResult().responseBody!!

      with(prisonerContactRestriction) {
        assertThat(prisonerContactRestrictionId).isGreaterThan(3L)
        assertThat(contactId).isEqualTo(1L)
        assertThat(restrictionType).isEqualTo("PREINF")
        assertThat(startDate).isEqualTo(LocalDate.of(2024, 1, 1))
        assertThat(expiryDate).isEqualTo(LocalDate.of(2024, 12, 31))
        assertThat(comments).isEqualTo("Restriction due to ongoing investigation")
        assertThat(createdBy).isEqualTo("admin")
        assertThat(createdTime).isAfter(LocalDateTime.now().minusMinutes(5))
        assertThat(updatedBy).isNull()
        assertThat(updatedTime).isNull()
      }

      val updatedPrisonerContactRestriction = webTestClient.put()
        .uri("/sync/prisoner-contact-restriction/{id}", prisonerContactRestriction.prisonerContactRestrictionId)
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .headers(setAuthorisationUsingCurrentUser())
        .bodyValue(updatePrisonerContactRestrictionRequest())
        .exchange()
        .expectStatus()
        .isOk
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBody(SyncPrisonerContactRestriction::class.java)
        .returnResult().responseBody!!

      // Check the updated copy
      with(updatedPrisonerContactRestriction) {
        assertThat(prisonerContactRestrictionId).isGreaterThan(3L)
        assertThat(contactId).isEqualTo(1L)
        assertThat(restrictionType).isEqualTo("PREINF")
        assertThat(startDate).isEqualTo(LocalDate.of(2024, 1, 1))
        assertThat(expiryDate).isEqualTo(LocalDate.of(2024, 12, 31))
        assertThat(comments).isEqualTo("Restriction due to ongoing investigation")
        assertThat(createdBy).isEqualTo("admin")
        assertThat(createdTime).isAfter(LocalDateTime.now().minusMinutes(5))
        assertThat(updatedBy).isEqualTo("UpdatedUser")
        assertThat(updatedTime).isAfter(LocalDateTime.now().minusMinutes(5))
      }
      stubEvents.assertHasEvent(
        event = OutboundEvent.PRISONER_CONTACT_RESTRICTION_UPDATED,
        additionalInfo = PrisonerContactRestrictionInfo(updatedPrisonerContactRestriction.prisonerContactRestrictionId, Source.NOMIS, "UpdatedUser"),
        personReference = PersonReference(dpsContactId = updatedPrisonerContactRestriction.contactId, nomsNumber = updatedPrisonerContactRestriction.prisonerNumber),
      )
    }

    @Test
    fun `should delete an existing prisoner contact restriction`() {
      val prisonerContactRestriction = webTestClient.post()
        .uri("/sync/prisoner-contact-restriction")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .headers(setAuthorisationUsingCurrentUser())
        .bodyValue(createPrisonerContactRestrictionRequest())
        .exchange()
        .expectStatus()
        .isOk
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBody(SyncPrisonerContactRestriction::class.java)
        .returnResult().responseBody!!

      webTestClient.delete()
        .uri("/sync/prisoner-contact-restriction/{id}", prisonerContactRestriction.prisonerContactRestrictionId)
        .accept(MediaType.APPLICATION_JSON)
        .headers(setAuthorisationUsingCurrentUser())
        .exchange()
        .expectStatus()
        .isOk

      webTestClient.get()
        .uri("/sync/prisoner-contact-restriction/{id}", prisonerContactRestriction.prisonerContactRestrictionId)
        .accept(MediaType.APPLICATION_JSON)
        .headers(setAuthorisationUsingCurrentUser())
        .exchange()
        .expectStatus()
        .isNotFound
      stubEvents.assertHasEvent(
        event = OutboundEvent.PRISONER_CONTACT_RESTRICTION_DELETED,
        additionalInfo = PrisonerContactRestrictionInfo(prisonerContactRestriction.prisonerContactRestrictionId, Source.NOMIS, "SYS"),
        personReference = PersonReference(dpsContactId = prisonerContactRestriction.contactId, nomsNumber = prisonerContactRestriction.prisonerNumber),
      )
    }

    private fun updatePrisonerContactRestrictionRequest() = SyncUpdatePrisonerContactRestrictionRequest(
      restrictionType = "PREINF",
      startDate = LocalDate.of(2024, 1, 1),
      expiryDate = LocalDate.of(2024, 12, 31),
      comments = "Restriction due to ongoing investigation",
      updatedBy = "UpdatedUser",
      updatedTime = LocalDateTime.now(),
    )

    private fun createPrisonerContactRestrictionRequest() = SyncCreatePrisonerContactRestrictionRequest(
      prisonerContactId = 1L,
      restrictionType = "PREINF",
      startDate = LocalDate.of(2024, 1, 1),
      expiryDate = LocalDate.of(2024, 12, 31),
      comments = "Restriction due to ongoing investigation",
      createdBy = "admin",
      createdTime = LocalDateTime.now(),
    )
  }
}
