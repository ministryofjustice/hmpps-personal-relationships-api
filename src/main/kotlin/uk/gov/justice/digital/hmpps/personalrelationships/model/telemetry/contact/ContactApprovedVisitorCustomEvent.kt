package uk.gov.justice.digital.hmpps.personalrelationships.model.telemetry.contact

import uk.gov.justice.digital.hmpps.personalrelationships.config.User
import uk.gov.justice.digital.hmpps.personalrelationships.service.events.Source
import uk.gov.justice.digital.hmpps.personalrelationships.service.telemetry.EventActionType
import uk.gov.justice.digital.hmpps.personalrelationships.service.telemetry.EventActionType.CREATE
import uk.gov.justice.digital.hmpps.personalrelationships.service.telemetry.EventActionType.DELETE
import uk.gov.justice.digital.hmpps.personalrelationships.service.telemetry.EventActionType.UPDATE
import uk.gov.justice.digital.hmpps.personalrelationships.service.telemetry.TelemetryCustomEventType

class ContactApprovedVisitorCustomEvent private constructor(
  override val contactId: Long,
  val linkedPrisonersCount: Long,
  private val contactApprovedVisitorCustomProperties: ContactApprovedVisitorCustomProperties,
  val eventActionType: EventActionType,
  val eventSource: Source,
  val eventUser: User,
) : ContactCustomTelemetryEvent(
  contactId = contactId,
  linkedPrisonerCount = linkedPrisonersCount,
  telemetryCustomEventType = getEvent(eventActionType),
  source = eventSource.name,
  user = eventUser,
) {
  override fun customProperties(): Map<String, String> = mapOf(
    "prisoner_contact_id" to contactApprovedVisitorCustomProperties.prisonerContactId.toString(),
    "prisoner_number" to contactApprovedVisitorCustomProperties.prisonerNumber,
  )

  companion object {
    fun getEvent(eventActionType: EventActionType): TelemetryCustomEventType = when (eventActionType) {
      CREATE -> TelemetryCustomEventType.CONTACT_APPROVED_VISITOR_CREATED_EVENT
      UPDATE -> throw UnsupportedOperationException("Update not supported for approved visitor")
      DELETE -> TelemetryCustomEventType.CONTACT_APPROVED_VISITOR_DELETED_EVENT
    }
  }

  constructor(
    contactId: Long,
    prisonerContactId: Long,
    linkedPrisonersCount: Long,
    prisonerNumber: String,
    eventActionType: EventActionType,
    eventSource: Source,
    eventUser: User,
  ) : this(
    contactId = contactId,
    linkedPrisonersCount = linkedPrisonersCount,
    contactApprovedVisitorCustomProperties = ContactApprovedVisitorCustomProperties(prisonerContactId = prisonerContactId, prisonerNumber = prisonerNumber),
    eventActionType = eventActionType,
    eventSource = eventSource,
    eventUser = eventUser,
  )
}

internal class ContactApprovedVisitorCustomProperties(
  val prisonerContactId: Long,
  val prisonerNumber: String,
)
