package uk.gov.justice.digital.hmpps.personalrelationships.model.telemetry

import uk.gov.justice.digital.hmpps.personalrelationships.config.User
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.ContactDetails
import uk.gov.justice.digital.hmpps.personalrelationships.service.events.Source
import uk.gov.justice.digital.hmpps.personalrelationships.service.telemetry.CustomTelemetryEvent
import uk.gov.justice.digital.hmpps.personalrelationships.service.telemetry.TelemetryCustomEventType.CONTACT_CREATED_EVENT

data class ContactCreatedCustomEvent(
  val contact: ContactDetails,
  val eventSource: Source,
  val eventUser: User,
) : CustomTelemetryEvent(telemetryCustomEventType = CONTACT_CREATED_EVENT, source = eventSource.name, user = eventUser) {
  override fun properties(): Map<String, String> {
    val properties = mutableMapOf(
      "contact_id" to contact.id.toString(),
    )

    return (properties + additionalProperties()).toMap()
  }
}
