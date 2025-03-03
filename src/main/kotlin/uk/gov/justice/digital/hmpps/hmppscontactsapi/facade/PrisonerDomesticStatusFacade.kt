package uk.gov.justice.digital.hmpps.hmppscontactsapi.facade

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.CreateOrUpdatePrisonerDomesticStatusRequest
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

  fun createOrUpdateDomesticStatus(
    prisonerNumber: String,
    request: CreateOrUpdatePrisonerDomesticStatusRequest,
  ): PrisonerDomesticStatusResponse {
    val response = prisonerDomesticStatusService.createOrUpdateDomesticStatus(prisonerNumber, request)

    outboundEventsService.send(
      outboundEvent = OutboundEvent.PRISONER_DOMESTIC_STATUS_CREATED,
      identifier = response.id,
      noms = prisonerNumber,
    )

    return response
  }
}
