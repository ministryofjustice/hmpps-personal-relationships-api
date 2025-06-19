package uk.gov.justice.digital.hmpps.hmppscontactsapi.service.migrate

import jakarta.persistence.EntityNotFoundException
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.PrisonerRestriction
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.ReferenceCodeGroup
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.migrate.MigratePrisonerRestrictionsRequest
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
        effectiveDate = it.effectiveDate,
        expiryDate = it.expiryDate,
        commentText = it.commentText,
        authorisedUsername = it.authorisedUsername,
        createdBy = it.createdBy,
        createdTime = it.createdTime,
        updatedBy = it.updatedBy,
        updatedTime = it.updatedTime,
      )
    }

    // Save all new restrictions
    val savedEntities = prisonerRestrictionsRepository.saveAll(entitiesToSave)

    return PrisonerRestrictionsMigrationResponse(
      prisonerNumber = request.prisonerNumber,
      restrictionIds = savedEntities.map { it.prisonerRestrictionId },
    )
  }

  private fun validateReferenceDataExists(code: String) = referenceCodeRepository
    .findByGroupCodeAndCode(ReferenceCodeGroup.RESTRICTION, code)
    ?: throw EntityNotFoundException("No reference data found for groupCode: ReferenceCodeGroup.RESTRICTION and code: $code")
}
