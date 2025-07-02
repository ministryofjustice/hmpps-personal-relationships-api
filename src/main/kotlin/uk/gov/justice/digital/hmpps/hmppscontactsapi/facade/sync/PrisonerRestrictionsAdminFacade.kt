package uk.gov.justice.digital.hmpps.hmppscontactsapi.facade.sync

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppscontactsapi.config.User
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.OutboundEventsService
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.Source
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.sync.PrisonerRestrictionsAdminService

@Service
class PrisonerRestrictionsAdminFacade(
  private val restrictionsAdminService: PrisonerRestrictionsAdminService,
  private val outboundEventsService: OutboundEventsService,
) {

  fun merge(keepingPrisonerNumber: String, removingPrisonerNumber: String) {
    restrictionsAdminService.mergePrisonerRestrictions(keepingPrisonerNumber, removingPrisonerNumber)
      .also {
        if (it.wasUpdated) {
          outboundEventsService.sendPrisonerRestrictionsChanged(
            keepingPrisonerNumber,
            removingPrisonerNumber,
            source = Source.NOMIS,
            user = User.SYS_USER,
          )
        }
      }
  }

  fun reset(prisonerNumber: String) {
    restrictionsAdminService.resetPrisonerRestrictions(prisonerNumber)
      .also {
        if (it.wasDeleted) {
          // Send PRISONER_RESTRICTION_DELETED event for each deleted restriction
          it.deletedRestrictions.forEach { restriction ->
            outboundEventsService.send(
              outboundEvent = OutboundEvent.PRISONER_RESTRICTION_DELETED,
              identifier = restriction.prisonerRestrictionId,
              noms = prisonerNumber,
              source = Source.NOMIS,
              user = User.SYS_USER,
            )
          }
        }
      }
  }
}
