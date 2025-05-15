package uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.resource.sync

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.PostgresIntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.migrate.CodedValue
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync.MergePrisonerContactRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync.ResetPrisonerContactRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync.SyncPrisonerRelationship
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync.SyncRelationshipRestriction
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.sync.MergePrisonerContactResponse
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.sync.PrisonerContactAndRestrictionIds
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.sync.PrisonerRelationshipIds
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.sync.ResetPrisonerContactResponse
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.PersonReference
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.PrisonerContactInfo
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.PrisonerContactRestrictionInfo
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.Source
import uk.gov.justice.digital.hmpps.hmppscontactsapi.util.StubUser
import java.time.LocalDate

@Sql("classpath:merge.tests/data-for-merge-test.sql")
@Sql(scripts = ["classpath:merge.tests/cleanup-merge-test.sql"], executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class SyncAdminIntegrationTest : PostgresIntegrationTestBase() {

  @BeforeEach
  fun setUp() {
    setCurrentUser(StubUser.SYNC_AND_MIGRATE_USER)
  }

  @Nested
  inner class MergeTests {

    @BeforeEach
    fun resetEvents() {
      stubEvents.reset()
    }

    @Test
    fun `should return unauthorized for a merge if no token provided`() {
      webTestClient.post()
        .uri("/sync/admin/merge")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(createMergeRequest(retained = "A3333AA", removed = "A4444AA"))
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `should return forbidden for merge without an authorised role on the token`() {
      setCurrentUser(StubUser.SYNC_AND_MIGRATE_USER.copy(roles = listOf("ROLE_WRONG")))
      webTestClient.post()
        .uri("/sync/admin/merge")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(createMergeRequest(retained = "A3333AA", removed = "A4444AA"))
        .headers(setAuthorisationUsingCurrentUser())
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `merge should remove prisoner contacts and restrictions with nothing to recreate - empty list provided`() {
      val mergeResponse = webTestClient.post()
        .uri("/sync/admin/merge")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .headers(setAuthorisationUsingCurrentUser())
        .bodyValue(createMergeRequest(retained = "A3333AA", removed = "A4444AA"))
        .exchange()
        .expectStatus()
        .isOk
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBody(MergePrisonerContactResponse::class.java)
        .returnResult().responseBody!!

      with(mergeResponse) {
        assertThat(relationshipsCreated).hasSize(0)
        assertThat(relationshipsRemoved).hasSize(5)
        assertThat(relationshipsRemoved).extracting("prisonerNumber").containsOnly("A3333AA", "A4444AA")

        checkForExpectedEvents(relationshipsRemoved, relationshipsCreated, createdPrisonerNumber = "A3333AA")
      }
    }

    @Test
    fun `merge should remove the prisoner contacts and restrictions and recreate for the retained prisoner`() {
      val mergeResponse = webTestClient.post()
        .uri("/sync/admin/merge")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .headers(setAuthorisationUsingCurrentUser())
        .bodyValue(
          createMergeRequest(
            retained = "A3333AA",
            removed = "A4444AA",
            prisonerContacts = listOf(
              aRelationship(
                id = 1,
                contactId = 30001,
                contactType = "S",
                relationshipType = "BRO",
                restrictions = listOf(
                  aRestriction(
                    id = 1,
                    startDate = LocalDate.now().minusDays(1),
                  ),
                ),
              ),
              aRelationship(
                id = 2,
                contactId = 30002,
                contactType = "O",
                relationshipType = "POL",
                restrictions = listOf(
                  aRestriction(
                    id = 2,
                    startDate = LocalDate.now().minusDays(1),
                  ),
                ),
              ),
            ),
          ),
        )
        .exchange()
        .expectStatus()
        .isOk
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBody(MergePrisonerContactResponse::class.java)
        .returnResult().responseBody!!

      with(mergeResponse) {
        assertThat(relationshipsRemoved).hasSize(5)
        assertThat(relationshipsRemoved).extracting("prisonerNumber").containsOnly("A3333AA", "A4444AA")
        assertThat(relationshipsCreated).hasSize(2)

        checkForExpectedEvents(relationshipsRemoved, relationshipsCreated, createdPrisonerNumber = "A3333AA")
      }
    }

    private fun createMergeRequest(
      retained: String,
      removed: String,
      prisonerContacts: List<SyncPrisonerRelationship> = emptyList(),
    ) = MergePrisonerContactRequest(
      retainedPrisonerNumber = retained,
      prisonerContacts,
      removedPrisonerNumber = removed,
    )
  }

  @Nested
  inner class ResetTests {

    @BeforeEach
    fun resetEvents() {
      stubEvents.reset()
    }

    @Test
    fun `should return unauthorized for a reset if no token provided`() {
      webTestClient.post()
        .uri("/sync/admin/reset")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(createResetRequest(prisonerNumber = "A3333AA"))
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `should return forbidden for a reset without an authorised role on the token`() {
      setCurrentUser(StubUser.SYNC_AND_MIGRATE_USER.copy(roles = listOf("ROLE_WRONG")))
      webTestClient.post()
        .uri("/sync/admin/reset")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(createResetRequest(prisonerNumber = "A3333AA"))
        .headers(setAuthorisationUsingCurrentUser())
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `reset should remove prisoner contacts and restrictions with nothing to recreate - empty list provided`() {
      val resetResponse = webTestClient.post()
        .uri("/sync/admin/reset")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .headers(setAuthorisationUsingCurrentUser())
        .bodyValue(createResetRequest(prisonerNumber = "A3333AA"))
        .exchange()
        .expectStatus()
        .isOk
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBody(ResetPrisonerContactResponse::class.java)
        .returnResult().responseBody!!

      with(resetResponse) {
        assertThat(relationshipsCreated).hasSize(0)
        assertThat(relationshipsRemoved).hasSize(2)
        assertThat(relationshipsRemoved).extracting("prisonerNumber").containsOnly("A3333AA")
        val restrictionsRemoved = relationshipsRemoved.map { relationship ->
          relationship.prisonerContactRestrictionIds
        }.flatten()
        assertThat(restrictionsRemoved).hasSize(2)

        checkForExpectedEvents(relationshipsRemoved, relationshipsCreated, createdPrisonerNumber = "A3333AA")
      }
    }

    @Test
    fun `reset should remove the prisoner contacts and restrictions and recreate for this prisoner`() {
      val resetResponse = webTestClient.post()
        .uri("/sync/admin/reset")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .headers(setAuthorisationUsingCurrentUser())
        .bodyValue(
          createResetRequest(
            prisonerNumber = "A4444AA",
            prisonerContacts = listOf(
              aRelationship(
                id = 1,
                contactId = 30001,
                contactType = "S",
                relationshipType = "BRO",
                restrictions = listOf(
                  aRestriction(
                    id = 1,
                    startDate = LocalDate.now().minusDays(1),
                  ),
                ),
              ),
              aRelationship(
                id = 2,
                contactId = 30002,
                contactType = "O",
                relationshipType = "POL",
                restrictions = listOf(
                  aRestriction(
                    id = 2,
                    startDate = LocalDate.now().minusDays(1),
                  ),
                ),
              ),
            ),
          ),
        )
        .exchange()
        .expectStatus()
        .isOk
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBody(ResetPrisonerContactResponse::class.java)
        .returnResult().responseBody!!

      with(resetResponse) {
        assertThat(relationshipsRemoved).hasSize(3)
        assertThat(relationshipsRemoved).extracting("prisonerNumber").containsOnly("A4444AA")
        assertThat(relationshipsCreated).hasSize(2)

        val restrictionsRemoved = relationshipsRemoved.map { relationship ->
          relationship.prisonerContactRestrictionIds
        }.flatten()

        assertThat(restrictionsRemoved).hasSize(0)

        val restrictionsCreated = relationshipsCreated.map { relationship ->
          relationship.restrictions.map { it.dpsId }
        }.flatten()

        assertThat(restrictionsCreated).hasSize(2)

        checkForExpectedEvents(relationshipsRemoved, relationshipsCreated, createdPrisonerNumber = "A4444AA")
      }
    }

    private fun createResetRequest(
      prisonerNumber: String,
      prisonerContacts: List<SyncPrisonerRelationship> = emptyList(),
    ) = ResetPrisonerContactRequest(prisonerNumber, prisonerContacts)
  }

  private fun aRelationship(
    id: Long,
    contactId: Long,
    contactType: String = "S",
    relationshipType: String = "BRO",
    restrictions: List<SyncRelationshipRestriction> = emptyList(),
  ) = SyncPrisonerRelationship(
    id = id,
    contactId = contactId,
    contactType = CodedValue(code = contactType, description = "contact type"),
    relationshipType = CodedValue(code = relationshipType, description = "relationship type"),
    currentTerm = true,
    active = true,
    approvedVisitor = true,
    nextOfKin = true,
    emergencyContact = true,
    prisonerNumber = "A3333AA",
    comment = "comment",
    restrictions = restrictions,
  )

  private fun aRestriction(
    id: Long,
    restrictionType: String = "BAN",
    comment: String = "comment",
    startDate: LocalDate,
    expiryDate: LocalDate? = null,
  ) = SyncRelationshipRestriction(
    id = id,
    restrictionType = CodedValue(code = restrictionType, description = "restriction type"),
    comment = comment,
    startDate = startDate,
    expiryDate = expiryDate,
  )

  private fun checkForExpectedEvents(
    relationshipsRemoved: List<PrisonerRelationshipIds>,
    relationshipsCreated: List<PrisonerContactAndRestrictionIds>,
    createdPrisonerNumber: String,
  ) {
    relationshipsRemoved.map { relationship ->
      relationship.prisonerContactRestrictionIds.map { restrictionId ->
        stubEvents.assertHasEvent(
          event = OutboundEvent.PRISONER_CONTACT_RESTRICTION_DELETED,
          additionalInfo = PrisonerContactRestrictionInfo(restrictionId, Source.NOMIS, "SYS", null),
          personReference = PersonReference(
            dpsContactId = relationship.contactId,
            nomsNumber = relationship.prisonerNumber,
          ),
        )
      }
      stubEvents.assertHasEvent(
        event = OutboundEvent.PRISONER_CONTACT_DELETED,
        additionalInfo = PrisonerContactInfo(relationship.prisonerContactId, Source.NOMIS, "SYS", null),
        personReference = PersonReference(
          dpsContactId = relationship.contactId,
          nomsNumber = relationship.prisonerNumber,
        ),
      )
    }

    relationshipsCreated.map { created ->
      created.restrictions.map { restriction ->
        stubEvents.assertHasEvent(
          event = OutboundEvent.PRISONER_CONTACT_RESTRICTION_CREATED,
          additionalInfo = PrisonerContactRestrictionInfo(restriction.dpsId, Source.NOMIS, "SYS", null),
          personReference = PersonReference(
            dpsContactId = created.contactId,
            nomsNumber = createdPrisonerNumber,
          ),
        )
      }

      stubEvents.assertHasEvent(
        event = OutboundEvent.PRISONER_CONTACT_CREATED,
        additionalInfo = PrisonerContactInfo(created.relationship.dpsId, Source.NOMIS, "SYS", null),
        personReference = PersonReference(
          dpsContactId = created.contactId,
          nomsNumber = createdPrisonerNumber,
        ),
      )
    }
  }
}
