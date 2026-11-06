package uk.gov.justice.digital.hmpps.personalrelationships.model.telemetry.prisoner

import uk.gov.justice.digital.hmpps.personalrelationships.config.User
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.PrisonerRestrictionDetails
import uk.gov.justice.digital.hmpps.personalrelationships.service.events.Source
import uk.gov.justice.digital.hmpps.personalrelationships.service.telemetry.TelemetryCustomEventType.PRISONER_RESTRICTION_CLOSED_EVENT

class PrisonerRestrictionClosedCustomEvent(
  prisonerNumber: String,
  private val prisonerRestrictionDetails: PrisonerRestrictionDetails,
  private val closedImmediately: Boolean,
  source: Source,
  user: User,
) : PrisonerCustomTelemetryEvent(prisonerNumber, PRISONER_RESTRICTION_CLOSED_EVENT, source.name, user) {
  override fun customProperties(): Map<String, String> = mapOf(
    "prisoner_restriction_id" to prisonerRestrictionDetails.prisonerRestrictionId.toString(),
    "restriction_code" to prisonerRestrictionDetails.restrictionType,
    "closed_immediately" to closedImmediately.toString(),
  )
}
