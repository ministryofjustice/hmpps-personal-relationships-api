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
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync.SyncPrisonerRelationship
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync.SyncRelationshipRestriction
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.sync.MergePrisonerContactResponse
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.sync.PrisonerContactAndRestrictionIds
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.sync.PrisonerRelationshipIds
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.PersonReference
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.PrisonerContactInfo
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.PrisonerContactRestrictionInfo
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.Source
import java.time.LocalDate

@Sql("classpath:merge.tests/data-for-merge-test.sql")
@Sql(scripts = ["classpath:merge.tests/cleanup-merge-test.sql"], executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class MergePrisonerContactsIntegrationTest : PostgresIntegrationTestBase() {

  @Nested
  inner class MergePrisonerContactTests {

    @BeforeEach
    fun resetEvents() {
      stubEvents.reset()
    }

    @Test
    fun `Merge endpoint should return unauthorized if no token provided`() {
      webTestClient.post()
        .uri("/sync/prisoner-contact/merge")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(createMergeRequest(retained = "A3333AA", removed = "A4444AA"))
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `Merge endpoint should return forbidden without an authorised role on the token`() {
      webTestClient.post()
        .uri("/sync/prisoner-contact/merge")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(createMergeRequest(retained = "A3333AA", removed = "A4444AA"))
        .headers(setAuthorisation(roles = listOf("ROLE_WRONG")))
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `should remove the prisoner contacts and restrictions with nothing to recreate - empty list provided`() {
      val mergeResponse = webTestClient.post()
        .uri("/sync/prisoner-contact/merge")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf("PERSONAL_RELATIONSHIPS_MIGRATION")))
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

        checkForExpectedEvents(relationshipsRemoved, relationshipsCreated)
      }
    }

    @Test
    fun `should remove the prisoner contacts and restrictions then recreate for the retained prisoner`() {
      val mergeResponse = webTestClient.post()
        .uri("/sync/prisoner-contact/merge")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf("PERSONAL_RELATIONSHIPS_MIGRATION")))
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

        checkForExpectedEvents(relationshipsRemoved, relationshipsCreated)
      }
    }

    private fun checkForExpectedEvents(
      relationshipsRemoved: List<PrisonerRelationshipIds>,
      relationshipsCreated: List<PrisonerContactAndRestrictionIds>,
    ) {
      val retainedPrisonerNumber = "A3333AA"

      relationshipsRemoved.map { relationship ->
        relationship.prisonerContactRestrictionIds.map { restrictionId ->
          stubEvents.assertHasEvent(
            event = OutboundEvent.PRISONER_CONTACT_RESTRICTION_DELETED,
            additionalInfo = PrisonerContactRestrictionInfo(restrictionId, Source.NOMIS),
            personReference = PersonReference(
              dpsContactId = relationship.contactId,
              nomsNumber = relationship.prisonerNumber,
            ),
          )
        }
        stubEvents.assertHasEvent(
          event = OutboundEvent.PRISONER_CONTACT_DELETED,
          additionalInfo = PrisonerContactInfo(relationship.prisonerContactId, Source.NOMIS),
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
            additionalInfo = PrisonerContactRestrictionInfo(restriction.dpsId, Source.NOMIS),
            personReference = PersonReference(
              dpsContactId = created.contactId,
              nomsNumber = retainedPrisonerNumber,
            ),
          )
        }

        stubEvents.assertHasEvent(
          event = OutboundEvent.PRISONER_CONTACT_CREATED,
          additionalInfo = PrisonerContactInfo(created.relationship.dpsId, Source.NOMIS),
          personReference = PersonReference(
            dpsContactId = created.contactId,
            nomsNumber = retainedPrisonerNumber,
          ),
        )
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
  }
}
