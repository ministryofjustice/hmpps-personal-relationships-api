package uk.gov.justice.digital.hmpps.hmppscontactsapi.facade

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.CreateOrUpdatePrisonerNumberOfChildrenRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.PrisonerNumberOfChildrenResponse
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.PrisonerNumberOfChildrenService
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.OutboundEventsService
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.Source

@Service
class PrisonerNumberOfChildrenFacade(
  private val prisonerNumberOfChildrenService: PrisonerNumberOfChildrenService,
  private val outboundEventsService: OutboundEventsService,
) {
  fun getNumberOfChildren(prisonerNumber: String): PrisonerNumberOfChildrenResponse = prisonerNumberOfChildrenService.getNumberOfChildren(prisonerNumber)

  fun createOrUpdateNumberOfChildren(
    prisonerNumber: String,
    request: CreateOrUpdatePrisonerNumberOfChildrenRequest,
  ): PrisonerNumberOfChildrenResponse = prisonerNumberOfChildrenService.createOrUpdateNumberOfChildren(prisonerNumber, request)
    .also {
      outboundEventsService.send(
        outboundEvent = OutboundEvent.PRISONER_NUMBER_OF_CHILDREN_CREATED,
        identifier = it.id,
        noms = prisonerNumber,
        source = Source.DPS,
      )
    }
}
