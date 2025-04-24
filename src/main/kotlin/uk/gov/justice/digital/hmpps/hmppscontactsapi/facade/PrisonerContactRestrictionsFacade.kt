package uk.gov.justice.digital.hmpps.hmppscontactsapi.facade

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppscontactsapi.config.User
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.restrictions.CreatePrisonerContactRestrictionRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.restrictions.UpdatePrisonerContactRestrictionRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.PrisonerContactRestrictionDetails
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.PrisonerContactRestrictionsResponse
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.RestrictionsService
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.OutboundEventsService

@Service
class PrisonerContactRestrictionsFacade(
  private val restrictionsService: RestrictionsService,
  private val outboundEventsService: OutboundEventsService,
) {

  fun getPrisonerContactRestrictions(prisonerContactId: Long): PrisonerContactRestrictionsResponse = restrictionsService.getPrisonerContactRestrictions(prisonerContactId)

  fun createPrisonerContactRestriction(
    prisonerContactId: Long,
    request: CreatePrisonerContactRestrictionRequest,
    user: User,
  ): PrisonerContactRestrictionDetails = restrictionsService.createPrisonerContactRestriction(prisonerContactId, request, user).also {
    outboundEventsService.send(
      outboundEvent = OutboundEvent.PRISONER_CONTACT_RESTRICTION_CREATED,
      identifier = it.prisonerContactRestrictionId,
      contactId = it.contactId,
      noms = it.prisonerNumber,
      user = user,
    )
  }

  fun updatePrisonerContactRestriction(
    prisonerContactId: Long,
    prisonerContactRestrictionId: Long,
    request: UpdatePrisonerContactRestrictionRequest,
    user: User,
  ): PrisonerContactRestrictionDetails = restrictionsService.updatePrisonerContactRestriction(prisonerContactId, prisonerContactRestrictionId, request, user).also {
    outboundEventsService.send(
      outboundEvent = OutboundEvent.PRISONER_CONTACT_RESTRICTION_UPDATED,
      identifier = prisonerContactRestrictionId,
      contactId = it.contactId,
      noms = it.prisonerNumber,
      user = user,
    )
  }
}
