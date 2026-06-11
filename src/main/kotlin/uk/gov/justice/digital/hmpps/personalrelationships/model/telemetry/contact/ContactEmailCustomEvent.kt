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
  val linkedPrisonersCount: Long,
  private val contactEmailCustomProperties: ContactEmailCustomProperties,
  val eventActionType: EventActionType,
  val eventSource: Source,
  val eventUser: User,
) : ContactCustomTelemetryEvent(
  contactId = contactId,
  telemetryCustomEventType = getEvent(eventActionType),
  source = eventSource.name,
  user = eventUser,
  linkedPrisonerCount = linkedPrisonersCount,
) {
  override fun customProperties(): Map<String, String> = mapOf("contact_email_id" to contactEmailCustomProperties.contactEmailId.toString())

  companion object {
    fun getEvent(eventActionType: EventActionType): TelemetryCustomEventType = when (eventActionType) {
      CREATE -> CONTACT_EMAIL_CREATED_EVENT
      UPDATE -> CONTACT_EMAIL_UPDATED_EVENT
      DELETE -> CONTACT_EMAIL_DELETED_EVENT
    }
  }

  constructor(
    contactId: Long,
    linkedPrisonersCount: Long,
    contactEmailDetails: ContactEmailDetails,
    eventActionType: EventActionType,
    eventSource: Source,
    eventUser: User,
  ) : this(
    contactId = contactId,
    linkedPrisonersCount = linkedPrisonersCount,
    contactEmailCustomProperties = ContactEmailCustomProperties(contactEmailDetails),
    eventActionType = eventActionType,
    eventSource = eventSource,
    eventUser = eventUser,
  )

  constructor(
    contactId: Long,
    linkedPrisonersCount: Long,
    syncContactEmail: SyncContactEmail,
    eventActionType: EventActionType,
    eventSource: Source,
    eventUser: User,
  ) : this(
    contactId = contactId,
    linkedPrisonersCount = linkedPrisonersCount,
    contactEmailCustomProperties = ContactEmailCustomProperties(syncContactEmail),
    eventActionType = eventActionType,
    eventSource = eventSource,
    eventUser = eventUser,
  )

  constructor(
    contactId: Long,
    contactEmailId: Long,
    linkedPrisonersCount: Long,
    eventActionType: EventActionType,
    eventSource: Source,
    eventUser: User,
  ) : this(
    contactId = contactId,
    linkedPrisonersCount = linkedPrisonersCount,
    contactEmailCustomProperties = ContactEmailCustomProperties(contactEmailId, null),
    eventActionType = eventActionType,
    eventSource = eventSource,
    eventUser = eventUser,
  )
}

internal class ContactEmailCustomProperties(
  val contactEmailId: Long,
  val email: String?,
) {
  constructor(syncContactEmail: SyncContactEmail) : this(syncContactEmail.contactEmailId, email = syncContactEmail.emailAddress)
  constructor(contactEmailDetails: ContactEmailDetails) : this(contactEmailDetails.contactEmailId, email = contactEmailDetails.emailAddress)
}
