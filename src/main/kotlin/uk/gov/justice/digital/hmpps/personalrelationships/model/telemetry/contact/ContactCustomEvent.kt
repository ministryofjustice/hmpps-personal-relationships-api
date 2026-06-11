package uk.gov.justice.digital.hmpps.personalrelationships.model.telemetry.contact

import uk.gov.justice.digital.hmpps.personalrelationships.config.User
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.ContactDetails
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.PatchContactResponse
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.sync.SyncContact
import uk.gov.justice.digital.hmpps.personalrelationships.service.events.Source
import uk.gov.justice.digital.hmpps.personalrelationships.service.telemetry.EventActionType
import uk.gov.justice.digital.hmpps.personalrelationships.service.telemetry.EventActionType.CREATE
import uk.gov.justice.digital.hmpps.personalrelationships.service.telemetry.EventActionType.DELETE
import uk.gov.justice.digital.hmpps.personalrelationships.service.telemetry.EventActionType.UPDATE
import uk.gov.justice.digital.hmpps.personalrelationships.service.telemetry.TelemetryCustomEventType
import uk.gov.justice.digital.hmpps.personalrelationships.service.telemetry.TelemetryCustomEventType.CONTACT_CREATED_EVENT
import uk.gov.justice.digital.hmpps.personalrelationships.service.telemetry.TelemetryCustomEventType.CONTACT_DELETED_EVENT
import uk.gov.justice.digital.hmpps.personalrelationships.service.telemetry.TelemetryCustomEventType.CONTACT_UPDATED_EVENT

class ContactCustomEvent private constructor(
  override val contactId: Long,
  val linkedPrisonersCount: Long,
  private val contactCustomProperties: ContactCustomProperties,
  val eventActionType: EventActionType,
  val eventSource: Source,
  val eventUser: User,
) : ContactCustomTelemetryEvent(
  contactId = contactId,
  linkedPrisonerCount = linkedPrisonersCount,
  telemetryCustomEventType = getEvent(eventActionType = eventActionType),
  source = eventSource.name,
  user = eventUser,
) {
  override fun customProperties(): Map<String, String> = emptyMap()

  companion object {
    fun getEvent(eventActionType: EventActionType): TelemetryCustomEventType = when (eventActionType) {
      CREATE -> CONTACT_CREATED_EVENT
      UPDATE -> CONTACT_UPDATED_EVENT
      DELETE -> CONTACT_DELETED_EVENT
    }
  }

  constructor(
    contactId: Long,
    syncContact: SyncContact,
    linkedPrisonersCount: Long,
    eventActionType: EventActionType,
    eventSource: Source,
    eventUser: User,
  ) : this(
    contactId = contactId,
    linkedPrisonersCount = linkedPrisonersCount,
    contactCustomProperties = ContactCustomProperties(syncContact),
    eventActionType = eventActionType,
    eventSource = eventSource,
    eventUser = eventUser,
  )

  constructor(
    contactId: Long,
    contactDetails: ContactDetails,
    linkedPrisonersCount: Long,
    eventActionType: EventActionType,
    eventSource: Source,
    eventUser: User,
  ) : this(
    contactId = contactId,
    linkedPrisonersCount = linkedPrisonersCount,
    contactCustomProperties = ContactCustomProperties(contactDetails),
    eventActionType = eventActionType,
    eventSource = eventSource,
    eventUser = eventUser,
  )

  constructor(
    contactId: Long,
    patchContactResponse: PatchContactResponse,
    linkedPrisonersCount: Long,
    eventActionType: EventActionType,
    eventSource: Source,
    eventUser: User,
  ) : this(
    contactId = contactId,
    linkedPrisonersCount = linkedPrisonersCount,
    contactCustomProperties = ContactCustomProperties(patchContactResponse),
    eventActionType = eventActionType,
    eventSource = eventSource,
    eventUser = eventUser,
  )

  constructor(
    contactId: Long,
    linkedPrisonersCount: Long,
    eventActionType: EventActionType,
    eventSource: Source,
    eventUser: User,
  ) : this(
    contactId = contactId,
    linkedPrisonersCount = linkedPrisonersCount,
    contactCustomProperties = ContactCustomProperties(null, null),
    eventActionType = eventActionType,
    eventSource = eventSource,
    eventUser = eventUser,
  )
}

internal class ContactCustomProperties(
  val firstName: String?,
  val lastName: String?,
) {
  constructor(syncContact: SyncContact) : this(firstName = syncContact.firstName, lastName = syncContact.lastName)
  constructor(contactDetails: ContactDetails) : this(firstName = contactDetails.firstName, lastName = contactDetails.lastName)
  constructor(patchContactResponse: PatchContactResponse) : this(firstName = patchContactResponse.firstName, lastName = patchContactResponse.lastName)
}
