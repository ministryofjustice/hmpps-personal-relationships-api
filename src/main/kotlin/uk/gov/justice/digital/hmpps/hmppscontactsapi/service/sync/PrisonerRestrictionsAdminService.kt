package uk.gov.justice.digital.hmpps.hmppscontactsapi.service.sync

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.PrisonerRestriction
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync.ResetPrisonerRestrictionsRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.PrisonerRestrictionsRepository

@Service
@Transactional
class PrisonerRestrictionsAdminService(
  private val prisonerRestrictionsRepository: PrisonerRestrictionsRepository,
) {

  /**
   * Merges prisoner restriction records from the removing prisoner to the retaining prisoner.
   * Copies all restrictions from the removing prisoner to the retaining prisoner, generating new IDs.
   * Deletes all restrictions for the removing prisoner after copying.
   * Returns the created restriction's IDs, the removed restriction's IDs and whether any records were created.
   */
  fun mergePrisonerRestrictions(retainingPrisonerNumber: String, removingPrisonerNumber: String): MergeRestrictionsResponse {
    // Get all restrictions for removingPrisonerNumber
    val removingRestrictions = prisonerRestrictionsRepository.findByPrisonerNumber(removingPrisonerNumber)

    // If there are no restrictions to move, return default response
    if (removingRestrictions.isEmpty()) {
      return MergeRestrictionsResponse(wasUpdated = false)
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

    return MergeRestrictionsResponse(wasUpdated = true)
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
  fun resetPrisonerRestrictions(request: ResetPrisonerRestrictionsRequest): DeleteRestrictionsResponse {
    val restrictionsForPrisoner = prisonerRestrictionsRepository.findByPrisonerNumber(request.prisonerNumber)

    // If there are no restrictions to remove, throw an exception
    if (restrictionsForPrisoner.isEmpty()) {
      throw EntityNotFoundException("No restrictions found for prisoner ${request.prisonerNumber}")
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

    return DeleteRestrictionsResponse(wasDeleted = true, createdRestrictions, deletedRestrictions)
  }

  data class MergeRestrictionsResponse(
    val wasUpdated: Boolean,
  )

  data class DeleteRestrictionsResponse(
    val wasDeleted: Boolean,
    val createdRestrictions: List<Long> = emptyList(),
    val deletedRestrictions: List<Long> = emptyList(),
  )
}
