package uk.gov.justice.digital.hmpps.personalrelationships.model.telemetry.prisoner

import uk.gov.justice.digital.hmpps.personalrelationships.config.User
import uk.gov.justice.digital.hmpps.personalrelationships.service.telemetry.StandardTelemetryEvent
import uk.gov.justice.digital.hmpps.personalrelationships.service.telemetry.TelemetryCustomEventType

abstract class PrisonerCustomTelemetryEvent(open val prisonerNumber: String, val telemetryCustomEventType: TelemetryCustomEventType, val source: String, val user: User) : StandardTelemetryEvent(telemetryCustomEventType.eventName) {
  override fun properties(): Map<String, String> = mapOf(
    "description" to telemetryCustomEventType.description,
    "source" to source,
    "username" to user.username,
    "active_caseload_id" to user.activeCaseLoadId.toString(),
    "prisoner_number" to prisonerNumber,
  ) + customProperties()

  abstract fun customProperties(): Map<String, String>
}
