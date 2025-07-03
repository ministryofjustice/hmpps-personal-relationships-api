package uk.gov.justice.digital.hmpps.hmppscontactsapi.facade.sync

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppscontactsapi.config.User
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.OutboundEventsService
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.Source
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.sync.PrisonerRestrictionsMergeService

@Service
class PrisonerRestrictionsMergeFacade(
  private val prisonerMergeService: PrisonerRestrictionsMergeService,
  private val outboundEventsService: OutboundEventsService,
) {

  fun merge(keepingPrisonerNumber: String, removingPrisonerNumber: String) {
    prisonerMergeService.mergePrisonerRestrictions(keepingPrisonerNumber, removingPrisonerNumber)
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
}
