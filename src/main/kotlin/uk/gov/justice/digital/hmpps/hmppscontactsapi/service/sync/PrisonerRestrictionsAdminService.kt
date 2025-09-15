package uk.gov.justice.digital.hmpps.hmppscontactsapi.service.sync

import PrisonerRestrictionId
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.PrisonerRestriction
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.ReferenceCodeGroup
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync.MergePrisonerRestrictionsRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync.ResetPrisonerRestrictionsRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.ChangedRestrictionsResponse
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.PrisonerRestrictionsRepository
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.ReferenceCodeService

@Service
@Transactional
class PrisonerRestrictionsAdminService(
  private val prisonerRestrictionsRepository: PrisonerRestrictionsRepository,
  private val referenceCodeService: ReferenceCodeService,
) {

  /**
   * Merges prisoner restriction records from the removing prisoner to the retaining prisoner.
   * Deletes all restrictions for the removing prisoner and resets the restrictions for the retaining prisoner to those in the request.
   * Returns the created restriction's IDs, the removed restriction's IDs and whether any records were created.
   */
  fun mergePrisonerRestrictions(request: MergePrisonerRestrictionsRequest): ChangedRestrictionsResponse {
    // Delete all restrictions for removingPrisonerNumber
    val removingRestrictions = prisonerRestrictionsRepository.findByPrisonerNumber(request.removingPrisonerNumber)
    prisonerRestrictionsRepository.deleteAll(removingRestrictions)

    // Delete all restrictions for keepingPrisonerNumber
    val keepingRestrictions = prisonerRestrictionsRepository.findByPrisonerNumber(request.keepingPrisonerNumber)
    prisonerRestrictionsRepository.deleteAll(keepingRestrictions)

    // Validate reference data for each restriction
    request.restrictions.forEach { restriction ->
      validateReferenceDataExists(restriction.restrictionType)
    }

    // Save new restrictions for keepingPrisonerNumber
    val newRestrictions = request.restrictions.map { restriction ->
      PrisonerRestriction(
        prisonerRestrictionId = 0, // Let JPA generate new ID
        prisonerNumber = request.keepingPrisonerNumber,
        restrictionType = restriction.restrictionType,
        effectiveDate = restriction.effectiveDate,
        expiryDate = restriction.expiryDate,
        commentText = restriction.commentText,
        currentTerm = restriction.currentTerm,
        authorisedUsername = restriction.authorisedUsername,
        createdBy = restriction.createdBy,
        createdTime = restriction.createdTime,
        updatedBy = restriction.updatedBy,
        updatedTime = restriction.updatedTime,
      )
    }
    val createdRestrictions = prisonerRestrictionsRepository.saveAllAndFlush(newRestrictions).map { it.prisonerRestrictionId }
    val deletedRestrictions = (removingRestrictions + keepingRestrictions).map { it.prisonerRestrictionId }

    return ChangedRestrictionsResponse(
      createdRestrictions = createdRestrictions,
      deletedRestrictions = deletedRestrictions,
      hasChanged = createdRestrictions.isNotEmpty() || deletedRestrictions.isNotEmpty(),
    )
  }

  /**
   * Resets the restrictions for a specified prisoner by removing all existing restrictions
   * and adding new ones provided in the request. This function is typically used to align
   * a prisoner's restrictions with the records in NOMIS.
   *
   * @param request The request containing the prisoner number and the new set of restrictions.
   * @return DeleteRestrictionsResponse indicating whether any restrictions were removed and
   *         a list of the deleted restrictions if applicable.
   */
  fun resetPrisonerRestrictions(request: ResetPrisonerRestrictionsRequest): ChangedRestrictionsResponse {
    val restrictionsForPrisoner = prisonerRestrictionsRepository.findByPrisonerNumber(request.prisonerNumber)
    request.restrictions.forEach { restriction ->
      validateReferenceDataExists(restriction.restrictionType)
    }
    // Delete all restrictions for the prisoner
    prisonerRestrictionsRepository.deleteAll(restrictionsForPrisoner)

    // Save new restrictions from request
    val newRestrictions = request.restrictions.map { restriction ->
      PrisonerRestriction(
        prisonerRestrictionId = 0, // Let JPA generate new ID
        prisonerNumber = request.prisonerNumber,
        restrictionType = restriction.restrictionType,
        effectiveDate = restriction.effectiveDate,
        expiryDate = restriction.expiryDate,
        commentText = restriction.commentText,
        currentTerm = restriction.currentTerm,
        authorisedUsername = restriction.authorisedUsername,
        createdBy = restriction.createdBy,
        createdTime = restriction.createdTime,
        updatedBy = restriction.updatedBy,
        updatedTime = restriction.updatedTime,
      )
    }
    val createdRestrictions =
      prisonerRestrictionsRepository.saveAllAndFlush(newRestrictions).map { it.prisonerRestrictionId }
    val deletedRestrictions = restrictionsForPrisoner.map { it.prisonerRestrictionId }

    return ChangedRestrictionsResponse(hasChanged = true, createdRestrictions, deletedRestrictions)
  }

  private fun validateReferenceDataExists(code: String) {
    referenceCodeService.validateReferenceCode(ReferenceCodeGroup.RESTRICTION, code, allowInactive = true)
  }

  fun getAllRestrictionIds(pageable: Pageable): Page<PrisonerRestrictionId> = prisonerRestrictionsRepository.findAllBy(pageable).map { PrisonerRestrictionId(it.prisonerRestrictionId) }
}
