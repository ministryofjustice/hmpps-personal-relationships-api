package uk.gov.justice.digital.hmpps.hmppscontactsapi.facade.sync

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync.SyncUpdatePrisonerDomesticStatusRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.sync.Status
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.sync.SyncPrisonerDomesticStatusResponse
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.sync.SyncPrisonerDomesticStatusResponseData
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.OutboundEventsService
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.Source
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.sync.SyncPrisonerDomesticStatusService

@Service
class PrisonerDomesticStatusSyncFacade(
  private val syncDomesticStatusService: SyncPrisonerDomesticStatusService,
  private val outboundEventsService: OutboundEventsService,
) {
  fun getDomesticStatusByPrisonerNumber(prisonerNumber: String): SyncPrisonerDomesticStatusResponse = syncDomesticStatusService.getDomesticStatusByPrisonerNumber(prisonerNumber)

  fun createOrUpdateDomesticStatus(
    prisonerNumber: String,
    request: SyncUpdatePrisonerDomesticStatusRequest,
  ): SyncPrisonerDomesticStatusResponse = syncDomesticStatusService
    .createOrUpdateDomesticStatus(prisonerNumber, request)
    .also { response -> handleStatusEvents(response, prisonerNumber) }
    .data

  private fun handleStatusEvents(
    statusResponse: SyncPrisonerDomesticStatusResponseData,
    prisonerNumber: String,
  ) {
    when (statusResponse.status) {
      Status.UPDATED -> handleUpdatedStatus(statusResponse, prisonerNumber)
      Status.CREATED -> sendCreatedEvent(statusResponse.data.id, prisonerNumber)
      else -> {} // No events for unchanged status
    }
  }

  private fun handleUpdatedStatus(
    statusResponse: SyncPrisonerDomesticStatusResponseData,
    prisonerNumber: String,
  ) {
    statusResponse.updatedId?.let { updatedId ->
      sendOutboundEvent(
        OutboundEvent.PRISONER_DOMESTIC_STATUS_UPDATED,
        updatedId,
        prisonerNumber,
      )
    }
    sendCreatedEvent(statusResponse.data.id, prisonerNumber)
  }

  private fun sendCreatedEvent(id: Long, prisonerNumber: String) {
    sendOutboundEvent(
      OutboundEvent.PRISONER_DOMESTIC_STATUS_CREATED,
      id,
      prisonerNumber,
    )
  }

  private fun sendOutboundEvent(
    outboundEvent: OutboundEvent,
    identifier: Long,
    prisonerNumber: String,
  ) {
    outboundEventsService.send(
      outboundEvent = outboundEvent,
      identifier = identifier,
      noms = prisonerNumber,
      source = Source.NOMIS,
    )
  }
}
