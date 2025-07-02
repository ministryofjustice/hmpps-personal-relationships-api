package uk.gov.justice.digital.hmpps.hmppscontactsapi.facade.sync

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppscontactsapi.config.User
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.PrisonerRestriction
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.OutboundEventsService
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.Source
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.sync.DeleteRestrictionsResponse
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.sync.MergeRestrictionsResponse
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.sync.PrisonerRestrictionsAdminService
import java.time.LocalDate
import java.time.LocalDateTime

class PrisonerRestrictionsAdminFacadeTest {
  private val mergeService = mock<PrisonerRestrictionsAdminService>()
  private val outboundEventsService = mock<OutboundEventsService>()
  private val facade = PrisonerRestrictionsAdminFacade(mergeService, outboundEventsService)

  @Nested
  inner class MergeRestrictions {
    @Test
    fun `merge send created event`() {
      val keepingPrisonerNumber = "A1234BC"
      val removingPrisonerNumber = "A4567BC"

      val mergePrisonerRestrictionsResponse = MergeRestrictionsResponse(
        wasUpdated = true,
      )
      whenever(
        mergeService.mergePrisonerRestrictions(keepingPrisonerNumber, removingPrisonerNumber),
      ).thenReturn(mergePrisonerRestrictionsResponse)

      facade.merge(keepingPrisonerNumber, removingPrisonerNumber)

      verify(outboundEventsService).sendPrisonerRestrictionsChanged(
        keepingPrisonerNumber,
        removingPrisonerNumber,
        source = Source.NOMIS,
        user = User.SYS_USER,
      )
    }

    @Test
    fun `should not send event`() {
      val retainingPrisonerNumber = "A1234BC"
      val removedPrisonerNumber = "A4567BC"

      whenever(mergeService.mergePrisonerRestrictions(retainingPrisonerNumber, removedPrisonerNumber)).thenReturn(
        MergeRestrictionsResponse(
          wasUpdated = false,
        ),
      )

      facade.merge(retainingPrisonerNumber, removedPrisonerNumber)

      verifyNoMoreInteractions(outboundEventsService)
    }
  }

  @Nested
  inner class ResetRestrictions {
    private val prisonerNumber = "A1234BC"

    @Test
    fun `should send PRISONER_RESTRICTION_DELETED event for each deleted restriction`() {
      // Given
      val restriction1 = createPrisonerRestriction(1L)
      val restriction2 = createPrisonerRestriction(2L)
      val response = DeleteRestrictionsResponse(
        wasDeleted = true,
        deletedRestrictions = listOf(restriction1, restriction2),
      )

      whenever(mergeService.resetPrisonerRestrictions(prisonerNumber)).thenReturn(response)

      // When
      facade.reset(prisonerNumber)

      // Then
      verify(mergeService).resetPrisonerRestrictions(prisonerNumber)
      verify(outboundEventsService).send(
        outboundEvent = OutboundEvent.PRISONER_RESTRICTION_DELETED,
        identifier = restriction1.prisonerRestrictionId,
        noms = prisonerNumber,
        source = Source.NOMIS,
        user = User.SYS_USER,
      )
      verify(outboundEventsService).send(
        outboundEvent = OutboundEvent.PRISONER_RESTRICTION_DELETED,
        identifier = restriction2.prisonerRestrictionId,
        noms = prisonerNumber,
        source = Source.NOMIS,
        user = User.SYS_USER,
      )
    }

    @Test
    fun `should not send events when no restrictions were deleted`() {
      // Given
      val response = DeleteRestrictionsResponse(
        wasDeleted = false,
        deletedRestrictions = emptyList(),
      )

      whenever(mergeService.resetPrisonerRestrictions(prisonerNumber)).thenReturn(response)

      // When
      facade.reset(prisonerNumber)

      // Then
      verify(mergeService).resetPrisonerRestrictions(prisonerNumber)
      verifyNoMoreInteractions(outboundEventsService)
    }

    private fun createPrisonerRestriction(id: Long) = PrisonerRestriction(
      prisonerRestrictionId = id,
      prisonerNumber = "A1234BC",
      restrictionType = "NO_VISIT",
      effectiveDate = LocalDate.of(2024, 1, 1),
      expiryDate = LocalDate.of(2024, 12, 31),
      commentText = "Test restriction",
      authorisedUsername = "AUSER",
      currentTerm = true,
      createdBy = "user1",
      createdTime = LocalDateTime.now(),
      updatedBy = "user2",
      updatedTime = LocalDateTime.now(),
    )
  }
}
