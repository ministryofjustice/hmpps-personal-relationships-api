package uk.gov.justice.digital.hmpps.personalrelationships.model.telemetry

import uk.gov.justice.digital.hmpps.personalrelationships.config.User
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.ContactAddressDetails
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.ContactAddressResponse
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.sync.SyncContactAddress
import uk.gov.justice.digital.hmpps.personalrelationships.service.events.Source
import uk.gov.justice.digital.hmpps.personalrelationships.service.telemetry.EventActionType
import uk.gov.justice.digital.hmpps.personalrelationships.service.telemetry.EventActionType.CREATE
import uk.gov.justice.digital.hmpps.personalrelationships.service.telemetry.EventActionType.DELETE
import uk.gov.justice.digital.hmpps.personalrelationships.service.telemetry.EventActionType.UPDATE
import uk.gov.justice.digital.hmpps.personalrelationships.service.telemetry.TelemetryCustomEventType
import uk.gov.justice.digital.hmpps.personalrelationships.service.telemetry.TelemetryCustomEventType.CONTACT_ADDRESS_CREATED_EVENT
import uk.gov.justice.digital.hmpps.personalrelationships.service.telemetry.TelemetryCustomEventType.CONTACT_ADDRESS_DELETED_EVENT
import uk.gov.justice.digital.hmpps.personalrelationships.service.telemetry.TelemetryCustomEventType.CONTACT_ADDRESS_UPDATED_EVENT

class ContactAddressCustomEvent private constructor(
  override val contactId: Long,
  private val contactAddressCustomProperties: ContactAddressCustomProperties,
  eventActionType: EventActionType,
  eventSource: Source,
  eventUser: User,
) : ContactCustomTelemetryEvent(contactId, telemetryCustomEventType = getEvent(eventActionType), source = eventSource.name, user = eventUser) {
  override fun customProperties(): Map<String, String> = emptyMap()

  companion object {
    fun getEvent(eventActionType: EventActionType): TelemetryCustomEventType = when (eventActionType) {
      CREATE -> CONTACT_ADDRESS_CREATED_EVENT
      UPDATE -> CONTACT_ADDRESS_UPDATED_EVENT
      DELETE -> CONTACT_ADDRESS_DELETED_EVENT
    }
  }

  constructor(
    contactId: Long,
    contactAddressDetails: ContactAddressDetails,
    eventActionType: EventActionType,
    eventSource: Source,
    eventUser: User,
  ) : this(contactId, ContactAddressCustomProperties(contactAddressDetails), eventActionType, eventSource, eventUser)

  constructor(
    contactId: Long,
    syncContactAddress: SyncContactAddress,
    eventActionType: EventActionType,
    eventSource: Source,
    eventUser: User,
  ) : this(contactId, ContactAddressCustomProperties(syncContactAddress), eventActionType, eventSource, eventUser)

  constructor(
    contactId: Long,
    contactAddressResponse: ContactAddressResponse,
    eventActionType: EventActionType,
    eventSource: Source,
    eventUser: User,
  ) : this(contactId, ContactAddressCustomProperties(contactAddressResponse), eventActionType, eventSource, eventUser)

  constructor(
    contactId: Long,
    contactAddressId: Long,
    eventActionType: EventActionType,
    eventSource: Source,
    eventUser: User,
  ) : this(contactId, ContactAddressCustomProperties(contactAddressId), eventActionType, eventSource, eventUser)
}

internal class ContactAddressCustomProperties(contactAddressId: Long) {
  constructor(syncContactAddress: SyncContactAddress) : this(syncContactAddress.contactAddressId)
  constructor(contactAddressDetails: ContactAddressDetails) : this(contactAddressDetails.contactAddressId)
  constructor(contactAddressResponse: ContactAddressResponse) : this(contactAddressResponse.contactAddressId)
}
