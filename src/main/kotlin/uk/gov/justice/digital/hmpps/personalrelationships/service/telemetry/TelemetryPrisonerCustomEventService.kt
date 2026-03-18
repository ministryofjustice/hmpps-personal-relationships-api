package uk.gov.justice.digital.hmpps.personalrelationships.service.telemetry

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.personalrelationships.config.User
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.PrisonerDomesticStatusResponse
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.PrisonerNumberOfChildrenResponse
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.sync.SyncPrisonerDomesticStatusResponse
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.sync.SyncPrisonerNumberOfChildrenResponse
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.sync.SyncPrisonerRestriction
import uk.gov.justice.digital.hmpps.personalrelationships.model.telemetry.PrisonerDomesticStatusCustomEvent
import uk.gov.justice.digital.hmpps.personalrelationships.model.telemetry.PrisonerNumberOfChildrenCustomEvent
import uk.gov.justice.digital.hmpps.personalrelationships.model.telemetry.PrisonerRestrictionCustomEvent
import uk.gov.justice.digital.hmpps.personalrelationships.service.events.Source

@Service
class TelemetryPrisonerCustomEventService(private val telemetryService: TelemetryService) {
  fun trackCreatePrisonerDomesticStatusEvent(prisonerNumber: String, prisonerDomesticStatusResponse: PrisonerDomesticStatusResponse, source: Source, user: User) {
    val event = PrisonerDomesticStatusCustomEvent(prisonerNumber, prisonerDomesticStatusResponse, EventActionType.CREATE, source, user)
    telemetryService.track(event)
  }

  fun trackCreatePrisonerDomesticStatusEvent(prisonerNumber: String, syncPrisonerDomesticStatusResponse: SyncPrisonerDomesticStatusResponse, source: Source, user: User) {
    val event = PrisonerDomesticStatusCustomEvent(prisonerNumber, syncPrisonerDomesticStatusResponse, EventActionType.CREATE, source, user)
    telemetryService.track(event)
  }

  fun trackCreatePrisonerDomesticStatusEvent(prisonerNumber: String, prisonerDomesticStatusId: Long, source: Source, user: User) {
    val event = PrisonerDomesticStatusCustomEvent(prisonerNumber, prisonerDomesticStatusId, EventActionType.CREATE, source, user)
    telemetryService.track(event)
  }

  fun trackUpdatePrisonerDomesticStatusEvent(prisonerNumber: String, syncPrisonerDomesticStatusResponse: SyncPrisonerDomesticStatusResponse, source: Source, user: User) {
    val event = PrisonerDomesticStatusCustomEvent(prisonerNumber, syncPrisonerDomesticStatusResponse, EventActionType.UPDATE, source, user)
    telemetryService.track(event)
  }

  fun trackCreatePrisonerNumberOfChildrenEvent(prisonerNumber: String, prisonerNumberOfChildrenResponse: PrisonerNumberOfChildrenResponse, source: Source, user: User) {
    val event = PrisonerNumberOfChildrenCustomEvent(prisonerNumber, prisonerNumberOfChildrenResponse, EventActionType.CREATE, source, user)
    telemetryService.track(event)
  }

  fun trackCreatePrisonerNumberOfChildrenEvent(prisonerNumber: String, prisonerNumberOfChildrenId: Long, source: Source, user: User) {
    val event = PrisonerNumberOfChildrenCustomEvent(prisonerNumber, prisonerNumberOfChildrenId, EventActionType.CREATE, source, user)
    telemetryService.track(event)
  }

  fun trackCreatePrisonerNumberOfChildrenEvent(prisonerNumber: String, syncPrisonerNumberOfChildrenResponse: SyncPrisonerNumberOfChildrenResponse, source: Source, user: User) {
    val event = PrisonerNumberOfChildrenCustomEvent(prisonerNumber, syncPrisonerNumberOfChildrenResponse, EventActionType.CREATE, source, user)
    telemetryService.track(event)
  }

  fun trackUpdatePrisonerNumberOfChildrenEvent(prisonerNumber: String, syncPrisonerNumberOfChildrenResponse: SyncPrisonerNumberOfChildrenResponse, source: Source, user: User) {
    val event = PrisonerNumberOfChildrenCustomEvent(prisonerNumber, syncPrisonerNumberOfChildrenResponse, EventActionType.UPDATE, source, user)
    telemetryService.track(event)
  }

  fun trackCreatePrisonerRestrictionEvent(prisonerNumber: String, syncPrisonerRestriction: SyncPrisonerRestriction, source: Source, user: User) {
    val event = PrisonerRestrictionCustomEvent(prisonerNumber, syncPrisonerRestriction, EventActionType.CREATE, source, user)
    telemetryService.track(event)
  }

  fun trackCreatePrisonerRestrictionEvent(prisonerNumber: String, prisonerRestrictionId: Long, source: Source, user: User) {
    val event = PrisonerRestrictionCustomEvent(prisonerNumber, prisonerRestrictionId, EventActionType.CREATE, source, user)
    telemetryService.track(event)
  }

  fun trackUpdatePrisonerRestrictionEvent(prisonerNumber: String, syncPrisonerRestriction: SyncPrisonerRestriction, source: Source, user: User) {
    val event = PrisonerRestrictionCustomEvent(prisonerNumber, syncPrisonerRestriction, EventActionType.UPDATE, source, user)
    telemetryService.track(event)
  }

  fun trackDeletePrisonerRestrictionEvent(prisonerNumber: String, prisonerRestrictionId: Long, source: Source, user: User) {
    val event = PrisonerRestrictionCustomEvent(prisonerNumber, prisonerRestrictionId, EventActionType.DELETE, source, user)
    telemetryService.track(event)
  }
}
