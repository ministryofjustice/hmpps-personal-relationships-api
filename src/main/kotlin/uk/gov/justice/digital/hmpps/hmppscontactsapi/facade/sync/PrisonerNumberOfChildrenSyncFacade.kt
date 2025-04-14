package uk.gov.justice.digital.hmpps.hmppscontactsapi.facade.sync

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync.SyncUpdatePrisonerNumberOfChildrenRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.sync.Status
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.sync.SyncPrisonerNumberOfChildrenData
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
  ): SyncPrisonerNumberOfChildrenResponse = syncNumberOfChildrenService.createOrUpdateNumberOfChildren(prisonerNumber, request)
    .also { response -> handleStatusEvents(response, prisonerNumber) }.data

  private fun handleStatusEvents(
    statusResponse: SyncPrisonerNumberOfChildrenData,
    prisonerNumber: String,
  ) {
    when (statusResponse.status) {
      Status.UPDATED -> handleUpdatedStatus(statusResponse, prisonerNumber)
      Status.CREATED -> sendCreatedEvent(statusResponse.data.id, prisonerNumber)
      else -> {} // No events for unchanged status
    }
  }

  private fun handleUpdatedStatus(
    statusResponse: SyncPrisonerNumberOfChildrenData,
    prisonerNumber: String,
  ) {
    statusResponse.updatedId?.let { updatedId ->
      sendOutboundEvent(
        OutboundEvent.PRISONER_NUMBER_OF_CHILDREN_UPDATED,
        updatedId,
        prisonerNumber,
      )
    }
    sendCreatedEvent(statusResponse.data.id, prisonerNumber)
  }

  private fun sendCreatedEvent(id: Long, prisonerNumber: String) {
    sendOutboundEvent(
      OutboundEvent.PRISONER_NUMBER_OF_CHILDREN_CREATED,
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
