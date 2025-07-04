package uk.gov.justice.digital.hmpps.hmppscontactsapi.facade.sync

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppscontactsapi.config.User
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync.ResetPrisonerRestrictionsRequest
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
        if (it.hasChanged) {
          outboundEventsService.sendPrisonerRestrictionsChanged(
            keepingPrisonerNumber,
            removingPrisonerNumber,
            source = Source.NOMIS,
            user = User.SYS_USER,
          )
        }
      }
  }

  fun reset(request: ResetPrisonerRestrictionsRequest) {
    restrictionsAdminService.resetPrisonerRestrictions(request)
      .also {
        if (it.hasChanged) {
          outboundEventsService.sendPrisonerRestrictionsChanged(
            request.prisonerNumber,
            null,
            source = Source.NOMIS,
            user = User.SYS_USER,
          )
        }
      }
  }
}
