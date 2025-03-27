package uk.gov.justice.digital.hmpps.hmppscontactsapi.facade.sync

import org.springframework.stereotype.Service
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
    prisonerMergeService.mergeNumberOfChildren(keepingPrisonerNumber, removedPrisonerNo)
      .also {
        if (it.wasCreated) {
          outboundEventsService.send(
            outboundEvent = OutboundEvent.PRISONER_NUMBER_OF_CHILDREN_CREATED,
            identifier = it.id,
            noms = keepingPrisonerNumber,
            source = Source.DPS,
          )
        }
      }

    prisonerMergeService.mergeDomesticStatus(keepingPrisonerNumber, removedPrisonerNo)
      .also {
        if (it.wasCreated) {
          outboundEventsService.send(
            outboundEvent = OutboundEvent.PRISONER_DOMESTIC_STATUS_CREATED,
            identifier = it.id,
            noms = keepingPrisonerNumber,
            source = Source.DPS,
          )
        }
      }
  }
}
