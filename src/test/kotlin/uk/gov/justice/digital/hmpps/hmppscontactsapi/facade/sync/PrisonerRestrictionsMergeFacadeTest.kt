package uk.gov.justice.digital.hmpps.hmppscontactsapi.facade.sync

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppscontactsapi.config.User
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.OutboundEventsService
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.Source
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.sync.MergeRestrictionsResponse
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.sync.PrisonerRestrictionsMergeService

class PrisonerRestrictionsMergeFacadeTest {
  private val mergeService = mock<PrisonerRestrictionsMergeService>()
  private val outboundEventsService = mock<OutboundEventsService>()
  private val facade = PrisonerRestrictionsMergeFacade(mergeService, outboundEventsService)

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
}
