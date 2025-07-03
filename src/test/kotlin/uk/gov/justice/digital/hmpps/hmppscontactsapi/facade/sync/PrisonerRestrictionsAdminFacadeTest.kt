package uk.gov.justice.digital.hmpps.hmppscontactsapi.facade.sync

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppscontactsapi.config.User
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.migrate.PrisonerRestrictionDetailsRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync.ResetPrisonerRestrictionsRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.OutboundEventsService
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.Source
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.sync.PrisonerRestrictionsAdminService
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.sync.PrisonerRestrictionsAdminService.DeleteRestrictionsResponse
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.sync.PrisonerRestrictionsAdminService.MergeRestrictionsResponse
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
    fun `should send deleted and created event for each deleted and created restrictions`() {
      // Given
      val response = DeleteRestrictionsResponse(
        wasDeleted = true,
        createdRestrictions = listOf(1L),
        deletedRestrictions = listOf(2L),
      )
      val request = createRequest()

      whenever(mergeService.resetPrisonerRestrictions(request)).thenReturn(response)

      // When
      facade.reset(request)

      // Then
      verify(mergeService).resetPrisonerRestrictions(request)
      response.deletedRestrictions.forEach {
        verify(outboundEventsService).send(
          outboundEvent = OutboundEvent.PRISONER_RESTRICTION_DELETED,
          identifier = it,
          noms = prisonerNumber,
          source = Source.NOMIS,
          user = User.SYS_USER,
        )
      }
      response.createdRestrictions.forEach {
        verify(outboundEventsService).send(
          outboundEvent = OutboundEvent.PRISONER_RESTRICTION_CREATED,
          identifier = it,
          noms = prisonerNumber,
          source = Source.NOMIS,
          user = User.SYS_USER,
        )
      }
    }

    @Test
    fun `should not send events when no restrictions were created or deleted`() {
      // Given
      val response = DeleteRestrictionsResponse(
        wasDeleted = false,
        createdRestrictions = emptyList(),
        deletedRestrictions = emptyList(),
      )
      val request = createRequest()

      whenever(mergeService.resetPrisonerRestrictions(request)).thenReturn(response)

      // When
      facade.reset(request)

      // Then
      verify(mergeService).resetPrisonerRestrictions(request)
      verifyNoMoreInteractions(outboundEventsService)
    }

    private fun createRequest() = ResetPrisonerRestrictionsRequest(
      prisonerNumber = "A1234BC",
      restrictions = listOf(
        PrisonerRestrictionDetailsRequest(
          restrictionType = "NO_VISIT",
          effectiveDate = LocalDate.now(),
          expiryDate = LocalDate.now().plusDays(1),
          commentText = "Test comment",
          currentTerm = true,
          authorisedUsername = "user",
          createdBy = "user",
          createdTime = LocalDateTime.now(),
          updatedBy = "user",
          updatedTime = LocalDateTime.now(),
        ),
      ),
    )
  }
}
