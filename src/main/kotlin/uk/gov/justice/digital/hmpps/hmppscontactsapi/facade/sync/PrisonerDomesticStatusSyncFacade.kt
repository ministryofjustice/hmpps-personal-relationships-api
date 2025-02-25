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

  fun createOrUpdateDomesticStatus(
    prisonerNumber: String,
    request: SyncUpdatePrisonerDomesticStatusRequest,
  ): SyncPrisonerDomesticStatusResponse {
    val existingRecord = syncDomesticStatusService.getPrisonerDomesticStatusActive(prisonerNumber)

    return syncDomesticStatusService.createOrUpdateDomesticStatus(prisonerNumber, request)
      .also {
        existingRecord?.prisonerDomesticStatusId?.let { identifier ->
          outboundEventsService.send(
            outboundEvent = OutboundEvent.PRISONER_DOMESTIC_STATUS_UPDATED,
            identifier = identifier,
            noms = prisonerNumber,
            source = Source.NOMIS,
          )
        }

        outboundEventsService.send(
          outboundEvent = OutboundEvent.PRISONER_DOMESTIC_STATUS_CREATED,
          identifier = it.id,
          noms = prisonerNumber,
          source = Source.NOMIS,
        )
      }
  }
}
