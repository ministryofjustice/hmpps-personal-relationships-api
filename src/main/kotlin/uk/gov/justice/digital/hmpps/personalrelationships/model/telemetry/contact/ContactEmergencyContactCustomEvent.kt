package uk.gov.justice.digital.hmpps.personalrelationships.model.telemetry.contact

import uk.gov.justice.digital.hmpps.personalrelationships.config.User
import uk.gov.justice.digital.hmpps.personalrelationships.service.events.Source
import uk.gov.justice.digital.hmpps.personalrelationships.service.telemetry.EventActionType
import uk.gov.justice.digital.hmpps.personalrelationships.service.telemetry.EventActionType.CREATE
import uk.gov.justice.digital.hmpps.personalrelationships.service.telemetry.EventActionType.DELETE
import uk.gov.justice.digital.hmpps.personalrelationships.service.telemetry.EventActionType.UPDATE
import uk.gov.justice.digital.hmpps.personalrelationships.service.telemetry.TelemetryCustomEventType

class ContactEmergencyContactCustomEvent private constructor(
  override val contactId: Long,
  private val contactEmergencyContactCustomProperties: ContactEmergencyContactCustomProperties,
  val eventActionType: EventActionType,
  val eventSource: Source,
  val eventUser: User,
) : ContactCustomTelemetryEvent(contactId, getEvent(eventActionType), eventSource.name, eventUser) {
  override fun customProperties(): Map<String, String> = mapOf("prisoner_contact_id" to contactEmergencyContactCustomProperties.prisonerContactId.toString())

  companion object {
    fun getEvent(eventActionType: EventActionType): TelemetryCustomEventType = when (eventActionType) {
      CREATE -> TelemetryCustomEventType.CONTACT_EMERGENCY_CONTACT_CREATED_EVENT
      UPDATE -> throw UnsupportedOperationException("Update not supported for emergency contact")
      DELETE -> TelemetryCustomEventType.CONTACT_EMERGENCY_CONTACT_DELETED_EVENT
    }
  }

  constructor(
    contactId: Long,
    prisonerContactId: Long,
    eventActionType: EventActionType,
    eventSource: Source,
    eventUser: User,
  ) : this(contactId, ContactEmergencyContactCustomProperties(prisonerContactId), eventActionType, eventSource, eventUser)
}

internal class ContactEmergencyContactCustomProperties(
  val prisonerContactId: Long,
)
