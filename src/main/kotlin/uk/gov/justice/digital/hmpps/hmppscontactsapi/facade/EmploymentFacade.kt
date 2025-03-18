package uk.gov.justice.digital.hmpps.hmppscontactsapi.facade

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.employment.CreateEmploymentRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.employment.PatchEmploymentsRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.employment.UpdateEmploymentRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.EmploymentDetails
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.OutboundEventsService
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.Source
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.sync.EmploymentService

@Service
class EmploymentFacade(
  private val employmentService: EmploymentService,
  private val outboundEventsService: OutboundEventsService,
) {

  fun patchEmployments(contactId: Long, request: PatchEmploymentsRequest): List<EmploymentDetails> = employmentService.patchEmployments(contactId, request).also { result ->
    result.createdIds.onEach { outboundEventsService.send(OutboundEvent.EMPLOYMENT_CREATED, it, contactId = contactId, source = Source.DPS) }
    result.updatedIds.onEach { outboundEventsService.send(OutboundEvent.EMPLOYMENT_UPDATED, it, contactId = contactId, source = Source.DPS) }
    result.deletedIds.onEach { outboundEventsService.send(OutboundEvent.EMPLOYMENT_DELETED, it, contactId = contactId, source = Source.DPS) }
  }.employmentsAfterUpdate

  fun createEmployment(contactId: Long, request: CreateEmploymentRequest): EmploymentDetails = employmentService.createEmployment(
    contactId,
    request.organisationId,
    request.isActive,
    request.createdBy,
  ).also { result ->
    outboundEventsService.send(OutboundEvent.EMPLOYMENT_CREATED, result.employmentId, contactId = contactId, source = Source.DPS)
  }

  fun updateEmployment(contactId: Long, employmentId: Long, request: UpdateEmploymentRequest): EmploymentDetails = employmentService.updateEmployment(contactId, employmentId, request).also {
    outboundEventsService.send(OutboundEvent.EMPLOYMENT_UPDATED, employmentId, contactId = contactId, source = Source.DPS)
  }

  fun deleteEmployment(contactId: Long, employmentId: Long) {
    employmentService.deleteEmployment(contactId, employmentId).also {
      outboundEventsService.send(OutboundEvent.EMPLOYMENT_DELETED, employmentId, contactId = contactId, source = Source.DPS)
    }
  }

  fun getEmployment(contactId: Long, employmentId: Long): EmploymentDetails = employmentService.getEmployment(contactId, employmentId)
}
