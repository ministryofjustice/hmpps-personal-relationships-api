package uk.gov.justice.digital.hmpps.personalrelationships.facade

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.personalrelationships.config.User
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.restrictions.ClosePrisonerRestrictionRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.restrictions.CreatePrisonerRestrictionRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.restrictions.UpdatePrisonerRestrictionRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.PrisonerRestrictionDetails
import uk.gov.justice.digital.hmpps.personalrelationships.service.ManagePrisonerRestrictionsService
import uk.gov.justice.digital.hmpps.personalrelationships.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.personalrelationships.service.events.OutboundEventsService
import uk.gov.justice.digital.hmpps.personalrelationships.service.events.Source
import uk.gov.justice.digital.hmpps.personalrelationships.service.telemetry.TelemetryPrisonerCustomEventService
import java.time.LocalDate

@Service
class ManagePrisonerRestrictionsFacade(
  private val managePrisonerRestrictionsService: ManagePrisonerRestrictionsService,
  private val outboundEventsService: OutboundEventsService,
  private val telemetryPrisonerCustomEventService: TelemetryPrisonerCustomEventService,
) {
  fun createPrisonerRestriction(
    prisonerNumber: String,
    request: CreatePrisonerRestrictionRequest,
    user: User,
  ): PrisonerRestrictionDetails = managePrisonerRestrictionsService.createPrisonerRestriction(prisonerNumber, request, user).also {
    outboundEventsService.send(
      outboundEvent = OutboundEvent.PRISONER_RESTRICTION_CREATED,
      identifier = it.prisonerRestrictionId,
      noms = prisonerNumber,
      source = Source.DPS,
      user = user,
    )
    telemetryPrisonerCustomEventService.trackCreatePrisonerRestrictionEvent(prisonerNumber, it, Source.DPS, user)
  }

  fun updatePrisonerRestriction(
    prisonerNumber: String,
    prisonerRestrictionId: Long,
    request: UpdatePrisonerRestrictionRequest,
    user: User,
  ): PrisonerRestrictionDetails = managePrisonerRestrictionsService.updatePrisonerRestriction(prisonerNumber, prisonerRestrictionId, request, user).also {
    outboundEventsService.send(
      outboundEvent = OutboundEvent.PRISONER_RESTRICTION_UPDATED,
      identifier = prisonerRestrictionId,
      noms = prisonerNumber,
      source = Source.DPS,
      user = user,
    )
  }.also {
    telemetryPrisonerCustomEventService.trackUpdatePrisonerRestrictionEvent(prisonerNumber, it, Source.DPS, user)
  }

  // Closing sets the expiry date, so it is published as an update for NOMIS to apply
  fun closePrisonerRestriction(
    prisonerNumber: String,
    prisonerRestrictionId: Long,
    request: ClosePrisonerRestrictionRequest,
    user: User,
  ): PrisonerRestrictionDetails = managePrisonerRestrictionsService.closePrisonerRestriction(prisonerNumber, prisonerRestrictionId, request, user).also {
    outboundEventsService.send(
      outboundEvent = OutboundEvent.PRISONER_RESTRICTION_UPDATED,
      identifier = prisonerRestrictionId,
      noms = prisonerNumber,
      source = Source.DPS,
      user = user,
    )
    telemetryPrisonerCustomEventService.trackClosePrisonerRestrictionEvent(prisonerNumber, it, request.expiryDate == LocalDate.now(), Source.DPS, user)
  }
}
