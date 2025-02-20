package uk.gov.justice.digital.hmpps.hmppscontactsapi.facade.sync

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync.SyncUpdatePrisonerDomesticStatusRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.sync.SyncPrisonerDomesticStatusResponse
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.OutboundEventsService
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.PrisonerDomesticStatus
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.Source
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.sync.SyncPrisonerDomesticStatusService
import java.time.LocalDateTime

class PrisonerDomesticStatusSyncFacadeTest {

  private val syncDomesticStatusService = mock<SyncPrisonerDomesticStatusService>()
  private val outboundEventsService = mock<OutboundEventsService>()
  private lateinit var facade: PrisonerDomesticStatusSyncFacade

  @BeforeEach
  fun setUp() {
    facade = PrisonerDomesticStatusSyncFacade(syncDomesticStatusService, outboundEventsService)
  }

  @Nested
  inner class GetDomesticStatusByPrisonerNumber {
    @Test
    fun `should return domestic status when prisoner number exists`() {
      val prisonerNumber = "A1234BC"
      val expectedStatus = SyncPrisonerDomesticStatusResponse(
        id = 1L,
        prisonerNumber = prisonerNumber,
        domesticStatusCode = "ABC",
        createdBy = "User",
        active = true,
      )

      whenever(syncDomesticStatusService.getDomesticStatusByPrisonerNumber(prisonerNumber))
        .thenReturn(expectedStatus)

      val result = facade.getDomesticStatusByPrisonerNumber(prisonerNumber)

      verify(syncDomesticStatusService).getDomesticStatusByPrisonerNumber(prisonerNumber)
      assert(result == expectedStatus)
    }
  }

  @Nested
  inner class UpdateDomesticStatus {
    @Test
    fun `should update domestic status and send event`() {
      val prisonerNumber = "A1234BC"
      val request = SyncUpdatePrisonerDomesticStatusRequest(
        domesticStatusCode = "ABC",
        createdBy = "User",
        createdTime = LocalDateTime.now(),
      )
      val updatedStatus = SyncPrisonerDomesticStatusResponse(
        id = 1L,
        prisonerNumber = prisonerNumber,
        domesticStatusCode = "ABC",
        createdBy = "User",
        active = true,
      )

      whenever(syncDomesticStatusService.createOrUpdateDomesticStatus(prisonerNumber, request))
        .thenReturn(updatedStatus)

      val result = facade.updateDomesticStatus(prisonerNumber, request)

      verify(syncDomesticStatusService).createOrUpdateDomesticStatus(prisonerNumber, request)
      verify(outboundEventsService).send(
        outboundEvent = OutboundEvent.PRISONER_DOMESTIC_STATUS_CREATED,
        identifier = updatedStatus.id,
        additionalInformation = PrisonerDomesticStatus(
          domesticStatusId = updatedStatus.id,
          domesticStatusCode = updatedStatus.domesticStatusCode,
          source = Source.NOMIS,
        ),
      )
      assert(result == updatedStatus)
    }
  }

  @Nested
  inner class DeleteDomesticStatus {
    @Test
    fun `should delete domestic status and send event`() {
      val prisonerNumber = "A1234BC"
      val deletedStatus = SyncPrisonerDomesticStatusResponse(
        id = 1L,
        prisonerNumber = prisonerNumber,
        domesticStatusCode = "ABC",
        createdBy = "User",
        active = true,
      )

      whenever(syncDomesticStatusService.deactivateDomesticStatus(prisonerNumber))
        .thenReturn(deletedStatus)

      facade.deleteDomesticStatus(prisonerNumber)

      verify(syncDomesticStatusService).deactivateDomesticStatus(prisonerNumber)
      verify(outboundEventsService).send(
        outboundEvent = OutboundEvent.PRISONER_DOMESTIC_STATUS_DELETED,
        identifier = deletedStatus.id,
        additionalInformation = PrisonerDomesticStatus(
          domesticStatusId = deletedStatus.id,
          domesticStatusCode = deletedStatus.domesticStatusCode,
          source = Source.NOMIS,
        ),
      )
    }
  }
}
