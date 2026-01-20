package uk.gov.justice.digital.hmpps.personalrelationships.facade.sync

import org.springframework.data.domain.Pageable
import org.springframework.data.web.PagedModel
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.personalrelationships.config.User
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.sync.MergePrisonerRestrictionsRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.sync.ResetPrisonerRestrictionsRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.ChangedRestrictionsResponse
import uk.gov.justice.digital.hmpps.personalrelationships.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.personalrelationships.service.events.OutboundEventsService
import uk.gov.justice.digital.hmpps.personalrelationships.service.events.Source
import uk.gov.justice.digital.hmpps.personalrelationships.service.sync.PrisonerRestrictionsAdminService

@Service
class PrisonerRestrictionsAdminFacade(
  private val restrictionsAdminService: PrisonerRestrictionsAdminService,
  private val outboundEventsService: OutboundEventsService,
) {

  fun merge(request: MergePrisonerRestrictionsRequest): ChangedRestrictionsResponse {
    val response = restrictionsAdminService.mergePrisonerRestrictions(request)

    if (response.hasChanged) {
      // Send CREATED events for new restrictions
      response.createdRestrictions.forEach { restrictionId ->
        outboundEventsService.send(
          outboundEvent = OutboundEvent.PRISONER_RESTRICTION_CREATED,
          identifier = restrictionId,
          source = Source.NOMIS,
          noms = request.keepingPrisonerNumber,
          user = User.SYS_USER,
        )
      }

      // Send DELETED events for removed restrictions
      response.deletedRestrictions.forEach { restrictionId ->
        outboundEventsService.send(
          outboundEvent = OutboundEvent.PRISONER_RESTRICTION_DELETED,
          identifier = restrictionId,
          source = Source.NOMIS,
          noms = request.removingPrisonerNumber,
          user = User.SYS_USER,
        )
      }
    }

    return response
  }

  fun reset(request: ResetPrisonerRestrictionsRequest): ChangedRestrictionsResponse {
    val response = restrictionsAdminService.resetPrisonerRestrictions(request)

    if (response.hasChanged) {
      // Send DELETED events for removed restrictions
      response.deletedRestrictions.forEach { restrictionId ->
        outboundEventsService.send(
          outboundEvent = OutboundEvent.PRISONER_RESTRICTION_DELETED,
          identifier = restrictionId,
          noms = request.prisonerNumber,
          source = Source.NOMIS,
          user = User.SYS_USER,
        )
      }

      // Send CREATED events for new restrictions
      response.createdRestrictions.forEach { restrictionId ->
        outboundEventsService.send(
          outboundEvent = OutboundEvent.PRISONER_RESTRICTION_CREATED,
          identifier = restrictionId,
          noms = request.prisonerNumber,
          source = Source.NOMIS,
          user = User.SYS_USER,
        )
      }
    }

    return response
  }

  fun getAllRestrictionIds(pageable: Pageable) = PagedModel(restrictionsAdminService.getAllRestrictionIds(pageable))
}
