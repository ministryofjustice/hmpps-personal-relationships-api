package uk.gov.justice.digital.hmpps.personalrelationships.model.telemetry

import uk.gov.justice.digital.hmpps.personalrelationships.config.User
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.PrisonerNumberOfChildrenResponse
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.sync.SyncPrisonerNumberOfChildrenResponse
import uk.gov.justice.digital.hmpps.personalrelationships.service.events.Source
import uk.gov.justice.digital.hmpps.personalrelationships.service.telemetry.EventActionType
import uk.gov.justice.digital.hmpps.personalrelationships.service.telemetry.EventActionType.CREATE
import uk.gov.justice.digital.hmpps.personalrelationships.service.telemetry.EventActionType.DELETE
import uk.gov.justice.digital.hmpps.personalrelationships.service.telemetry.EventActionType.UPDATE
import uk.gov.justice.digital.hmpps.personalrelationships.service.telemetry.TelemetryCustomEventType
import uk.gov.justice.digital.hmpps.personalrelationships.service.telemetry.TelemetryCustomEventType.PRISONER_NUMBER_OF_CHILDREN_CREATED_EVENT
import uk.gov.justice.digital.hmpps.personalrelationships.service.telemetry.TelemetryCustomEventType.PRISONER_NUMBER_OF_CHILDREN_UPDATED_EVENT

class PrisonerNumberOfChildrenCustomEvent private constructor(
  override val prisonerNumber: String,
  private val prisonerNumberOfChildrenCustomProperties: PrisonerNumberOfChildrenCustomProperties,
  val eventActionType: EventActionType,
  val eventSource: Source,
  val eventUser: User,
) : PrisonerCustomTelemetryEvent(prisonerNumber, telemetryCustomEventType = getEvent(eventActionType), source = eventSource.name, user = eventUser) {
  override fun customProperties(): Map<String, String> = emptyMap()

  companion object {
    fun getEvent(eventActionType: EventActionType): TelemetryCustomEventType = when (eventActionType) {
      CREATE -> PRISONER_NUMBER_OF_CHILDREN_CREATED_EVENT
      UPDATE -> PRISONER_NUMBER_OF_CHILDREN_UPDATED_EVENT
      DELETE -> throw UnsupportedOperationException("Delete not supported for PrisonerNumberOfChildren")
    }
  }

  constructor(
    prisonerNumber: String,
    prisonerNumberOfChildrenResponse: PrisonerNumberOfChildrenResponse,
    eventActionType: EventActionType,
    eventSource: Source,
    eventUser: User,
  ) : this(prisonerNumber, PrisonerNumberOfChildrenCustomProperties(prisonerNumberOfChildrenResponse), eventActionType, eventSource, eventUser)

  constructor(
    prisonerNumber: String,
    syncPrisonerNumberOfChildrenResponse: SyncPrisonerNumberOfChildrenResponse,
    eventActionType: EventActionType,
    eventSource: Source,
    eventUser: User,
  ) : this(prisonerNumber, PrisonerNumberOfChildrenCustomProperties(syncPrisonerNumberOfChildrenResponse), eventActionType, eventSource, eventUser)

  constructor(
    prisonerNumber: String,
    prisonerNumberOfChildrenId: Long,
    eventActionType: EventActionType,
    eventSource: Source,
    eventUser: User,
  ) : this(prisonerNumber, PrisonerNumberOfChildrenCustomProperties(prisonerNumberOfChildrenId, null), eventActionType, eventSource, eventUser)
}

internal class PrisonerNumberOfChildrenCustomProperties(
  val prisonerNumberOfChildrenId: Long,
  val numberOfChildren: String?,
) {
  constructor(syncPrisonerNumberOfChildrenResponse: SyncPrisonerNumberOfChildrenResponse) : this(syncPrisonerNumberOfChildrenResponse.id, syncPrisonerNumberOfChildrenResponse.numberOfChildren)
  constructor(prisonerNumberOfChildrenResponse: PrisonerNumberOfChildrenResponse) : this(prisonerNumberOfChildrenResponse.id, prisonerNumberOfChildrenResponse.numberOfChildren)
}
