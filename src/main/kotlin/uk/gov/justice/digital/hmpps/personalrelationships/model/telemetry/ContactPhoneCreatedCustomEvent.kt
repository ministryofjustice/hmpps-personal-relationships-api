package uk.gov.justice.digital.hmpps.personalrelationships.model.telemetry

import uk.gov.justice.digital.hmpps.personalrelationships.config.User
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.ContactPhoneDetails
import uk.gov.justice.digital.hmpps.personalrelationships.service.events.Source
import uk.gov.justice.digital.hmpps.personalrelationships.service.telemetry.CustomTelemetryEvent
import uk.gov.justice.digital.hmpps.personalrelationships.service.telemetry.TelemetryCustomEventType.CONTACT_PHONE_CREATED_EVENT

class ContactPhoneCreatedCustomEvent(
  val contactPhoneDetails: ContactPhoneDetails,
  val eventSource: Source,
  val eventUser: User,
  ): CustomTelemetryEvent(telemetryCustomEventType = CONTACT_PHONE_CREATED_EVENT, source = eventSource.name, user = eventUser) {
  override fun properties(): Map<String, String> {
    val properties = emptyMap<String, String>()
    return (properties + additionalProperties()).toMap()
  }
}
