package uk.gov.justice.digital.hmpps.hmppscontactsapi.facade.sync

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync.SyncUpdatePrisonerDomesticStatusRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.sync.SyncPrisonerDomesticStatusResponse
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

  fun updateDomesticStatus(prisonerNumber: String, request: SyncUpdatePrisonerDomesticStatusRequest): SyncPrisonerDomesticStatusResponse = syncDomesticStatusService.createOrUpdateDomesticStatus(prisonerNumber, request)
    .also {
      outboundEventsService.send(
        outboundEvent = OutboundEvent.PRISONER_DOMESTIC_STATUS_UPDATED,
        identifier = it.id,
        noms = it.prisonerNumber,
        source = Source.NOMIS,
      )
    }

  fun deleteDomesticStatus(prisonerNumber: String) = syncDomesticStatusService.deactivateDomesticStatus(prisonerNumber)
    .also {
      outboundEventsService.send(
        outboundEvent = OutboundEvent.PRISONER_DOMESTIC_STATUS_DELETED,
        identifier = it.id,
        noms = it.prisonerNumber,
        source = Source.NOMIS,
      )
    }
}
