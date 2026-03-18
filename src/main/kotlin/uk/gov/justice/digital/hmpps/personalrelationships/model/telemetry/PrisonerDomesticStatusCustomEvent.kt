package uk.gov.justice.digital.hmpps.personalrelationships.model.telemetry

import uk.gov.justice.digital.hmpps.personalrelationships.config.User
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.PrisonerDomesticStatusResponse
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.sync.SyncPrisonerDomesticStatusResponse
import uk.gov.justice.digital.hmpps.personalrelationships.service.events.Source
import uk.gov.justice.digital.hmpps.personalrelationships.service.telemetry.EventActionType
import uk.gov.justice.digital.hmpps.personalrelationships.service.telemetry.EventActionType.CREATE
import uk.gov.justice.digital.hmpps.personalrelationships.service.telemetry.EventActionType.DELETE
import uk.gov.justice.digital.hmpps.personalrelationships.service.telemetry.EventActionType.UPDATE
import uk.gov.justice.digital.hmpps.personalrelationships.service.telemetry.TelemetryCustomEventType
import uk.gov.justice.digital.hmpps.personalrelationships.service.telemetry.TelemetryCustomEventType.PRISONER_DOMESTIC_STATUS_CREATED_EVENT
import uk.gov.justice.digital.hmpps.personalrelationships.service.telemetry.TelemetryCustomEventType.PRISONER_DOMESTIC_STATUS_UPDATED_EVENT

class PrisonerDomesticStatusCustomEvent private constructor(
  override val prisonerNumber: String,
  private val prisonerDomesticStatusCustomProperties: PrisonerDomesticStatusCustomProperties,
  val eventActionType: EventActionType,
  val eventSource: Source,
  val eventUser: User,
) : PrisonerCustomTelemetryEvent(prisonerNumber, telemetryCustomEventType = getEvent(eventActionType), source = eventSource.name, user = eventUser) {
  override fun customProperties(): Map<String, String> = emptyMap()

  companion object {
    fun getEvent(eventActionType: EventActionType): TelemetryCustomEventType = when (eventActionType) {
      CREATE -> PRISONER_DOMESTIC_STATUS_CREATED_EVENT
      UPDATE -> PRISONER_DOMESTIC_STATUS_UPDATED_EVENT
      DELETE -> throw UnsupportedOperationException("Delete not supported for PrisonerDomesticStatus")
    }
  }

  constructor(
    prisonerNumber: String,
    prisonerDomesticStatusResponse: PrisonerDomesticStatusResponse,
    eventActionType: EventActionType,
    eventSource: Source,
    eventUser: User,
  ) : this(prisonerNumber, PrisonerDomesticStatusCustomProperties(prisonerDomesticStatusResponse), eventActionType, eventSource, eventUser)

  constructor(
    prisonerNumber: String,
    syncPrisonerDomesticStatusResponse: SyncPrisonerDomesticStatusResponse,
    eventActionType: EventActionType,
    eventSource: Source,
    eventUser: User,
  ) : this(prisonerNumber, PrisonerDomesticStatusCustomProperties(syncPrisonerDomesticStatusResponse), eventActionType, eventSource, eventUser)

  constructor(
    prisonerNumber: String,
    prisonerDomesticStatusId: Long,
    eventActionType: EventActionType,
    eventSource: Source,
    eventUser: User,
  ) : this(prisonerNumber, PrisonerDomesticStatusCustomProperties(prisonerDomesticStatusId, null), eventActionType, eventSource, eventUser)
}

internal class PrisonerDomesticStatusCustomProperties(
  val prisonerDomesticStatusId: Long,
  val domesticStatusCode: String?,
) {
  constructor(syncPrisonerDomesticStatusResponse: SyncPrisonerDomesticStatusResponse) : this(syncPrisonerDomesticStatusResponse.id, domesticStatusCode = syncPrisonerDomesticStatusResponse.domesticStatusCode)
  constructor(prisonerDomesticStatusResponse: PrisonerDomesticStatusResponse) : this(prisonerDomesticStatusResponse.id, prisonerDomesticStatusResponse.domesticStatusCode)
}
