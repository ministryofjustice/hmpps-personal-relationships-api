package uk.gov.justice.digital.hmpps.personalrelationships.model.telemetry.prisoner

import uk.gov.justice.digital.hmpps.personalrelationships.config.User
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.PrisonerRestrictionDetails
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.sync.SyncPrisonerRestriction
import uk.gov.justice.digital.hmpps.personalrelationships.service.events.Source
import uk.gov.justice.digital.hmpps.personalrelationships.service.telemetry.EventActionType
import uk.gov.justice.digital.hmpps.personalrelationships.service.telemetry.EventActionType.CREATE
import uk.gov.justice.digital.hmpps.personalrelationships.service.telemetry.EventActionType.DELETE
import uk.gov.justice.digital.hmpps.personalrelationships.service.telemetry.EventActionType.UPDATE
import uk.gov.justice.digital.hmpps.personalrelationships.service.telemetry.TelemetryCustomEventType
import uk.gov.justice.digital.hmpps.personalrelationships.service.telemetry.TelemetryCustomEventType.PRISONER_RESTRICTION_CREATED_EVENT
import uk.gov.justice.digital.hmpps.personalrelationships.service.telemetry.TelemetryCustomEventType.PRISONER_RESTRICTION_DELETED_EVENT
import uk.gov.justice.digital.hmpps.personalrelationships.service.telemetry.TelemetryCustomEventType.PRISONER_RESTRICTION_UPDATED_EVENT

class PrisonerRestrictionCustomEvent private constructor(
  override val prisonerNumber: String,
  private val prisonerRestrictionCustomProperties: PrisonerRestrictionCustomProperties,
  val eventActionType: EventActionType,
  val eventSource: Source,
  val eventUser: User,
) : PrisonerCustomTelemetryEvent(prisonerNumber, telemetryCustomEventType = getEvent(eventActionType), source = eventSource.name, user = eventUser) {
  override fun customProperties(): Map<String, String> {
    val customProperties = mutableMapOf("prisoner_restriction_id" to prisonerRestrictionCustomProperties.prisonerRestrictionId.toString())
    prisonerRestrictionCustomProperties.restrictionType?.let { customProperties["restrictionType"] = it }
    return customProperties.toMap()
  }

  companion object {
    fun getEvent(eventActionType: EventActionType): TelemetryCustomEventType = when (eventActionType) {
      CREATE -> PRISONER_RESTRICTION_CREATED_EVENT
      UPDATE -> PRISONER_RESTRICTION_UPDATED_EVENT
      DELETE -> PRISONER_RESTRICTION_DELETED_EVENT
    }
  }

  constructor(
    prisonerNumber: String,
    prisonerRestrictionDetails: PrisonerRestrictionDetails,
    eventActionType: EventActionType,
    eventSource: Source,
    eventUser: User,
  ) : this(prisonerNumber, PrisonerRestrictionCustomProperties(prisonerRestrictionDetails), eventActionType, eventSource, eventUser)

  constructor(
    prisonerNumber: String,
    syncPrisonerRestriction: SyncPrisonerRestriction,
    eventActionType: EventActionType,
    eventSource: Source,
    eventUser: User,
  ) : this(prisonerNumber, PrisonerRestrictionCustomProperties(syncPrisonerRestriction), eventActionType, eventSource, eventUser)

  constructor(
    prisonerNumber: String,
    prisonerRestrictionId: Long,
    eventActionType: EventActionType,
    eventSource: Source,
    eventUser: User,
  ) : this(prisonerNumber, PrisonerRestrictionCustomProperties(prisonerRestrictionId, null), eventActionType, eventSource, eventUser)
}

internal class PrisonerRestrictionCustomProperties(
  val prisonerRestrictionId: Long,
  val restrictionType: String?,
) {
  constructor(prisonerRestrictionDetails: PrisonerRestrictionDetails) : this(prisonerRestrictionDetails.prisonerRestrictionId, prisonerRestrictionDetails.restrictionType)
  constructor(syncPrisonerRestriction: SyncPrisonerRestriction) : this(syncPrisonerRestriction.prisonerRestrictionId, syncPrisonerRestriction.restrictionType)
}
