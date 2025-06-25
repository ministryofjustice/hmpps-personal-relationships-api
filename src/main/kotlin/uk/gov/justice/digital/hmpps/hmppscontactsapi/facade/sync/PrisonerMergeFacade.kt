package uk.gov.justice.digital.hmpps.hmppscontactsapi.facade.sync

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppscontactsapi.config.User
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.OutboundEventsService
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.Source
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.sync.PrisonerMergeService

@Service
class PrisonerMergeFacade(
  private val prisonerMergeService: PrisonerMergeService,
  private val outboundEventsService: OutboundEventsService,
) {

  fun merge(
    keepingPrisonerNumber: String,
    removedPrisonerNo: String,
  ) {
    handlePrisonerNumberOfChildren(keepingPrisonerNumber, removedPrisonerNo)

    handlePrisonerDomesticStatus(keepingPrisonerNumber, removedPrisonerNo)

    handlePrisonerRestrictions(keepingPrisonerNumber, removedPrisonerNo)
  }

  private fun handlePrisonerNumberOfChildren(keepingPrisonerNumber: String, removedPrisonerNo: String) {
    prisonerMergeService.mergeNumberOfChildren(keepingPrisonerNumber, removedPrisonerNo)
      .also {
        if (it.wasCreated) {
          outboundEventsService.send(
            outboundEvent = OutboundEvent.PRISONER_NUMBER_OF_CHILDREN_CREATED,
            identifier = it.id,
            noms = keepingPrisonerNumber,
            source = Source.DPS,
            user = User.SYS_USER,
          )
        }
      }
  }

  private fun handlePrisonerDomesticStatus(keepingPrisonerNumber: String, removedPrisonerNo: String) {
    prisonerMergeService.mergeDomesticStatus(keepingPrisonerNumber, removedPrisonerNo)
      .also {
        if (it.wasCreated) {
          outboundEventsService.send(
            outboundEvent = OutboundEvent.PRISONER_DOMESTIC_STATUS_CREATED,
            identifier = it.id,
            noms = keepingPrisonerNumber,
            source = Source.DPS,
            user = User.SYS_USER,
          )
        }
      }
  }

  private fun handlePrisonerRestrictions(keepingPrisonerNumber: String, removedPrisonerNo: String) {
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
