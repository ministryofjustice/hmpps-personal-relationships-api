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

  fun merge(keepingPrisonerNumber: String, removedPrisonerNo: String) {
    prisonerMergeService.mergePrisonerRestrictions(keepingPrisonerNumber, removedPrisonerNo)
      .also {
        if (it.wasCreated) {
          val changedRestrictionIds = it.keepingPrisonerRestrictionIds + it.removingPrisonerRestrictionIds
          if (changedRestrictionIds.isNotEmpty()) {
            outboundEventsService.sendPrisonerRestrictionsChanged(
              updatedRestrictionIds = it.keepingPrisonerRestrictionIds,
              removedRestrictionIds = it.removingPrisonerRestrictionIds,
              noms = keepingPrisonerNumber,
              source = Source.NOMIS,
              user = User.SYS_USER,
            )
          }
        }
      }
  }
}
