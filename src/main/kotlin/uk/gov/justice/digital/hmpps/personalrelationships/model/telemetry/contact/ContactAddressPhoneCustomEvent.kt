package uk.gov.justice.digital.hmpps.personalrelationships.model.telemetry.contact

import uk.gov.justice.digital.hmpps.personalrelationships.config.User
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.ContactAddressPhoneDetails
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.sync.SyncContactAddressPhone
import uk.gov.justice.digital.hmpps.personalrelationships.service.events.Source
import uk.gov.justice.digital.hmpps.personalrelationships.service.telemetry.EventActionType
import uk.gov.justice.digital.hmpps.personalrelationships.service.telemetry.EventActionType.CREATE
import uk.gov.justice.digital.hmpps.personalrelationships.service.telemetry.EventActionType.DELETE
import uk.gov.justice.digital.hmpps.personalrelationships.service.telemetry.EventActionType.UPDATE
import uk.gov.justice.digital.hmpps.personalrelationships.service.telemetry.TelemetryCustomEventType
import uk.gov.justice.digital.hmpps.personalrelationships.service.telemetry.TelemetryCustomEventType.CONTACT_ADDRESS_PHONE_CREATED_EVENT
import uk.gov.justice.digital.hmpps.personalrelationships.service.telemetry.TelemetryCustomEventType.CONTACT_ADDRESS_PHONE_DELETED_EVENT
import uk.gov.justice.digital.hmpps.personalrelationships.service.telemetry.TelemetryCustomEventType.CONTACT_ADDRESS_PHONE_UPDATED_EVENT

class ContactAddressPhoneCustomEvent private constructor(
  override val contactId: Long,
  val linkedPrisonersCount: Long,
  private val contactAddressPhoneCustomProperties: ContactAddressPhoneCustomProperties,
  val eventActionType: EventActionType,
  val eventSource: Source,
  val eventUser: User,
) : ContactCustomTelemetryEvent(contactId = contactId, telemetryCustomEventType = getEvent(eventActionType), source = eventSource.name, user = eventUser, linkedPrisonerCount = linkedPrisonersCount) {
  override fun customProperties(): Map<String, String> = mapOf("contact_address_phone_id" to contactAddressPhoneCustomProperties.contactAddressPhoneId.toString())

  companion object {
    fun getEvent(eventActionType: EventActionType): TelemetryCustomEventType = when (eventActionType) {
      CREATE -> CONTACT_ADDRESS_PHONE_CREATED_EVENT
      UPDATE -> CONTACT_ADDRESS_PHONE_UPDATED_EVENT
      DELETE -> CONTACT_ADDRESS_PHONE_DELETED_EVENT
    }
  }

  constructor(
    contactId: Long,
    linkedPrisonersCount: Long,
    syncContactAddressPhone: SyncContactAddressPhone,
    eventActionType: EventActionType,
    eventSource: Source,
    eventUser: User,
  ) : this(
    contactId = contactId,
    linkedPrisonersCount = linkedPrisonersCount,
    contactAddressPhoneCustomProperties = ContactAddressPhoneCustomProperties(syncContactAddressPhone),
    eventActionType = eventActionType,
    eventSource = eventSource,
    eventUser = eventUser,
  )

  constructor(
    contactId: Long,
    linkedPrisonersCount: Long,
    contactAddressPhoneDetails: ContactAddressPhoneDetails,
    eventActionType: EventActionType,
    eventSource: Source,
    eventUser: User,
  ) : this(
    contactId = contactId,
    linkedPrisonersCount = linkedPrisonersCount,
    contactAddressPhoneCustomProperties = ContactAddressPhoneCustomProperties(contactAddressPhoneDetails),
    eventActionType = eventActionType,
    eventSource = eventSource,
    eventUser = eventUser,
  )

  constructor(
    contactId: Long,
    contactAddressPhoneId: Long,
    linkedPrisonersCount: Long,
    eventActionType: EventActionType,
    eventSource: Source,
    eventUser: User,
  ) : this(
    contactId = contactId,
    linkedPrisonersCount = linkedPrisonersCount,
    contactAddressPhoneCustomProperties = ContactAddressPhoneCustomProperties(contactAddressPhoneId = contactAddressPhoneId),
    eventActionType = eventActionType,
    eventSource = eventSource,
    eventUser = eventUser,
  )
}

internal class ContactAddressPhoneCustomProperties(val contactAddressPhoneId: Long) {
  constructor(syncContactAddressPhone: SyncContactAddressPhone) : this(syncContactAddressPhone.contactAddressPhoneId)
  constructor(contactAddressPhoneDetails: ContactAddressPhoneDetails) : this(contactAddressPhoneDetails.contactAddressPhoneId)
}
