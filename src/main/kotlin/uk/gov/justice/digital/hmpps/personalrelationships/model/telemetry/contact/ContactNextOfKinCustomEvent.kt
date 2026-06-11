package uk.gov.justice.digital.hmpps.personalrelationships.model.telemetry.contact

import uk.gov.justice.digital.hmpps.personalrelationships.config.User
import uk.gov.justice.digital.hmpps.personalrelationships.service.events.Source
import uk.gov.justice.digital.hmpps.personalrelationships.service.telemetry.EventActionType
import uk.gov.justice.digital.hmpps.personalrelationships.service.telemetry.EventActionType.CREATE
import uk.gov.justice.digital.hmpps.personalrelationships.service.telemetry.EventActionType.DELETE
import uk.gov.justice.digital.hmpps.personalrelationships.service.telemetry.EventActionType.UPDATE
import uk.gov.justice.digital.hmpps.personalrelationships.service.telemetry.TelemetryCustomEventType

class ContactNextOfKinCustomEvent private constructor(
  override val contactId: Long,
  val linkedPrisonersCount: Long,
  private val contactNextOfKinCustomProperties: ContactNextOfKinCustomProperties,
  val eventActionType: EventActionType,
  val eventSource: Source,
  val eventUser: User,
) : ContactCustomTelemetryEvent(contactId, linkedPrisonersCount, getEvent(eventActionType), eventSource.name, eventUser) {
  override fun customProperties(): Map<String, String> = mapOf(
    "prisoner_contact_id" to contactNextOfKinCustomProperties.prisonerContactId.toString(),
    "prisoner_number" to contactNextOfKinCustomProperties.prisonerNumber,
  )

  companion object {
    fun getEvent(eventActionType: EventActionType): TelemetryCustomEventType = when (eventActionType) {
      CREATE -> TelemetryCustomEventType.CONTACT_NEXT_OF_KIN_CREATED_EVENT
      UPDATE -> throw UnsupportedOperationException("Update not supported for next of kin")
      DELETE -> TelemetryCustomEventType.CONTACT_NEXT_OF_KIN_DELETED_EVENT
    }
  }

  constructor(
    contactId: Long,
    linkedPrisonersCount: Long,
    prisonerContactId: Long,
    prisonerNumber: String,
    eventActionType: EventActionType,
    eventSource: Source,
    eventUser: User,
  ) : this(
    contactId = contactId,
    linkedPrisonersCount = linkedPrisonersCount,
    contactNextOfKinCustomProperties = ContactNextOfKinCustomProperties(prisonerContactId, prisonerNumber),
    eventActionType = eventActionType,
    eventSource = eventSource,
    eventUser = eventUser,
  )
}

internal class ContactNextOfKinCustomProperties(
  val prisonerContactId: Long,
  val prisonerNumber: String,
)
