package uk.gov.justice.digital.hmpps.personalrelationships.model.telemetry.contact

import uk.gov.justice.digital.hmpps.personalrelationships.config.User
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.ContactRestrictionDetails
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.sync.SyncContactRestriction
import uk.gov.justice.digital.hmpps.personalrelationships.service.events.Source
import uk.gov.justice.digital.hmpps.personalrelationships.service.telemetry.EventActionType
import uk.gov.justice.digital.hmpps.personalrelationships.service.telemetry.EventActionType.CREATE
import uk.gov.justice.digital.hmpps.personalrelationships.service.telemetry.EventActionType.DELETE
import uk.gov.justice.digital.hmpps.personalrelationships.service.telemetry.EventActionType.UPDATE
import uk.gov.justice.digital.hmpps.personalrelationships.service.telemetry.TelemetryCustomEventType
import uk.gov.justice.digital.hmpps.personalrelationships.service.telemetry.TelemetryCustomEventType.CONTACT_RESTRICTION_CREATED_EVENT
import uk.gov.justice.digital.hmpps.personalrelationships.service.telemetry.TelemetryCustomEventType.CONTACT_RESTRICTION_DELETED_EVENT
import uk.gov.justice.digital.hmpps.personalrelationships.service.telemetry.TelemetryCustomEventType.CONTACT_RESTRICTION_UPDATED_EVENT

class ContactRestrictionCustomEvent private constructor(
  override val contactId: Long,
  private val contactRestrictionCustomProperties: ContactRestrictionCustomProperties,
  eventActionType: EventActionType,
  eventSource: Source,
  eventUser: User,
) : ContactCustomTelemetryEvent(contactId, getEvent(eventActionType), eventSource.name, eventUser) {
  override fun customProperties(): Map<String, String> {
    val customProperties = mutableMapOf("contact_restriction_id" to contactRestrictionCustomProperties.contactRestrictionId.toString())
    contactRestrictionCustomProperties.restrictionType?.let { customProperties["restriction_code"] = it }
    return customProperties.toMap()
  }

  companion object {
    fun getEvent(eventActionType: EventActionType): TelemetryCustomEventType = when (eventActionType) {
      CREATE -> CONTACT_RESTRICTION_CREATED_EVENT
      UPDATE -> CONTACT_RESTRICTION_UPDATED_EVENT
      DELETE -> CONTACT_RESTRICTION_DELETED_EVENT
    }
  }

  constructor(
    contactId: Long,
    contactRestrictionDetails: ContactRestrictionDetails,
    eventActionType: EventActionType,
    eventSource: Source,
    eventUser: User,
  ) : this(contactId, ContactRestrictionCustomProperties(contactRestrictionDetails), eventActionType, eventSource, eventUser)

  constructor(
    contactId: Long,
    syncContactRestriction: SyncContactRestriction,
    eventActionType: EventActionType,
    eventSource: Source,
    eventUser: User,
  ) : this(contactId, ContactRestrictionCustomProperties(syncContactRestriction), eventActionType, eventSource, eventUser)
}

internal class ContactRestrictionCustomProperties(
  val contactRestrictionId: Long,
  val restrictionType: String?,
) {
  constructor(syncContactRestriction: SyncContactRestriction) : this(syncContactRestriction.contactRestrictionId, syncContactRestriction.restrictionType)
  constructor(contactRestrictionDetails: ContactRestrictionDetails) : this(contactRestrictionDetails.contactRestrictionId, contactRestrictionDetails.restrictionType)
}
