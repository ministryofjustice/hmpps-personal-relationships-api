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
    val properties = mutableMapOf(
      "prisoner_contact_id" to prisonerContactCustomProperties.prisonerContactId.toString(),
      "prisoner_number" to prisonerContactCustomProperties.prisonerNumber,
    )

    prisonerContactCustomProperties.relationshipTypeCode?.let {
      properties["group_code"] = it
    }

    prisonerContactCustomProperties.relationshipToPrisonerCode?.let {
      properties["relationship_code"] = it
    }

    prisonerContactCustomProperties.isRelationshipActive?.let {
      properties["relationship_status"] = if (it) "active" else "inactive"
    }

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
    relationshipTypeCode: String? = null,
    relationshipToPrisonerCode: String? = null,
    activeRelationship: Boolean? = null,
    eventActionType: EventActionType,
    eventSource: Source,
    eventUser: User,
  ) : this(contactId, PrisonerContactCustomProperties(prisonerContactId = prisonerContactId, prisonerNumber = prisonerNumber, relationshipTypeCode = relationshipTypeCode, relationshipToPrisonerCode = relationshipToPrisonerCode, isRelationshipActive = activeRelationship), eventActionType, eventSource, eventUser)
}

internal class PrisonerContactCustomProperties(
  val prisonerContactId: Long,
  val prisonerNumber: String,
  val relationshipTypeCode: String? = null,
  val relationshipToPrisonerCode: String? = null,
  val isRelationshipActive: Boolean? = null,
) {
  constructor(syncPrisonerContact: SyncPrisonerContact) : this(
    prisonerContactId = syncPrisonerContact.id,
    prisonerNumber = syncPrisonerContact.prisonerNumber,
    relationshipTypeCode = syncPrisonerContact.contactType,
    relationshipToPrisonerCode = syncPrisonerContact.relationshipType,
    isRelationshipActive = syncPrisonerContact.active,
  )
  constructor(prisonerContactRelationship: PrisonerContactRelationshipDetails) : this(
    prisonerContactId = prisonerContactRelationship.prisonerContactId,
    prisonerNumber = prisonerContactRelationship.prisonerNumber,
    relationshipTypeCode = prisonerContactRelationship.relationshipTypeCode,
    relationshipToPrisonerCode = prisonerContactRelationship.relationshipToPrisonerCode,
    isRelationshipActive = prisonerContactRelationship.isRelationshipActive,
  )
  constructor(relationshipApproved: RelationshipsApproved) : this(relationshipApproved.prisonerContactId, relationshipApproved.prisonerNumber)
}
