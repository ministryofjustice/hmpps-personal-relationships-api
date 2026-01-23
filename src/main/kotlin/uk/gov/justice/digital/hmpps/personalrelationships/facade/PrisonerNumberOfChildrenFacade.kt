package uk.gov.justice.digital.hmpps.personalrelationships.facade

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.personalrelationships.config.User
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.CreateOrUpdatePrisonerNumberOfChildrenRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.PrisonerNumberOfChildrenResponse
import uk.gov.justice.digital.hmpps.personalrelationships.service.PrisonerNumberOfChildrenService
import uk.gov.justice.digital.hmpps.personalrelationships.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.personalrelationships.service.events.OutboundEventsService
import uk.gov.justice.digital.hmpps.personalrelationships.service.events.Source

@Service
class PrisonerNumberOfChildrenFacade(
  private val prisonerNumberOfChildrenService: PrisonerNumberOfChildrenService,
  private val outboundEventsService: OutboundEventsService,
) {
  fun getNumberOfChildren(prisonerNumber: String): PrisonerNumberOfChildrenResponse = prisonerNumberOfChildrenService.getNumberOfChildren(prisonerNumber)

  fun createOrUpdateNumberOfChildren(
    prisonerNumber: String,
    request: CreateOrUpdatePrisonerNumberOfChildrenRequest,
    user: User,
  ): PrisonerNumberOfChildrenResponse = prisonerNumberOfChildrenService.createOrUpdateNumberOfChildren(prisonerNumber, request, user)
    .also {
      outboundEventsService.send(
        outboundEvent = OutboundEvent.PRISONER_NUMBER_OF_CHILDREN_CREATED,
        identifier = it.id,
        noms = prisonerNumber,
        source = Source.DPS,
        user = user,
      )
    }
}
