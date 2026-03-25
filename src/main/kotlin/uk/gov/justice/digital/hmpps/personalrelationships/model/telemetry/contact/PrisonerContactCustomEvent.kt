package uk.gov.justice.digital.hmpps.personalrelationships.model.telemetry.contact

import uk.gov.justice.digital.hmpps.personalrelationships.config.User
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.PrisonerContactRelationshipDetails
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.RelationshipsApproved
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.sync.SyncPrisonerContact
import uk.gov.justice.digital.hmpps.personalrelationships.service.events.Source
import uk.gov.justice.digital.hmpps.personalrelationships.service.telemetry.EventActionType
import uk.gov.justice.digital.hmpps.personalrelationships.service.telemetry.EventActionType.CREATE
import uk.gov.justice.digital.hmpps.personalrelationships.service.telemetry.EventActionType.DELETE
import uk.gov.justice.digital.hmpps.personalrelationships.service.telemetry.EventActionType.UPDATE
import uk.gov.justice.digital.hmpps.personalrelationships.service.telemetry.TelemetryCustomEventType
import uk.gov.justice.digital.hmpps.personalrelationships.service.telemetry.TelemetryCustomEventType.PRISONER_CONTACT_CREATED_EVENT
import uk.gov.justice.digital.hmpps.personalrelationships.service.telemetry.TelemetryCustomEventType.PRISONER_CONTACT_DELETED_EVENT
import uk.gov.justice.digital.hmpps.personalrelationships.service.telemetry.TelemetryCustomEventType.PRISONER_CONTACT_UPDATED_EVENT

class PrisonerContactCustomEvent private constructor(
  override val contactId: Long,
  private val prisonerContactCustomProperties: PrisonerContactCustomProperties,
  val eventActionType: EventActionType,
  val eventSource: Source,
  val eventUser: User,
) : ContactCustomTelemetryEvent(contactId, telemetryCustomEventType = getEvent(eventActionType), source = eventSource.name, user = eventUser) {
  override fun customProperties(): Map<String, String> {
    val properties = mapOf(
      "prisoner_contact_id" to prisonerContactCustomProperties.prisonerContactId.toString(),
      "prisoner_number" to prisonerContactCustomProperties.prisonerNumber,
    )
    return properties.toMap()
  }

  companion object {
    fun getEvent(eventActionType: EventActionType): TelemetryCustomEventType = when (eventActionType) {
      CREATE -> PRISONER_CONTACT_CREATED_EVENT
      UPDATE -> PRISONER_CONTACT_UPDATED_EVENT
      DELETE -> PRISONER_CONTACT_DELETED_EVENT
    }
  }

  constructor(
    contactId: Long,
    prisonerContactRelationship: PrisonerContactRelationshipDetails,
    eventActionType: EventActionType,
    eventSource: Source,
    eventUser: User,
  ) : this(contactId, PrisonerContactCustomProperties(prisonerContactRelationship), eventActionType, eventSource, eventUser)

  constructor(
    contactId: Long,
    syncPrisonerContact: SyncPrisonerContact,
    eventActionType: EventActionType,
    eventSource: Source,
    eventUser: User,
  ) : this(contactId, PrisonerContactCustomProperties(syncPrisonerContact), eventActionType, eventSource, eventUser)

  constructor(
    contactId: Long,
    relationshipApproved: RelationshipsApproved,
    eventActionType: EventActionType,
    eventSource: Source,
    eventUser: User,
  ) : this(contactId, PrisonerContactCustomProperties(relationshipApproved), eventActionType, eventSource, eventUser)

  constructor(
    contactId: Long,
    prisonerContactId: Long,
    prisonerNumber: String,
    eventActionType: EventActionType,
    eventSource: Source,
    eventUser: User,
  ) : this(contactId, PrisonerContactCustomProperties(prisonerContactId, prisonerNumber), eventActionType, eventSource, eventUser)
}

internal class PrisonerContactCustomProperties(
  val prisonerContactId: Long,
  val prisonerNumber: String,
) {
  constructor(syncPrisonerContact: SyncPrisonerContact) : this(syncPrisonerContact.id, syncPrisonerContact.prisonerNumber)
  constructor(prisonerContactRelationship: PrisonerContactRelationshipDetails) : this(prisonerContactRelationship.prisonerContactId, prisonerContactRelationship.prisonerNumber)
  constructor(relationshipApproved: RelationshipsApproved) : this(relationshipApproved.prisonerContactId, relationshipApproved.prisonerNumber)
}
