package uk.gov.justice.digital.hmpps.hmppscontactsapi.facade

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.UpdatePrisonerDomesticStatusRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.PrisonerDomesticStatusResponse
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.PrisonerDomesticStatusService
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.OutboundEventsService

@Service
class PrisonerDomesticStatusFacade(
  private val prisonerDomesticStatusService: PrisonerDomesticStatusService,
  private val outboundEventsService: OutboundEventsService,
) {
  fun getDomesticStatus(prisonerNumber: String): PrisonerDomesticStatusResponse = prisonerDomesticStatusService.getDomesticStatus(prisonerNumber)

  /**
   *  From the prisoner's profile this will always be
   *  created if no existing value or updated if there is an existing value,
   *  in order to maintain history
   *  we always create new record
   *  and update the existing one as inactive if there is existing record
   *  Fire PRISONER_DOMESTIC_STATUS_CREATED with the DomesticStatusId, personReference
   *  Any listener of this domain event would do the following
   *  call your API to get the latest value
   *  update NOMIS in this case
   */
  fun createOrUpdateDomesticStatus(
    prisonerNumber: String,
    request: UpdatePrisonerDomesticStatusRequest,
  ): PrisonerDomesticStatusResponse {
    val response = prisonerDomesticStatusService.createOrUpdateDomesticStatus(prisonerNumber, request)

    outboundEventsService.send(
      outboundEvent = OutboundEvent.PRISONER_DOMESTIC_STATUS_CREATED,
      identifier = response.id,
      noms = response.prisonerNumber,
    )

    return response
  }
}
