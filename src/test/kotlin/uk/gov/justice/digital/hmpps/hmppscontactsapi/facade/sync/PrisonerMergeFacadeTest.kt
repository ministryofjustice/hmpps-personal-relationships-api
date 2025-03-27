package uk.gov.justice.digital.hmpps.hmppscontactsapi.facade.sync

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.OutboundEventsService
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.Source
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.sync.MergeResponse
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.sync.PrisonerMergeService

class PrisonerMergeFacadeTest {
  private val mergeService = mock<PrisonerMergeService>()
  private val outboundEventsService = mock<OutboundEventsService>()
  private val facade = PrisonerMergeFacade(mergeService, outboundEventsService)

  @Nested
  inner class MergeNumberOfChildren {
    @Test
    fun `merge send created event`() {
      val retainingPrisonerNumber = "A1234BC"
      val removedPrisonerNumber = "A4567BC"
      val updatedResponse = MergeResponse(
        id = 2L,
        wasCreated = true,
      )

      whenever(mergeService.mergeNumberOfChildren(retainingPrisonerNumber, removedPrisonerNumber)).thenReturn(
        updatedResponse,
      )
      whenever(mergeService.mergeDomesticStatus(retainingPrisonerNumber, removedPrisonerNumber)).thenReturn(
        updatedResponse,
      )
      facade.merge(retainingPrisonerNumber, removedPrisonerNumber)

      verify(outboundEventsService).send(
        outboundEvent = OutboundEvent.PRISONER_NUMBER_OF_CHILDREN_CREATED,
        identifier = updatedResponse.id,
        noms = retainingPrisonerNumber,
        source = Source.DPS,
      )
      verify(outboundEventsService).send(
        outboundEvent = OutboundEvent.PRISONER_DOMESTIC_STATUS_CREATED,
        identifier = updatedResponse.id,
        noms = retainingPrisonerNumber,
        source = Source.DPS,
      )
    }

    @Test
    fun `should not send event`() {
      val retainingPrisonerNumber = "A1234BC"
      val removedPrisonerNumber = "A4567BC"
      val updatedResponse = MergeResponse(
        id = 1L,
        wasCreated = false,
      )

      whenever(mergeService.mergeNumberOfChildren(retainingPrisonerNumber, removedPrisonerNumber)).thenReturn(
        updatedResponse,
      )
      whenever(mergeService.mergeDomesticStatus(retainingPrisonerNumber, removedPrisonerNumber)).thenReturn(
        updatedResponse,
      )

      facade.merge(retainingPrisonerNumber, removedPrisonerNumber)

      verifyNoMoreInteractions(outboundEventsService)
    }

    @Test
    fun `should handle mixed created responses`() {
      val retainingPrisonerNumber = "A1234BC"
      val removedPrisonerNumber = "A4567BC"
      val childrenResponse = MergeResponse(
        id = 1L,
        wasCreated = true,
      )
      val domesticResponse = MergeResponse(
        id = 2L,
        wasCreated = false,
      )

      whenever(mergeService.mergeNumberOfChildren(retainingPrisonerNumber, removedPrisonerNumber)).thenReturn(
        childrenResponse,
      )
      whenever(mergeService.mergeDomesticStatus(retainingPrisonerNumber, removedPrisonerNumber)).thenReturn(
        domesticResponse,
      )

      facade.merge(retainingPrisonerNumber, removedPrisonerNumber)

      verify(outboundEventsService).send(
        outboundEvent = OutboundEvent.PRISONER_NUMBER_OF_CHILDREN_CREATED,
        identifier = childrenResponse.id,
        noms = retainingPrisonerNumber,
        source = Source.DPS,
      )
    }
  }
}
