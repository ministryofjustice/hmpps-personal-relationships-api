package uk.gov.justice.digital.hmpps.personalrelationships.model.telemetry

import uk.gov.justice.digital.hmpps.personalrelationships.config.User
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.EmploymentDetails
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.sync.SyncEmployment
import uk.gov.justice.digital.hmpps.personalrelationships.service.events.Source
import uk.gov.justice.digital.hmpps.personalrelationships.service.telemetry.EventActionType
import uk.gov.justice.digital.hmpps.personalrelationships.service.telemetry.EventActionType.CREATE
import uk.gov.justice.digital.hmpps.personalrelationships.service.telemetry.EventActionType.DELETE
import uk.gov.justice.digital.hmpps.personalrelationships.service.telemetry.EventActionType.UPDATE
import uk.gov.justice.digital.hmpps.personalrelationships.service.telemetry.TelemetryCustomEventType
import uk.gov.justice.digital.hmpps.personalrelationships.service.telemetry.TelemetryCustomEventType.CONTACT_EMPLOYMENT_CREATED_EVENT
import uk.gov.justice.digital.hmpps.personalrelationships.service.telemetry.TelemetryCustomEventType.CONTACT_EMPLOYMENT_DELETED_EVENT
import uk.gov.justice.digital.hmpps.personalrelationships.service.telemetry.TelemetryCustomEventType.CONTACT_EMPLOYMENT_UPDATED_EVENT

class ContactEmploymentCustomEvent private constructor(
  override val contactId: Long,
  private val contactEmploymentCustomProperties: ContactEmploymentCustomProperties?,
  val eventActionType: EventActionType,
  val eventSource: Source,
  val eventUser: User,
) : ContactCustomTelemetryEvent(contactId, getEvent(eventActionType), eventSource.name, eventUser) {
  override fun customProperties(): Map<String, String> = emptyMap()

  companion object {
    fun getEvent(eventActionType: EventActionType): TelemetryCustomEventType = when (eventActionType) {
      CREATE -> CONTACT_EMPLOYMENT_CREATED_EVENT
      UPDATE -> CONTACT_EMPLOYMENT_UPDATED_EVENT
      DELETE -> CONTACT_EMPLOYMENT_DELETED_EVENT
    }
  }

  constructor(
    contactId: Long,
    employmentDetails: EmploymentDetails,
    eventActionType: EventActionType,
    eventSource: Source,
    eventUser: User,
  ) : this(contactId, ContactEmploymentCustomProperties(employmentDetails), eventActionType, eventSource, eventUser)

  constructor(
    contactId: Long,
    syncEmployment: SyncEmployment,
    eventActionType: EventActionType,
    eventSource: Source,
    eventUser: User,
  ) : this(contactId, ContactEmploymentCustomProperties(syncEmployment), eventActionType, eventSource, eventUser)

  constructor(
    contactId: Long,
    employmentId: Long,
    eventActionType: EventActionType,
    eventSource: Source,
    eventUser: User,
  ) : this(contactId, ContactEmploymentCustomProperties(employmentId), eventActionType, eventSource, eventUser)
}

internal class ContactEmploymentCustomProperties(
  val contactEmploymentId: Long,
) {
  constructor(syncEmployment: SyncEmployment) : this(syncEmployment.employmentId)
  constructor(employmentDetails: EmploymentDetails) : this(employmentDetails.employmentId)
}
