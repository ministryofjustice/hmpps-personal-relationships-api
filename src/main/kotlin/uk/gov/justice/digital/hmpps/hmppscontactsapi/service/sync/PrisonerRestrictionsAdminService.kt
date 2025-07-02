package uk.gov.justice.digital.hmpps.hmppscontactsapi.service.sync

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.PrisonerRestriction
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
   * Resets all restrictions for a given prisoner by removing them.
   * This is typically called when we want to reset a prisoner's restrictions to match NOMIS.
   *
   * @param prisonerNumber The prisoner number to reset restrictions for
   * @return DeleteRestrictionsResponse indicating if any restrictions were removed
   */
  fun resetPrisonerRestrictions(prisonerNumber: String): DeleteRestrictionsResponse {
    // Get all restrictions for the prisoner
    val restrictions = prisonerRestrictionsRepository.findByPrisonerNumber(prisonerNumber)

    // If there are no restrictions to remove, return early
    if (restrictions.isEmpty()) {
      return DeleteRestrictionsResponse(wasDeleted = false)
    }

    // Make a copy of the restrictions before deleting them
    val deletedRestrictions = restrictions.toList()

    // Delete all restrictions for the prisoner
    prisonerRestrictionsRepository.deleteAll(restrictions)

    return DeleteRestrictionsResponse(
      wasDeleted = true,
      deletedRestrictions = deletedRestrictions,
    )
  }
}

data class MergeRestrictionsResponse(
  val wasUpdated: Boolean,
)

data class DeleteRestrictionsResponse(
  val wasDeleted: Boolean,
  val deletedRestrictions: List<PrisonerRestriction> = emptyList(),
)
