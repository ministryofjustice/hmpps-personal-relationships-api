package uk.gov.justice.digital.hmpps.personalrelationships.model.telemetry.contact

import uk.gov.justice.digital.hmpps.personalrelationships.config.User
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.PrisonerContactRestrictionDetails
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.sync.SyncPrisonerContactRestriction
import uk.gov.justice.digital.hmpps.personalrelationships.service.events.Source
import uk.gov.justice.digital.hmpps.personalrelationships.service.telemetry.EventActionType
import uk.gov.justice.digital.hmpps.personalrelationships.service.telemetry.EventActionType.CREATE
import uk.gov.justice.digital.hmpps.personalrelationships.service.telemetry.EventActionType.DELETE
import uk.gov.justice.digital.hmpps.personalrelationships.service.telemetry.EventActionType.UPDATE
import uk.gov.justice.digital.hmpps.personalrelationships.service.telemetry.TelemetryCustomEventType
import uk.gov.justice.digital.hmpps.personalrelationships.service.telemetry.TelemetryCustomEventType.PRISONER_CONTACT_RESTRICTION_CREATED_EVENT
import uk.gov.justice.digital.hmpps.personalrelationships.service.telemetry.TelemetryCustomEventType.PRISONER_CONTACT_RESTRICTION_DELETED_EVENT
import uk.gov.justice.digital.hmpps.personalrelationships.service.telemetry.TelemetryCustomEventType.PRISONER_CONTACT_RESTRICTION_UPDATED_EVENT

class PrisonerContactRestrictionCustomEvent private constructor(
  override val contactId: Long,
  private val prisonerContactRestrictionCustomProperties: PrisonerContactRestrictionCustomProperties,
  eventActionType: EventActionType,
  eventSource: Source,
  eventUser: User,
) : ContactCustomTelemetryEvent(contactId, getEvent(eventActionType), eventSource.name, eventUser) {
  override fun customProperties(): Map<String, String> {
    val customProperties = mutableMapOf("prisoner_contact_restriction_id" to prisonerContactRestrictionCustomProperties.prisonerContactRestrictionId.toString())
    prisonerContactRestrictionCustomProperties.restrictionType?.let {
      customProperties["restrictionType"] = it
    }
    return customProperties.toMap()
  }

  companion object {
    fun getEvent(eventActionType: EventActionType): TelemetryCustomEventType = when (eventActionType) {
      CREATE -> PRISONER_CONTACT_RESTRICTION_CREATED_EVENT
      UPDATE -> PRISONER_CONTACT_RESTRICTION_UPDATED_EVENT
      DELETE -> PRISONER_CONTACT_RESTRICTION_DELETED_EVENT
    }
  }

  constructor(
    contactId: Long,
    prisonerContactRestrictionDetails: PrisonerContactRestrictionDetails,
    eventActionType: EventActionType,
    eventSource: Source,
    eventUser: User,
  ) : this(contactId, PrisonerContactRestrictionCustomProperties(prisonerContactRestrictionDetails), eventActionType, eventSource, eventUser)

  constructor(
    contactId: Long,
    syncPrisonerContactRestriction: SyncPrisonerContactRestriction,
    eventActionType: EventActionType,
    eventSource: Source,
    eventUser: User,
  ) : this(contactId, PrisonerContactRestrictionCustomProperties(syncPrisonerContactRestriction), eventActionType, eventSource, eventUser)

  constructor(
    contactId: Long,
    contactRestrictionId: Long,
    eventActionType: EventActionType,
    eventSource: Source,
    eventUser: User,
  ) : this(contactId, PrisonerContactRestrictionCustomProperties(contactRestrictionId, null), eventActionType, eventSource, eventUser)
}

internal class PrisonerContactRestrictionCustomProperties(
  val prisonerContactRestrictionId: Long,
  val restrictionType: String?,
) {
  constructor(syncPrisonerContactRestriction: SyncPrisonerContactRestriction) : this(syncPrisonerContactRestriction.prisonerContactRestrictionId, syncPrisonerContactRestriction.restrictionType)
  constructor(prisonerContactRestrictionDetails: PrisonerContactRestrictionDetails) : this(prisonerContactRestrictionDetails.prisonerContactRestrictionId, prisonerContactRestrictionDetails.restrictionType)
}
