package uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.resource.sync

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppscontactsapi.config.User
import uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.PostgresIntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.migrate.MigratePrisonerRestrictionsRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.migrate.PrisonerRestrictionDetailsRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.migrate.PrisonerRestrictionsMigrationResponse
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.PrisonerRestrictionsRepository
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.PersonReference
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.PrisonerRestrictionInfo
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.Source
import uk.gov.justice.digital.hmpps.hmppscontactsapi.util.StubUser
import java.time.LocalDate
import java.time.LocalDateTime

class PrisonerRestrictionsResetIntegrationTest : PostgresIntegrationTestBase() {
  @Autowired
  private lateinit var prisonerRestrictionRepository: PrisonerRestrictionsRepository
  companion object {
    private const val PRISONER_NUMBER = "A1234BC"
    private const val RESET_URI = "/prisoner-restrictions/reset/$PRISONER_NUMBER"
  }

  @Nested
  inner class ResetPrisonerRestrictionsTest {

    @BeforeEach
    fun setUp() {
      setCurrentUser(StubUser.SYNC_AND_MIGRATE_USER)
      prisonerRestrictionRepository.deleteAll()
      stubEvents.reset()
    }

    @Test
    fun `should return unauthorized when no token provided for reset`() {
      webTestClient.post()
        .uri(RESET_URI)
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `should return forbidden when user lacks authorised role for reset`() {
      setCurrentUser(StubUser.USER_WITH_NO_ROLES)
      webTestClient.post()
        .uri(RESET_URI)
        .headers(setAuthorisationUsingCurrentUser())
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `should return 200 with no events fired when no restrictions exist`() {
      webTestClient.post()
        .uri(RESET_URI)
        .headers(setAuthorisationUsingCurrentUser())
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus().isOk

      stubEvents.assertHasNoEvents(event = OutboundEvent.PRISONER_RESTRICTION_DELETED)
    }

    @Test
    fun `should delete all restrictions and return them in response`() {
      // Given
      val restrictions = migratePrisonerRestrictions()

      // When/Then
      webTestClient.post()
        .uri(RESET_URI)
        .headers(setAuthorisationUsingCurrentUser())
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus().isOk
        .expectBody()

      // Verify restrictions were deleted
      assertThat(prisonerRestrictionRepository.findByPrisonerNumber(PRISONER_NUMBER)).isEmpty()

      // Verify events were published
      restrictions.prisonerRestrictionsIds.forEach(
        { prisonerRestrictionId ->
          stubEvents.assertHasEvent(
            event = OutboundEvent.PRISONER_RESTRICTION_DELETED,
            additionalInfo = PrisonerRestrictionInfo(prisonerRestrictionId, Source.NOMIS, User.SYS_USER.username, null),
            personReference = PersonReference(nomsNumber = PRISONER_NUMBER),
          )
        },
      )
    }

    private fun migratePrisonerRestrictions(): PrisonerRestrictionsMigrationResponse = webTestClient.post()
      .uri("/migrate/prisoner-restrictions")
      .headers(setAuthorisationUsingCurrentUser())
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(
        MigratePrisonerRestrictionsRequest(
          prisonerNumber = PRISONER_NUMBER,
          restrictions = listOf(prisonerRestrictionDetailsRequest(), prisonerRestrictionDetailsRequest()),
        ),
      )
      .exchange()
      .expectStatus()
      .isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(PrisonerRestrictionsMigrationResponse::class.java)
      .returnResult().responseBody!!

    private fun prisonerRestrictionDetailsRequest(
      restrictionType: String = "CCTV",
      commentText: String = "No visits allowed",
      authorisedUsername: String = "JSMITH",
    ) = PrisonerRestrictionDetailsRequest(
      restrictionType,
      effectiveDate = LocalDate.of(2024, 1, 1),
      expiryDate = LocalDate.of(2024, 12, 31),
      commentText,
      authorisedUsername,
      currentTerm = true,
      createdBy = "user1",
      createdTime = LocalDateTime.now(),
      updatedBy = "user2",
      updatedTime = LocalDateTime.now().plusDays(1),
    )
  }
}
