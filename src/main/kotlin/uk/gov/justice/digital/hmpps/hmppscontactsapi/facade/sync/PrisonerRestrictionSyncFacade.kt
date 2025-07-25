package uk.gov.justice.digital.hmpps.hmppscontactsapi.facade.sync

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppscontactsapi.config.User
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync.SyncCreatePrisonerRestrictionRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync.SyncUpdatePrisonerRestrictionRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.OutboundEventsService
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.Source
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.sync.SyncPrisonerRestrictionsService
import uk.gov.justice.digital.hmpps.hmppscontactsapi.util.UserUtil

@Component
class PrisonerRestrictionSyncFacade(
  private val prisonerRestrictionService: SyncPrisonerRestrictionsService,
  private val outboundEventsService: OutboundEventsService,
  private val userUtil: UserUtil,
) {
  fun getPrisonerRestrictionById(prisonerRestrictionId: Long) = prisonerRestrictionService.getPrisonerRestrictionById(prisonerRestrictionId)

  fun deletePrisonerRestriction(prisonerRestrictionId: Long) {
    prisonerRestrictionService.deletePrisonerRestriction(prisonerRestrictionId)
      .also {
        outboundEventsService.send(
          outboundEvent = OutboundEvent.PRISONER_RESTRICTION_DELETED,
          identifier = prisonerRestrictionId,
          noms = it.prisonerNumber,
          source = Source.NOMIS,
          user = User.SYS_USER,
        )
      }
  }

  fun createPrisonerRestriction(request: SyncCreatePrisonerRestrictionRequest) = prisonerRestrictionService.createPrisonerRestriction(request).also {
    outboundEventsService.send(
      outboundEvent = OutboundEvent.PRISONER_RESTRICTION_CREATED,
      identifier = it.prisonerRestrictionId,
      noms = it.prisonerNumber,
      source = Source.NOMIS,
      user = userUtil.userOrDefault(request.createdBy),
    )
  }

  fun updatePrisonerRestriction(
    prisonerRestrictionId: Long,
    request: SyncUpdatePrisonerRestrictionRequest,
  ) = prisonerRestrictionService.updatePrisonerRestriction(prisonerRestrictionId, request).also {
    outboundEventsService.send(
      outboundEvent = OutboundEvent.PRISONER_RESTRICTION_UPDATED,
      identifier = it.prisonerRestrictionId,
      noms = it.prisonerNumber,
      source = Source.NOMIS,
      user = userUtil.userOrDefault(request.updatedBy),
    )
  }
}
