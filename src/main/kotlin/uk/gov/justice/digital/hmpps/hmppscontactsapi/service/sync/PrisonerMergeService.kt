package uk.gov.justice.digital.hmpps.hmppscontactsapi.service.sync

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.PrisonerDomesticStatus
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.PrisonerNumberOfChildren
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.PrisonerDomesticStatusRepository
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.PrisonerNumberOfChildrenRepository

@Service
@Transactional
class PrisonerMergeService(
  private val numberOfChildrenRepository: PrisonerNumberOfChildrenRepository,
  private val prisonerDomesticStatusRepository: PrisonerDomesticStatusRepository,
) {

  /**
   * Merges the number of children records between two prisoner numbers.
   * Moves all inactive records from the removing prisoner to the retaining prisoner.
   * For active records, if the removing record is newer it becomes the active record,
   * otherwise it is moved as inactive.
   */
  fun mergeNumberOfChildren(retainingPrisonerNumber: String, removingPrisonerNumber: String): MergeResponse {
    val retainingActiveRecord = numberOfChildrenRepository.findByPrisonerNumberAndActiveTrue(retainingPrisonerNumber)
    val removingActiveRecord = numberOfChildrenRepository.findByPrisonerNumberAndActiveTrue(removingPrisonerNumber)

    // Return if either record doesn't exist
    if (retainingActiveRecord == null || removingActiveRecord == null) {
      return MergeResponse(id = 0, wasCreated = false)
    }

    val removingPrisonerHistoryRecords = numberOfChildrenRepository.findByPrisonerNumberAndActiveFalse(removingPrisonerNumber)

    // Move inactive records
    removingPrisonerHistoryRecords.forEach { record ->
      numberOfChildrenRepository.save(record.copy(prisonerNumber = retainingPrisonerNumber))
    }

    // Handle active records
    return when {
      removingActiveRecord.createdTime.isAfter(retainingActiveRecord.createdTime) -> {
        // If the removing record is newer than the retaining record:
        // 1. Set the retaining record as inactive
        // 2. Move the removing record to the new prisoner number and keep it active
        numberOfChildrenRepository.save(retainingActiveRecord.copy(active = false))
        val updatedRecord = numberOfChildrenRepository.save(removingActiveRecord.copy(prisonerNumber = retainingPrisonerNumber))
        // When moving the removing prisoner's active record to the new prisoner number,
        // set wasCreated=true so domain events will notify listeners of the new record
        updatedRecord.toResponse(wasCreated = true)
      }

      else -> {
        val updatedRecord = numberOfChildrenRepository.save(
          removingActiveRecord.copy(
            prisonerNumber = retainingPrisonerNumber,
            active = false,
          ),
        )
        // When moving the removing prisoner's active record as an inactive record in to the retaining prisoner's history,
        // set wasCreated=false so domain events won't notify listeners of the record
        updatedRecord.toResponse(wasCreated = false)
      }
    }
  }

  /**
   * Merges the domestic status records between two prisoner numbers.
   * Moves all inactive records from the removing prisoner to the retaining prisoner.
   * For active records, if the removing record is newer it becomes the active record,
   * otherwise it is moved as inactive.
   */
  fun mergeDomesticStatus(retainingPrisonerNumber: String, removingPrisonerNumber: String): MergeResponse {
    val retainingActiveRecord = prisonerDomesticStatusRepository.findByPrisonerNumberAndActiveTrue(retainingPrisonerNumber)
    val removingActiveRecord = prisonerDomesticStatusRepository.findByPrisonerNumberAndActiveTrue(removingPrisonerNumber)

    // Return if either record doesn't exist
    if (retainingActiveRecord == null || removingActiveRecord == null) {
      return MergeResponse(id = 0, wasCreated = false)
    }

    val removingPrisonerHistoryRecords = prisonerDomesticStatusRepository.findByPrisonerNumberAndActiveFalse(removingPrisonerNumber)

    // Move inactive records
    removingPrisonerHistoryRecords.forEach { record ->
      prisonerDomesticStatusRepository.save(record.copy(prisonerNumber = retainingPrisonerNumber))
    }

    // Handle active records
    return when {
      removingActiveRecord.createdTime.isAfter(retainingActiveRecord.createdTime) -> {
        // If the removing record is newer than the retaining record:
        // 1. Set the retaining record as inactive
        // 2. Move the removing record to the new prisoner number and keep it active
        prisonerDomesticStatusRepository.save(retainingActiveRecord.copy(active = false))
        val updatedRecord = prisonerDomesticStatusRepository.save(removingActiveRecord.copy(prisonerNumber = retainingPrisonerNumber))
        // When moving the removing prisoner's active record to the new prisoner number,
        // set wasCreated=true so domain events will notify listeners of the new record
        updatedRecord.toResponse(wasCreated = true)
      }

      else -> {
        val updatedRecord = prisonerDomesticStatusRepository.save(
          removingActiveRecord.copy(
            prisonerNumber = retainingPrisonerNumber,
            active = false,
          ),
        )
        // When moving the removing prisoner's active record as an inactive record in to the retaining prisoner's history,
        // set wasCreated=false so domain events won't notify listeners of the record
        updatedRecord.toResponse(wasCreated = false)
      }
    }
  }
}

private fun PrisonerNumberOfChildren.toResponse(wasCreated: Boolean): MergeResponse = MergeResponse(
  id = prisonerNumberOfChildrenId,
  wasCreated = wasCreated,
)

private fun PrisonerDomesticStatus.toResponse(wasCreated: Boolean): MergeResponse = MergeResponse(
  id = prisonerDomesticStatusId,
  wasCreated = wasCreated,
)

data class MergeResponse(
  val id: Long,
  val wasCreated: Boolean,
)
