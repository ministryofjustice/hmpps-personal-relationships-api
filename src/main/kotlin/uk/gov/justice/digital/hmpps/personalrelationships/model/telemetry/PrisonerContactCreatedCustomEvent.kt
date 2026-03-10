package uk.gov.justice.digital.hmpps.personalrelationships.model.telemetry

import uk.gov.justice.digital.hmpps.personalrelationships.config.User
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.PrisonerContactRelationshipDetails
import uk.gov.justice.digital.hmpps.personalrelationships.service.events.Source
import uk.gov.justice.digital.hmpps.personalrelationships.service.telemetry.CustomTelemetryEvent
import uk.gov.justice.digital.hmpps.personalrelationships.service.telemetry.TelemetryCustomEventType.PRISONER_CONTACT_CREATED_EVENT

data class PrisonerContactCreatedCustomEvent(
  val prisonerContactRelationshipDetails: PrisonerContactRelationshipDetails,
  val eventSource: Source,
  val eventUser: User,
) : CustomTelemetryEvent(telemetryCustomEventType = PRISONER_CONTACT_CREATED_EVENT, source = eventSource.name, user = eventUser) {
  override fun properties(): Map<String, String> {
    val properties = mutableMapOf(
      "prisoner_number" to prisonerContactRelationshipDetails.prisonerNumber,
      "contact_id" to prisonerContactRelationshipDetails.contactId.toString(),
    )

    return (properties + additionalProperties()).toMap()
  }
}
