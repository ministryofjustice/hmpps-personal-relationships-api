package uk.gov.justice.digital.hmpps.personalrelationships.model.telemetry.contact

import uk.gov.justice.digital.hmpps.personalrelationships.config.User
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.ContactEmailDetails
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.sync.SyncContactEmail
import uk.gov.justice.digital.hmpps.personalrelationships.service.events.Source
import uk.gov.justice.digital.hmpps.personalrelationships.service.telemetry.EventActionType
import uk.gov.justice.digital.hmpps.personalrelationships.service.telemetry.EventActionType.CREATE
import uk.gov.justice.digital.hmpps.personalrelationships.service.telemetry.EventActionType.DELETE
import uk.gov.justice.digital.hmpps.personalrelationships.service.telemetry.EventActionType.UPDATE
import uk.gov.justice.digital.hmpps.personalrelationships.service.telemetry.TelemetryCustomEventType
import uk.gov.justice.digital.hmpps.personalrelationships.service.telemetry.TelemetryCustomEventType.CONTACT_EMAIL_CREATED_EVENT
import uk.gov.justice.digital.hmpps.personalrelationships.service.telemetry.TelemetryCustomEventType.CONTACT_EMAIL_DELETED_EVENT
import uk.gov.justice.digital.hmpps.personalrelationships.service.telemetry.TelemetryCustomEventType.CONTACT_EMAIL_UPDATED_EVENT

class ContactEmailCustomEvent private constructor(
  override val contactId: Long,
  private val contactEmailCustomProperties: ContactEmailCustomProperties,
  val eventActionType: EventActionType,
  val eventSource: Source,
  val eventUser: User,
) : ContactCustomTelemetryEvent(contactId, telemetryCustomEventType = getEvent(eventActionType), source = eventSource.name, user = eventUser) {
  override fun customProperties(): Map<String, String> = emptyMap()

  companion object {
    fun getEvent(eventActionType: EventActionType): TelemetryCustomEventType = when (eventActionType) {
      CREATE -> CONTACT_EMAIL_CREATED_EVENT
      UPDATE -> CONTACT_EMAIL_UPDATED_EVENT
      DELETE -> CONTACT_EMAIL_DELETED_EVENT
    }
  }

  constructor(
    contactId: Long,
    contactEmailDetails: ContactEmailDetails,
    eventActionType: EventActionType,
    eventSource: Source,
    eventUser: User,
  ) : this(contactId, ContactEmailCustomProperties(contactEmailDetails), eventActionType, eventSource, eventUser)

  constructor(
    contactId: Long,
    syncContactEmail: SyncContactEmail,
    eventActionType: EventActionType,
    eventSource: Source,
    eventUser: User,
  ) : this(contactId, ContactEmailCustomProperties(syncContactEmail), eventActionType, eventSource, eventUser)

  constructor(
    contactId: Long,
    contactEmailId: Long,
    eventActionType: EventActionType,
    eventSource: Source,
    eventUser: User,
  ) : this(contactId, ContactEmailCustomProperties(contactEmailId, null), eventActionType, eventSource, eventUser)
}

internal class ContactEmailCustomProperties(
  contactEmailId: Long,
  val email: String?,
) {
  constructor(syncContactEmail: SyncContactEmail) : this(syncContactEmail.contactEmailId, email = syncContactEmail.emailAddress)
  constructor(contactEmailDetails: ContactEmailDetails) : this(contactEmailDetails.contactEmailId, email = contactEmailDetails.emailAddress)
}
