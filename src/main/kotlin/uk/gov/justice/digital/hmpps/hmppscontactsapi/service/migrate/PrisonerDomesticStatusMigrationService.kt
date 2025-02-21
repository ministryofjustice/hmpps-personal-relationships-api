package uk.gov.justice.digital.hmpps.hmppscontactsapi.service.migrate

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.PrisonerDomesticStatus
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.ReferenceCodeGroup
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.migrate.MigratePrisonerDomesticStatusRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.migrate.PrisonerDomesticStatusMigrationResponse
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.PrisonerDomesticStatusRepository
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.ReferenceCodeRepository

@Service
class PrisonerDomesticStatusMigrationService(
  private val prisonerDomesticStatusRepository: PrisonerDomesticStatusRepository,
  private val referenceCodeRepository: ReferenceCodeRepository,
) {

  fun migrateDomesticStatus(request: MigratePrisonerDomesticStatusRequest): PrisonerDomesticStatusMigrationResponse {
    // Create list of entities to save
    val entitiesToSave = buildList {
      addAll(
        request.history.map {
          validateReferenceDataExists(it.domesticStatusCode)
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
        validateReferenceDataExists(it.domesticStatusCode)
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

  private fun validateReferenceDataExists(code: String) = referenceCodeRepository
    .findByGroupCodeAndCode(ReferenceCodeGroup.DOMESTIC_STS, code)
    ?: throw EntityNotFoundException("No reference data found for groupCode: ReferenceCodeGroup.DOMESTIC_STS and code: $code")
}
