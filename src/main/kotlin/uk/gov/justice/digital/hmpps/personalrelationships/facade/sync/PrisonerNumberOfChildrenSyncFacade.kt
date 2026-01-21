package uk.gov.justice.digital.hmpps.personalrelationships.facade.sync

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.personalrelationships.config.User
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.sync.SyncUpdatePrisonerNumberOfChildrenRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.sync.Status
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.sync.SyncPrisonerNumberOfChildrenData
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.sync.SyncPrisonerNumberOfChildrenResponse
import uk.gov.justice.digital.hmpps.personalrelationships.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.personalrelationships.service.events.OutboundEventsService
import uk.gov.justice.digital.hmpps.personalrelationships.service.events.Source
import uk.gov.justice.digital.hmpps.personalrelationships.service.sync.SyncPrisonerNumberOfChildrenService

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
    .also { response -> hadleEvents(response, prisonerNumber) }
    .data

  private fun hadleEvents(
    response: SyncPrisonerNumberOfChildrenData,
    prisonerNumber: String,
  ) {
    when (response.status) {
      Status.UPDATED -> handleUpdatedStatus(response, prisonerNumber)
      Status.CREATED -> sendCreatedEvent(response.data.id, prisonerNumber)
      else -> {} // No events for unchanged status
    }
  }

  private fun handleUpdatedStatus(
    response: SyncPrisonerNumberOfChildrenData,
    prisonerNumber: String,
  ) {
    response.updatedId?.let { updatedId ->
      sendOutboundEvent(
        OutboundEvent.PRISONER_NUMBER_OF_CHILDREN_UPDATED,
        updatedId,
        prisonerNumber,
      )
    }
    sendCreatedEvent(response.data.id, prisonerNumber)
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
      user = User.SYS_USER,
    )
  }
}
