package uk.gov.justice.digital.hmpps.hmppscontactsapi.facade.sync

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync.SyncUpdatePrisonerNumberOfChildrenRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.sync.SyncPrisonerNumberOfChildrenResponse
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.OutboundEventsService
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.Source
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.sync.SyncPrisonerNumberOfChildrenService

@Service
class PrisonerNumberOfChildrenSyncFacade(
  private val syncNumberOfChildrenService: SyncPrisonerNumberOfChildrenService,
  private val outboundEventsService: OutboundEventsService,
) {
  fun getNumberOfChildrenByPrisonerNumber(prisonerNumber: String): SyncPrisonerNumberOfChildrenResponse = syncNumberOfChildrenService.getNumberOfChildrenByPrisonerNumber(prisonerNumber)

  fun createOrUpdateNumberOfChildren(
    prisonerNumber: String,
    request: SyncUpdatePrisonerNumberOfChildrenRequest,
  ): SyncPrisonerNumberOfChildrenResponse {
    val existingRecord = syncNumberOfChildrenService.getPrisonerNumberOfChildrenActive(prisonerNumber)

    return syncNumberOfChildrenService.createOrUpdateNumberOfChildren(prisonerNumber, request)
      .also {
        existingRecord?.prisonerNumberOfChildrenId?.let { identifier ->
          outboundEventsService.send(
            outboundEvent = OutboundEvent.PRISONER_NUMBER_OF_CHILDREN_UPDATED,
            identifier = identifier,
            noms = prisonerNumber,
            source = Source.NOMIS,
          )
        }

        outboundEventsService.send(
          outboundEvent = OutboundEvent.PRISONER_NUMBER_OF_CHILDREN_CREATED,
          identifier = it.id,
          noms = prisonerNumber,
          source = Source.NOMIS,
        )
      }
  }
}
