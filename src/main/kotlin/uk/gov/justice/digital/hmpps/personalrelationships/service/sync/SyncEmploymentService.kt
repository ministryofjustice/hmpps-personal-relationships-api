package uk.gov.justice.digital.hmpps.personalrelationships.service.sync

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.personalrelationships.mapping.sync.toEntity
import uk.gov.justice.digital.hmpps.personalrelationships.mapping.sync.toResponse
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.sync.SyncCreateEmploymentRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.sync.SyncUpdateEmploymentRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.sync.SyncEmployment
import uk.gov.justice.digital.hmpps.personalrelationships.repository.EmploymentRepository

@Service
@Transactional
class SyncEmploymentService(
  private val employmentRepository: EmploymentRepository,
) {

  @Transactional(readOnly = true)
  fun getEmploymentById(employmentId: Long): SyncEmployment {
    val employment = employmentRepository.findById(employmentId)
      .orElseThrow { EntityNotFoundException("Employment with ID $employmentId not found") }

    return employment.toResponse()
  }

  fun createEmployment(request: SyncCreateEmploymentRequest): SyncEmployment = employmentRepository
    .saveAndFlush(request.toEntity())
    .toResponse()

  fun updateEmployment(employmentId: Long, request: SyncUpdateEmploymentRequest): SyncEmployment {
    val employment = employmentRepository.findById(employmentId)
      .orElseThrow { EntityNotFoundException("Employment with ID $employmentId not found") }

    val changedEmployment = employment.copy(
      organisationId = request.organisationId,
      contactId = request.contactId,
      active = request.active,
      updatedBy = request.updatedBy,
      updatedTime = request.updatedTime,
    )

    return employmentRepository
      .saveAndFlush(changedEmployment)
      .toResponse()
  }

  fun deleteEmployment(employmentId: Long): SyncEmployment {
    val rowToDelete = employmentRepository.findById(employmentId)
      .orElseThrow { EntityNotFoundException("Employment with ID $employmentId not found") }

    employmentRepository.delete(rowToDelete)
    return rowToDelete.toResponse()
  }
}
