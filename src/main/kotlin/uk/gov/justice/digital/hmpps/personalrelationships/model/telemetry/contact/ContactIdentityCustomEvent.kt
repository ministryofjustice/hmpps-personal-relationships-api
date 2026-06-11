package uk.gov.justice.digital.hmpps.personalrelationships.model.telemetry.contact

import uk.gov.justice.digital.hmpps.personalrelationships.config.User
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.ContactIdentityDetails
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.sync.SyncContactIdentity
import uk.gov.justice.digital.hmpps.personalrelationships.service.events.Source
import uk.gov.justice.digital.hmpps.personalrelationships.service.telemetry.EventActionType
import uk.gov.justice.digital.hmpps.personalrelationships.service.telemetry.EventActionType.CREATE
import uk.gov.justice.digital.hmpps.personalrelationships.service.telemetry.EventActionType.DELETE
import uk.gov.justice.digital.hmpps.personalrelationships.service.telemetry.EventActionType.UPDATE
import uk.gov.justice.digital.hmpps.personalrelationships.service.telemetry.TelemetryCustomEventType
import uk.gov.justice.digital.hmpps.personalrelationships.service.telemetry.TelemetryCustomEventType.CONTACT_IDENTITY_CREATED_EVENT
import uk.gov.justice.digital.hmpps.personalrelationships.service.telemetry.TelemetryCustomEventType.CONTACT_IDENTITY_DELETED_EVENT
import uk.gov.justice.digital.hmpps.personalrelationships.service.telemetry.TelemetryCustomEventType.CONTACT_IDENTITY_UPDATED_EVENT

class ContactIdentityCustomEvent private constructor(
  override val contactId: Long,
  val linkedPrisonersCount: Long,
  private val contactIdentityCustomProperties: ContactIdentityCustomProperties,
  val eventActionType: EventActionType,
  val eventSource: Source,
  val eventUser: User,
) : ContactCustomTelemetryEvent(contactId, linkedPrisonersCount, getEvent(eventActionType), eventSource.name, eventUser) {
  override fun customProperties(): Map<String, String> = mapOf("contact_identity_id" to contactIdentityCustomProperties.contactIdentityId.toString())

  companion object {
    fun getEvent(eventActionType: EventActionType): TelemetryCustomEventType = when (eventActionType) {
      CREATE -> CONTACT_IDENTITY_CREATED_EVENT
      UPDATE -> CONTACT_IDENTITY_UPDATED_EVENT
      DELETE -> CONTACT_IDENTITY_DELETED_EVENT
    }
  }

  constructor(
    contactId: Long,
    linkedPrisonersCount: Long,
    contactIdentityDetails: ContactIdentityDetails,
    eventActionType: EventActionType,
    eventSource: Source,
    eventUser: User,
  ) : this(
    contactId = contactId,
    linkedPrisonersCount = linkedPrisonersCount,
    contactIdentityCustomProperties = ContactIdentityCustomProperties(contactIdentityDetails),
    eventActionType = eventActionType,
    eventSource = eventSource,
    eventUser = eventUser,
  )

  constructor(
    contactId: Long,
    linkedPrisonersCount: Long,
    syncContactIdentity: SyncContactIdentity,
    eventActionType: EventActionType,
    eventSource: Source,
    eventUser: User,
  ) : this(
    contactId = contactId,
    linkedPrisonersCount = linkedPrisonersCount,
    contactIdentityCustomProperties = ContactIdentityCustomProperties(syncContactIdentity),
    eventActionType = eventActionType,
    eventSource = eventSource,
    eventUser = eventUser,
  )

  constructor(
    contactId: Long,
    contactIdentityId: Long,
    linkedPrisonersCount: Long,
    eventActionType: EventActionType,
    eventSource: Source,
    eventUser: User,
  ) : this(
    contactId = contactId,
    linkedPrisonersCount = linkedPrisonersCount,
    contactIdentityCustomProperties = ContactIdentityCustomProperties(contactIdentityId),
    eventActionType = eventActionType,
    eventSource = eventSource,
    eventUser = eventUser,
  )
}

internal class ContactIdentityCustomProperties(
  val contactIdentityId: Long,
) {
  constructor(syncContactIdentity: SyncContactIdentity) : this(syncContactIdentity.contactIdentityId)
  constructor(contactIdentityDetails: ContactIdentityDetails) : this(contactIdentityDetails.contactIdentityId)
}
