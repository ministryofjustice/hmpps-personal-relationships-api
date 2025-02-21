package uk.gov.justice.digital.hmpps.hmppscontactsapi.facade.sync

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync.SyncUpdatePrisonerNumberOfChildrenRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.sync.SyncPrisonerNumberOfChildrenResponse
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.OutboundEventsService
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.PrisonerNumberOfChildren
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.Source
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.sync.SyncPrisonerNumberOfChildrenService
import java.time.LocalDateTime

class PrisonerNumberOfChildrenSyncFacadeTest {

  private val syncNumberOfChildrenService = mock<SyncPrisonerNumberOfChildrenService>()
  private val outboundEventsService = mock<OutboundEventsService>()
  private lateinit var facade: PrisonerNumberOfChildrenSyncFacade

  @BeforeEach
  fun setUp() {
    facade = PrisonerNumberOfChildrenSyncFacade(syncNumberOfChildrenService, outboundEventsService)
  }

  @Nested
  inner class GetNumberOfChildrenByPrisonerNumber {
    @Test
    fun `should return number of children when prisoner number exists`() {
      val prisonerNumber = "A1234BC"
      val expectedNumberOfChildrenCount = SyncPrisonerNumberOfChildrenResponse(
        id = 1L,
      )

      whenever(syncNumberOfChildrenService.getNumberOfChildrenByPrisonerNumber(prisonerNumber))
        .thenReturn(expectedNumberOfChildrenCount)

      val result = facade.getNumberOfChildrenByPrisonerNumber(prisonerNumber)

      verify(syncNumberOfChildrenService).getNumberOfChildrenByPrisonerNumber(prisonerNumber)
      assert(result == expectedNumberOfChildrenCount)
    }
  }

  @Nested
  inner class UpdateNumberOfChildren {
    @Test
    fun `should update number of children and send event`() {
      val prisonerNumber = "A1234BC"
      val request = SyncUpdatePrisonerNumberOfChildrenRequest(
        numberOfChildren = "1",
        createdBy = "User",
        createdTime = LocalDateTime.now(),
      )
      val updatedNumberOfChildrenCount = SyncPrisonerNumberOfChildrenResponse(
        id = 1L,
      )

      whenever(syncNumberOfChildrenService.createOrUpdateNumberOfChildren(prisonerNumber, request))
        .thenReturn(updatedNumberOfChildrenCount)

      val result = facade.updateNumberOfChildren(prisonerNumber, request)

      verify(syncNumberOfChildrenService).createOrUpdateNumberOfChildren(prisonerNumber, request)
      verify(outboundEventsService).send(
        outboundEvent = OutboundEvent.PRISONER_DEPENDENTS_CREATED,
        identifier = updatedNumberOfChildrenCount.id,
        additionalInformation = PrisonerNumberOfChildren(
          numberOfChildrenId = updatedNumberOfChildrenCount.id,
          source = Source.NOMIS,
        ),
      )
      assert(result == updatedNumberOfChildrenCount)
    }
  }

  @Nested
  inner class DeleteNumberOfChildren {
    @Test
    fun `should delete number of children and send event`() {
      val prisonerNumber = "A1234BC"
      val deletedNumberOfChildrenCount = SyncPrisonerNumberOfChildrenResponse(
        id = 1L,
      )

      whenever(syncNumberOfChildrenService.deactivateNumberOfChildren(prisonerNumber))
        .thenReturn(deletedNumberOfChildrenCount)

      facade.deleteNumberOfChildren(prisonerNumber)

      verify(syncNumberOfChildrenService).deactivateNumberOfChildren(prisonerNumber)
      verify(outboundEventsService).send(
        outboundEvent = OutboundEvent.PRISONER_DEPENDENTS_DELETED,
        identifier = deletedNumberOfChildrenCount.id,
        additionalInformation = PrisonerNumberOfChildren(
          numberOfChildrenId = deletedNumberOfChildrenCount.id,
          source = Source.NOMIS,
        ),
      )
    }
  }
}
