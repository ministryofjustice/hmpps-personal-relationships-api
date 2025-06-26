package uk.gov.justice.digital.hmpps.hmppscontactsapi.service.sync

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.PrisonerRestrictionsRepository

@Service
@Transactional
class PrisonerRestrictionsMergeService(
  private val prisonerRestrictionsRepository: PrisonerRestrictionsRepository,
) {

/**
   * Merges prisoner restriction records from the removing prisoner to the retaining prisoner.
   * Copies all restrictions from the removing prisoner to the retaining prisoner, generating new IDs.
   * Deletes all restrictions for the removing prisoner after copying.
   * Returns the created restriction's IDs , the removed restriction's IDs and whether any records were created.
   */
  fun mergePrisonerRestrictions(retainingPrisonerNumber: String, removingPrisonerNumber: String): MergeRestrictionsResponse {
    // Get all restrictions for removingPrisonerNumber
    val removingRestrictions = prisonerRestrictionsRepository.findByPrisonerNumber(removingPrisonerNumber)

    // If there are no restrictions to move, return default response
    if (removingRestrictions.isEmpty()) {
      return MergeRestrictionsResponse(listOf(), listOf(), wasCreated = false)
    }

    // For each removing restriction, create a new restriction for retainingPrisonerNumber with the same details
    val newRestrictions = prisonerRestrictionsRepository.saveAllAndFlush(
      removingRestrictions.map { restriction ->
        restriction.copy(
          prisonerRestrictionId = 0, // Let JPA generate new ID
          prisonerNumber = retainingPrisonerNumber,
        )
      },
    )

    // Delete all restrictions for removingPrisonerNumber
    prisonerRestrictionsRepository.deleteByPrisonerNumber(removingPrisonerNumber)

    // Return the created restriction's IDs, removed restriction IDs and wasCreated=true
    return MergeRestrictionsResponse(
      keepingPrisonerRestrictionIds = newRestrictions.map { it.prisonerRestrictionId },
      removingPrisonerRestrictionIds = removingRestrictions.map { it.prisonerRestrictionId },
      wasCreated = true,
    )
  }
}

data class MergeRestrictionsResponse(
  val keepingPrisonerRestrictionIds: List<Long>,
  val removingPrisonerRestrictionIds: List<Long>,
  val wasCreated: Boolean,
)
