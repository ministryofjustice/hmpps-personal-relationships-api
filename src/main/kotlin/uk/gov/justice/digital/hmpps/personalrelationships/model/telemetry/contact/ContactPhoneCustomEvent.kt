package uk.gov.justice.digital.hmpps.personalrelationships.model.telemetry.contact

import uk.gov.justice.digital.hmpps.personalrelationships.config.User
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.ContactPhoneDetails
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.sync.SyncContactPhone
import uk.gov.justice.digital.hmpps.personalrelationships.service.events.Source
import uk.gov.justice.digital.hmpps.personalrelationships.service.telemetry.EventActionType
import uk.gov.justice.digital.hmpps.personalrelationships.service.telemetry.EventActionType.CREATE
import uk.gov.justice.digital.hmpps.personalrelationships.service.telemetry.EventActionType.DELETE
import uk.gov.justice.digital.hmpps.personalrelationships.service.telemetry.EventActionType.UPDATE
import uk.gov.justice.digital.hmpps.personalrelationships.service.telemetry.TelemetryCustomEventType
import uk.gov.justice.digital.hmpps.personalrelationships.service.telemetry.TelemetryCustomEventType.CONTACT_PHONE_CREATED_EVENT
import uk.gov.justice.digital.hmpps.personalrelationships.service.telemetry.TelemetryCustomEventType.CONTACT_PHONE_DELETED_EVENT
import uk.gov.justice.digital.hmpps.personalrelationships.service.telemetry.TelemetryCustomEventType.CONTACT_PHONE_UPDATED_EVENT

class ContactPhoneCustomEvent private constructor(
  override val contactId: Long,
  private val contactPhoneCustomProperties: ContactPhoneCustomProperties,
  val eventActionType: EventActionType,
  val eventSource: Source,
  val eventUser: User,
) : ContactCustomTelemetryEvent(contactId, getEvent(eventActionType), eventSource.name, eventUser) {
  override fun customProperties(): Map<String, String> = mapOf("contact_phone_id" to contactPhoneCustomProperties.contactPhoneId.toString())

  companion object {
    fun getEvent(eventActionType: EventActionType): TelemetryCustomEventType = when (eventActionType) {
      CREATE -> CONTACT_PHONE_CREATED_EVENT
      UPDATE -> CONTACT_PHONE_UPDATED_EVENT
      DELETE -> CONTACT_PHONE_DELETED_EVENT
    }
  }

  constructor(
    contactId: Long,
    contactPhoneDetails: ContactPhoneDetails,
    eventActionType: EventActionType,
    eventSource: Source,
    eventUser: User,
  ) : this(contactId, ContactPhoneCustomProperties(contactPhoneDetails), eventActionType, eventSource, eventUser)

  constructor(
    contactId: Long,
    syncContactPhone: SyncContactPhone,
    eventActionType: EventActionType,
    eventSource: Source,
    eventUser: User,
  ) : this(contactId, ContactPhoneCustomProperties(syncContactPhone), eventActionType, eventSource, eventUser)

  constructor(
    contactId: Long,
    contactPhoneId: Long,
    eventActionType: EventActionType,
    eventSource: Source,
    eventUser: User,
  ) : this(contactId, ContactPhoneCustomProperties(contactPhoneId, null), eventActionType, eventSource, eventUser)
}

internal class ContactPhoneCustomProperties(
  val contactPhoneId: Long,
  val number: String?,
) {
  constructor(syncContactPhone: SyncContactPhone) : this(syncContactPhone.contactPhoneId, syncContactPhone.phoneNumber)
  constructor(contactPhoneDetails: ContactPhoneDetails) : this(contactPhoneDetails.contactPhoneId, contactPhoneDetails.phoneNumber)
}
