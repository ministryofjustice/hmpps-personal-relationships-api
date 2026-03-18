package uk.gov.justice.digital.hmpps.personalrelationships.model.telemetry.contact

import uk.gov.justice.digital.hmpps.personalrelationships.config.User
import uk.gov.justice.digital.hmpps.personalrelationships.service.telemetry.StandardTelemetryEvent
import uk.gov.justice.digital.hmpps.personalrelationships.service.telemetry.TelemetryCustomEventType

abstract class ContactCustomTelemetryEvent(open val contactId: Long, val telemetryCustomEventType: TelemetryCustomEventType, val source: String, val user: User) : StandardTelemetryEvent(telemetryCustomEventType.eventName) {
  override fun properties(): Map<String, String> = mapOf(
    "description" to telemetryCustomEventType.description,
    "source" to source,
    "username" to user.username,
    "active_caseload_id" to user.activeCaseLoadId.toString(),
    "contactId" to contactId.toString(),
  ) + customProperties()

  abstract fun customProperties(): Map<String, String>
}
