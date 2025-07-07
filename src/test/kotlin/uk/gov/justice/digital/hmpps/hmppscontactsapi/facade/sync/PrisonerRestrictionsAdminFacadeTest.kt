package uk.gov.justice.digital.hmpps.hmppscontactsapi.facade.sync

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppscontactsapi.config.User
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.internal.ChangedRestrictionsResponse
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.migrate.PrisonerRestrictionDetailsRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync.ResetPrisonerRestrictionsRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.OutboundEventsService
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.Source
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

      val mergePrisonerRestrictionsResponse = ChangedRestrictionsResponse(
        hasChanged = true,
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
        ChangedRestrictionsResponse(
          hasChanged = false,
        ),
      )

      facade.merge(retainingPrisonerNumber, removedPrisonerNumber)

      verifyNoMoreInteractions(outboundEventsService)
    }
  }

  @Nested
  inner class ResetRestrictions {

    @Test
    fun `should send changed event when restrictions were reset`() {
      // Given
      val response = ChangedRestrictionsResponse(
        hasChanged = true,
      )
      val request = createRequest()

      whenever(mergeService.resetPrisonerRestrictions(request)).thenReturn(response)

      // When
      facade.reset(request)

      // Then
      verify(mergeService).resetPrisonerRestrictions(request)
      verify(outboundEventsService).sendPrisonerRestrictionsChanged(
        request.prisonerNumber,
        null,
        source = Source.NOMIS,
        user = User.SYS_USER,
      )
    }

    @Test
    fun `should not send events when no restrictions were reset`() {
      // Given
      val response = ChangedRestrictionsResponse(
        hasChanged = false,
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
