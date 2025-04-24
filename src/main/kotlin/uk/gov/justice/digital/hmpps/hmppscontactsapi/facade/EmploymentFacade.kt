package uk.gov.justice.digital.hmpps.hmppscontactsapi.facade

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppscontactsapi.config.User
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.employment.CreateEmploymentRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.employment.PatchEmploymentsRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.employment.UpdateEmploymentRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.EmploymentDetails
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.EmploymentService
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.OutboundEventsService
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.Source

@Service
class EmploymentFacade(
  private val employmentService: EmploymentService,
  private val outboundEventsService: OutboundEventsService,
) {

  fun patchEmployments(contactId: Long, request: PatchEmploymentsRequest, user: User): List<EmploymentDetails> = employmentService.patchEmployments(contactId, request, user).also { result ->
    result.createdIds.onEach { outboundEventsService.send(OutboundEvent.EMPLOYMENT_CREATED, it, contactId = contactId, source = Source.DPS, user = user) }
    result.updatedIds.onEach { outboundEventsService.send(OutboundEvent.EMPLOYMENT_UPDATED, it, contactId = contactId, source = Source.DPS, user = user) }
    result.deletedIds.onEach { outboundEventsService.send(OutboundEvent.EMPLOYMENT_DELETED, it, contactId = contactId, source = Source.DPS, user = user) }
  }.employmentsAfterUpdate

  fun createEmployment(contactId: Long, request: CreateEmploymentRequest, user: User): EmploymentDetails = employmentService.createEmployment(
    contactId,
    request.organisationId,
    request.isActive,
    user.username,
  ).also { result ->
    outboundEventsService.send(OutboundEvent.EMPLOYMENT_CREATED, result.employmentId, contactId = contactId, source = Source.DPS, user = user)
  }

  fun updateEmployment(contactId: Long, employmentId: Long, request: UpdateEmploymentRequest, user: User): EmploymentDetails = employmentService.updateEmployment(contactId, employmentId, request, user).also {
    outboundEventsService.send(OutboundEvent.EMPLOYMENT_UPDATED, employmentId, contactId = contactId, source = Source.DPS, user = user)
  }

  fun deleteEmployment(contactId: Long, employmentId: Long, user: User) {
    employmentService.deleteEmployment(contactId, employmentId).also {
      outboundEventsService.send(OutboundEvent.EMPLOYMENT_DELETED, employmentId, contactId = contactId, source = Source.DPS, user = user)
    }
  }

  fun getEmployment(contactId: Long, employmentId: Long): EmploymentDetails = employmentService.getEmployment(contactId, employmentId)
}
