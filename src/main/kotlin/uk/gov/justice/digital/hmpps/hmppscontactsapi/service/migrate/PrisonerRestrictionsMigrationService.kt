package uk.gov.justice.digital.hmpps.hmppscontactsapi.service.migrate

import jakarta.persistence.EntityNotFoundException
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.PrisonerRestriction
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.ReferenceCodeGroup
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.migrate.MigratePrisonerRestrictionRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.migrate.MigratePrisonerRestrictionsRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.migrate.PrisonerRestrictionMigrationResponse
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.migrate.PrisonerRestrictionsMigrationResponse
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.PrisonerRestrictionsRepository
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.ReferenceCodeRepository

@Service
class PrisonerRestrictionsMigrationService(
  private val prisonerRestrictionsRepository: PrisonerRestrictionsRepository,
  private val referenceCodeRepository: ReferenceCodeRepository,
) {

  @Transactional
  fun migratePrisonerRestrictions(request: MigratePrisonerRestrictionsRequest): PrisonerRestrictionsMigrationResponse {
    // Remove existing records for this prisoner
    prisonerRestrictionsRepository.deleteByPrisonerNumber(request.prisonerNumber)

    // Validate and map incoming restrictions
    val entitiesToSave = request.restrictions.map {
      validateReferenceDataExists(it.restrictionType)
      PrisonerRestriction(
        prisonerRestrictionId = 0,
        prisonerNumber = request.prisonerNumber,
        restrictionType = it.restrictionType,
        startDate = it.effectiveDate,
        expiryDate = it.expiryDate,
        comments = it.commentText,
        authorisedUsername = it.authorisedUsername,
        currentTerm = it.currentTerm,
        createdBy = it.createdBy,
        createdTime = it.createdTime,
        updatedBy = it.updatedBy,
        updatedTime = it.updatedTime,
      )
    }

    // Save all new restrictions
    val savedEntities = prisonerRestrictionsRepository.saveAllAndFlush(entitiesToSave)

    return PrisonerRestrictionsMigrationResponse(
      prisonerNumber = request.prisonerNumber,
      prisonerRestrictionsIds = savedEntities.map { it.prisonerRestrictionId },
    )
  }

  private fun validateReferenceDataExists(code: String) = referenceCodeRepository
    .findByGroupCodeAndCode(ReferenceCodeGroup.RESTRICTION, code)
    ?: throw EntityNotFoundException("No reference data found for groupCode: ReferenceCodeGroup.RESTRICTION and code: $code")

  fun migratePrisonerRestriction(prisonerNumber: String, request: MigratePrisonerRestrictionRequest): PrisonerRestrictionMigrationResponse {
    // Validate reference data exists
    validateReferenceDataExists(request.restrictionType)

    // Create a new PrisonerRestriction entity
    val entity = PrisonerRestriction(
      prisonerRestrictionId = 0,
      prisonerNumber = prisonerNumber,
      restrictionType = request.restrictionType,
      startDate = request.effectiveDate,
      expiryDate = request.expiryDate,
      comments = request.commentText,
      authorisedUsername = request.authorisedUsername,
      currentTerm = request.currentTerm,
      createdBy = request.createdBy,
      createdTime = request.createdTime,
      updatedBy = request.updatedBy,
      updatedTime = request.updatedTime,
    )

    // Save the new restriction
    val savedEntity = prisonerRestrictionsRepository.saveAndFlush(entity)

    return PrisonerRestrictionMigrationResponse(
      prisonerRestrictionId = savedEntity.prisonerRestrictionId,
      prisonerNumber = savedEntity.prisonerNumber,
    )
  }
}
