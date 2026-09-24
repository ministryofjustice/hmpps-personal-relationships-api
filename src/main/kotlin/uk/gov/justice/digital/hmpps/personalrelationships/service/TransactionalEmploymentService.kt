package uk.gov.justice.digital.hmpps.personalrelationships.service

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.personalrelationships.client.organisationsapi.model.OrganisationSummary
import uk.gov.justice.digital.hmpps.personalrelationships.config.User
import uk.gov.justice.digital.hmpps.personalrelationships.entity.EmploymentEntity
import uk.gov.justice.digital.hmpps.personalrelationships.model.internal.PatchEmploymentResult
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.employment.PatchEmploymentsRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.EmploymentDetails
import uk.gov.justice.digital.hmpps.personalrelationships.repository.ContactRepository
import uk.gov.justice.digital.hmpps.personalrelationships.repository.EmploymentRepository
import java.time.LocalDateTime

@Service
@Transactional(readOnly = true)
class TransactionalEmploymentService(
  private val contactRepository: ContactRepository,
  private val employmentRepository: EmploymentRepository,
  private val organisationService: OrganisationService,
) {

  fun validateContactExists(contactId: Long) {
    contactRepository.findById(contactId)
      .orElseThrow { EntityNotFoundException("Contact ($contactId) not found") }
  }

  fun getEmploymentEntities(contactId: Long): List<EmploymentEntity> = employmentRepository.findByContactId(contactId)

  fun getEmployment(contactId: Long, employmentId: Long): EmploymentEntity {
    validateContactExists(contactId)
    return validateEmploymentExists(employmentId)
  }

  @Transactional
  fun patchEmployments(contactId: Long, request: PatchEmploymentsRequest, user: User): PatchEmploymentResult {
    validateContactExists(contactId)
    val createdIds = mutableListOf<Long>()
    val updatedIds = mutableListOf<Long>()
    val deletedIds = mutableListOf<Long>()
    val existingEmployments = employmentRepository.findByContactId(contactId)
    request.createEmployments.onEach { newEmployment ->
      val created = employmentRepository.saveAndFlush(
        EmploymentEntity(
          employmentId = 0,
          organisationId = newEmployment.organisationId,
          contactId = contactId,
          active = newEmployment.isActive,
          createdBy = user.username,
          createdTime = LocalDateTime.now(),
          updatedBy = null,
          updatedTime = null,
        ),
      )
      createdIds.add(created.employmentId)
    }
    request.updateEmployments.onEach { updatedEmployment ->
      val existingEmployment = existingEmployments.find { it.employmentId == updatedEmployment.employmentId }
        ?: throw EntityNotFoundException("Employment with id ${updatedEmployment.employmentId} not found")
      employmentRepository.saveAndFlush(
        existingEmployment.copy(
          organisationId = updatedEmployment.organisationId,
          active = updatedEmployment.isActive,
          updatedBy = user.username,
          updatedTime = LocalDateTime.now(),
        ),
      )
      updatedIds.add(updatedEmployment.employmentId)
    }
    request.deleteEmployments.onEach { deletedEmploymentId ->
      val existingEmployment = existingEmployments.find { it.employmentId == deletedEmploymentId }
        ?: throw EntityNotFoundException("Employment with id $deletedEmploymentId not found")
      employmentRepository.delete(existingEmployment)
      deletedIds.add(deletedEmploymentId)
    }
    return PatchEmploymentResult(
      createdIds = createdIds,
      updatedIds = updatedIds,
      deletedIds = deletedIds,
      employmentsAfterUpdate = getEmploymentDetails(contactId),
    )
  }

  @Transactional
  fun createEmployment(
    contactId: Long,
    organisationId: Long,
    isActive: Boolean,
    createdBy: String,
  ): EmploymentEntity = employmentRepository.saveAndFlush(
    EmploymentEntity(
      employmentId = 0,
      organisationId = organisationId,
      contactId = contactId,
      active = isActive,
      createdBy = createdBy,
      createdTime = LocalDateTime.now(),
      updatedBy = null,
      updatedTime = null,
    ),
  )

  @Transactional
  fun updateEmployment(
    employmentId: Long,
    organisationId: Long,
    isActive: Boolean,
    updatedBy: String,
  ): EmploymentEntity {
    val originalEntity = validateEmploymentExists(employmentId)
    return employmentRepository.saveAndFlush(
      originalEntity.copy(
        organisationId = organisationId,
        active = isActive,
        updatedBy = updatedBy,
        updatedTime = LocalDateTime.now(),
      ),
    )
  }

  @Transactional
  fun deleteEmployment(contactId: Long, employmentId: Long) {
    validateContactExists(contactId)
    employmentRepository.delete(validateEmploymentExists(employmentId))
  }

  private fun getEmploymentDetails(contactId: Long): List<EmploymentDetails> {
    val employments = employmentRepository.findByContactId(contactId)
    val organisations = employments
      .map { it.organisationId }
      .distinct()
      .associateWith { organisationService.getOrganisationSummaryById(it) }

    return employments.map { employment ->
      createEmploymentDetails(employment, organisations.getValue(employment.organisationId))
    }
  }

  private fun validateEmploymentExists(employmentId: Long): EmploymentEntity = employmentRepository.findById(employmentId)
    .orElseThrow { EntityNotFoundException("Employment ($employmentId) not found") }!!

  private fun createEmploymentDetails(
    employment: EmploymentEntity,
    organisation: OrganisationSummary,
  ) = EmploymentDetails(
    employmentId = employment.employmentId,
    contactId = employment.contactId,
    employer = organisation,
    isActive = employment.active,
    createdBy = employment.createdBy,
    createdTime = employment.createdTime,
    updatedBy = employment.updatedBy,
    updatedTime = employment.updatedTime,
  )
}
