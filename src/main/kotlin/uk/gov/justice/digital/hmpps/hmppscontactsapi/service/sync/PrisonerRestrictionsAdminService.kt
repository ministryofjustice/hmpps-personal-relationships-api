package uk.gov.justice.digital.hmpps.hmppscontactsapi.service.sync

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.PrisonerRestriction
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.ReferenceCodeGroup
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.internal.ChangedRestrictionsResponse
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync.ResetPrisonerRestrictionsRequest
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
   * Copies all restrictions from the removing prisoner to the retaining prisoner, generating new IDs.
   * Deletes all restrictions for the removing prisoner after copying.
   * Returns the created restriction's IDs, the removed restriction's IDs and whether any records were created.
   */
  fun mergePrisonerRestrictions(retainingPrisonerNumber: String, removingPrisonerNumber: String): ChangedRestrictionsResponse {
    // Get all restrictions for removingPrisonerNumber
    val removingRestrictions = prisonerRestrictionsRepository.findByPrisonerNumber(removingPrisonerNumber)

    // If there are no restrictions to move, return default response
    if (removingRestrictions.isEmpty()) {
      return ChangedRestrictionsResponse(hasChanged = false)
    }

    // For each removing restriction, create a new restriction for retainingPrisonerNumber with the same details
    prisonerRestrictionsRepository.saveAllAndFlush(
      removingRestrictions.map { restriction ->
        restriction.copy(
          prisonerRestrictionId = 0, // Let JPA generate new ID
          prisonerNumber = retainingPrisonerNumber,
        )
      },
    )

    // Delete all restrictions for removingPrisonerNumber
    prisonerRestrictionsRepository.deleteByPrisonerNumber(removingPrisonerNumber)

    return ChangedRestrictionsResponse(hasChanged = true)
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
    prisonerRestrictionsRepository.saveAllAndFlush(newRestrictions).map { it.prisonerRestrictionId }
    return ChangedRestrictionsResponse(hasChanged = true)
  }

  private fun validateReferenceDataExists(code: String) {
    referenceCodeService.validateReferenceCode(ReferenceCodeGroup.RESTRICTION, code, allowInactive = true)
  }
}
