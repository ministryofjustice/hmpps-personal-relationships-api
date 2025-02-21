package uk.gov.justice.digital.hmpps.hmppscontactsapi.service.sync

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.PrisonerNumberOfChildren
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync.SyncUpdatePrisonerNumberOfChildrenRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.sync.SyncPrisonerNumberOfChildrenResponse
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.PrisonerNumberOfChildrenRepository
import java.time.LocalDateTime

@Service
@Transactional
class SyncPrisonerNumberOfChildrenService(
  private val numberOfChildrenRepository: PrisonerNumberOfChildrenRepository,
) {
  companion object {
    const val NOT_FOUND_MESSAGE = "NumberOfChildren not found for prisoner: %s"

    fun from(numberOfChildren: PrisonerNumberOfChildren) = SyncPrisonerNumberOfChildrenResponse(
      id = numberOfChildren.prisonerNumberOfChildrenId,
    )
  }

  fun getNumberOfChildrenByPrisonerNumber(prisonerNumber: String): SyncPrisonerNumberOfChildrenResponse = numberOfChildrenRepository.findByPrisonerNumber(prisonerNumber)
    ?.let { from(it) }
    ?: throw EntityNotFoundException(String.format(NOT_FOUND_MESSAGE, prisonerNumber))

  @Transactional
  fun createOrUpdateNumberOfChildren(
    prisonerNumber: String,
    request: SyncUpdatePrisonerNumberOfChildrenRequest,
  ): SyncPrisonerNumberOfChildrenResponse {
    // Find existing numberOfChildren
    val existingNumberOfChildrenCount = numberOfChildrenRepository.findByPrisonerNumber(prisonerNumber)

    // If exists, deactivate it
    existingNumberOfChildrenCount?.let {
      val deactivatedNumberOfChildrenCount = it.copy(
        active = false,
      )
      numberOfChildrenRepository.save(deactivatedNumberOfChildrenCount)
    }

    // Create new active numberOfChildren
    val newNumberOfChildren = request.numberOfChildren?.let {
      PrisonerNumberOfChildren(
        prisonerNumber = prisonerNumber,
        numberOfChildren = it,
        createdBy = request.createdBy,
        createdTime = LocalDateTime.now(),
        active = true,
      )
    }
// todo check if this is right
    val saved = newNumberOfChildren?.let { numberOfChildrenRepository.save(it) }
      ?: throw IllegalArgumentException("Cannot save number of children for prisoner")
    return SyncPrisonerNumberOfChildrenResponse(saved.prisonerNumberOfChildrenId)
  }

  @Transactional
  fun deactivateNumberOfChildren(prisonerNumber: String): SyncPrisonerNumberOfChildrenResponse {
    val rowToDeactivate = numberOfChildrenRepository.findByPrisonerNumber(prisonerNumber)
      ?: throw EntityNotFoundException(String.format(NOT_FOUND_MESSAGE, prisonerNumber))

    // If exists, deactivate it
    rowToDeactivate.let {
      val deactivatedNumberOfChildrenCount = it.copy(
        active = false,
      )
      numberOfChildrenRepository.save(deactivatedNumberOfChildrenCount)
    }
    return SyncPrisonerNumberOfChildrenResponse(rowToDeactivate.prisonerNumberOfChildrenId)
  }
}
