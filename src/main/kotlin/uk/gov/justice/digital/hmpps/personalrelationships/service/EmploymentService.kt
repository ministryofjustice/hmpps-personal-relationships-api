package uk.gov.justice.digital.hmpps.personalrelationships.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.personalrelationships.client.organisationsapi.model.OrganisationSummary
import uk.gov.justice.digital.hmpps.personalrelationships.config.User
import uk.gov.justice.digital.hmpps.personalrelationships.entity.EmploymentEntity
import uk.gov.justice.digital.hmpps.personalrelationships.model.internal.PatchEmploymentResult
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.employment.PatchEmploymentsRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.employment.UpdateEmploymentRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.EmploymentDetails

@Service
class EmploymentService(
  private val organisationService: OrganisationService,
  private val transactionalEmploymentService: TransactionalEmploymentService,
) {

  fun patchEmployments(contactId: Long, request: PatchEmploymentsRequest, user: User): PatchEmploymentResult = transactionalEmploymentService.patchEmployments(contactId, request, user)

  fun getEmploymentDetails(contactId: Long): List<EmploymentDetails> {
    val employments = transactionalEmploymentService.getEmploymentEntities(contactId)
    val organisations = employments
      .map { it.organisationId }
      .distinct()
      .associateWith { organisationService.getOrganisationSummaryById(it) }

    return employments.map { employment ->
      createEmploymentDetails(employment, organisations.getValue(employment.organisationId))
    }
  }

  fun getEmployment(contactId: Long, employmentId: Long): EmploymentDetails {
    val employment = transactionalEmploymentService.getEmployment(contactId, employmentId)
    val org = organisationService.getOrganisationSummaryById(employment.organisationId)
    return createEmploymentDetails(employment, org)
  }

  fun createEmployment(contactId: Long, organisationId: Long, isActive: Boolean, createdBy: String): EmploymentDetails {
    transactionalEmploymentService.validateContactExists(contactId)
    val organisation = validateOrganisationExists(organisationId)
    val created = transactionalEmploymentService.createEmployment(
      contactId = contactId,
      organisationId = organisationId,
      isActive = isActive,
      createdBy = createdBy,
    )
    return createEmploymentDetails(created, organisation)
  }

  fun updateEmployment(contactId: Long, employmentId: Long, request: UpdateEmploymentRequest, user: User): EmploymentDetails {
    transactionalEmploymentService.validateContactExists(contactId)
    val organisation = validateOrganisationExists(request.organisationId)
    val updated = transactionalEmploymentService.updateEmployment(
      employmentId = employmentId,
      organisationId = request.organisationId,
      isActive = request.isActive,
      updatedBy = user.username,
    )
    return createEmploymentDetails(updated, organisation)
  }

  fun deleteEmployment(contactId: Long, employmentId: Long) = transactionalEmploymentService.deleteEmployment(contactId, employmentId)

  private fun validateOrganisationExists(organisationId: Long): OrganisationSummary = organisationService.getOrganisationSummaryById(organisationId)

  private fun createEmploymentDetails(
    employment: EmploymentEntity,
    org: OrganisationSummary,
  ) = EmploymentDetails(
    employmentId = employment.employmentId,
    contactId = employment.contactId,
    employer = org,
    isActive = employment.active,
    createdBy = employment.createdBy,
    createdTime = employment.createdTime,
    updatedBy = employment.updatedBy,
    updatedTime = employment.updatedTime,
  )
}
