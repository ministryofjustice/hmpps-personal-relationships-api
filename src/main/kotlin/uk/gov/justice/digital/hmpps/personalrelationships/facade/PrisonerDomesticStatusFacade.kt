package uk.gov.justice.digital.hmpps.personalrelationships.facade

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.personalrelationships.config.User
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.CreateOrUpdatePrisonerDomesticStatusRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.PrisonerDomesticStatusResponse
import uk.gov.justice.digital.hmpps.personalrelationships.service.PrisonerDomesticStatusService
import uk.gov.justice.digital.hmpps.personalrelationships.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.personalrelationships.service.events.OutboundEventsService
import uk.gov.justice.digital.hmpps.personalrelationships.service.events.Source

@Service
class PrisonerDomesticStatusFacade(
  private val prisonerDomesticStatusService: PrisonerDomesticStatusService,
  private val outboundEventsService: OutboundEventsService,
) {
  fun getDomesticStatus(prisonerNumber: String): PrisonerDomesticStatusResponse = prisonerDomesticStatusService.getDomesticStatus(prisonerNumber)

  fun createOrUpdateDomesticStatus(
    prisonerNumber: String,
    request: CreateOrUpdatePrisonerDomesticStatusRequest,
    user: User,
  ): PrisonerDomesticStatusResponse = prisonerDomesticStatusService.createOrUpdateDomesticStatus(prisonerNumber, request, user)
    .also {
      outboundEventsService.send(
        outboundEvent = OutboundEvent.PRISONER_DOMESTIC_STATUS_CREATED,
        identifier = it.id,
        noms = prisonerNumber,
        source = Source.DPS,
        user = user,
      )
    }
}
