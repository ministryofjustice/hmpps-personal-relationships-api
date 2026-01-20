package uk.gov.justice.digital.hmpps.personalrelationships.service.migrate

import jakarta.persistence.EntityNotFoundException
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.personalrelationships.entity.PrisonerDomesticStatus
import uk.gov.justice.digital.hmpps.personalrelationships.model.ReferenceCodeGroup
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.migrate.MigratePrisonerDomesticStatusRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.migrate.PrisonerDomesticStatusMigrationResponse
import uk.gov.justice.digital.hmpps.personalrelationships.repository.PrisonerDomesticStatusRepository
import uk.gov.justice.digital.hmpps.personalrelationships.repository.ReferenceCodeRepository

@Service
class PrisonerDomesticStatusMigrationService(
  private val prisonerDomesticStatusRepository: PrisonerDomesticStatusRepository,
  private val referenceCodeRepository: ReferenceCodeRepository,
) {

  @Transactional
  fun migrateDomesticStatus(request: MigratePrisonerDomesticStatusRequest): PrisonerDomesticStatusMigrationResponse {
    // Create list of entities to save
    removeExistingRecords(request)
    val entitiesToSave = buildList {
      addAll(
        request.history.map {
          it.domesticStatusCode?.let { it1 -> validateReferenceDataExists(it1) }
          PrisonerDomesticStatus(
            prisonerNumber = request.prisonerNumber,
            domesticStatusCode = it.domesticStatusCode,
            createdBy = it.createdBy,
            createdTime = it.createdTime,
            active = false,
          )
        },
      )

      // Add current status if provided
      request.current?.let {
        it.domesticStatusCode?.let { it1 -> validateReferenceDataExists(it1) }
        add(
          PrisonerDomesticStatus(
            prisonerNumber = request.prisonerNumber,
            domesticStatusCode = it.domesticStatusCode,
            createdBy = it.createdBy,
            createdTime = it.createdTime,
            active = true,
          ),
        )
      }
    }

    // Batch save all entities
    val savedEntities = prisonerDomesticStatusRepository.saveAll(entitiesToSave)

    return PrisonerDomesticStatusMigrationResponse(
      prisonerNumber = request.prisonerNumber,
      current = savedEntities.find { it.active }?.prisonerDomesticStatusId,
      history = savedEntities.filter { !it.active }.map { it.prisonerDomesticStatusId },
    )
  }

  private fun removeExistingRecords(request: MigratePrisonerDomesticStatusRequest) {
    prisonerDomesticStatusRepository.deleteByPrisonerNumber(request.prisonerNumber)
  }

  private fun validateReferenceDataExists(code: String) = referenceCodeRepository
    .findByGroupCodeAndCode(ReferenceCodeGroup.DOMESTIC_STS, code)
    ?: throw EntityNotFoundException("No reference data found for groupCode: ReferenceCodeGroup.DOMESTIC_STS and code: $code")
}
