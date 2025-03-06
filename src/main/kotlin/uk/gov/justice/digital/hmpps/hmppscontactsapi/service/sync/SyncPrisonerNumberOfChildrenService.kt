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
    const val NOT_FOUND_MESSAGE = "Could not find the number of children for prisoner: %s"

    fun from(numberOfChildren: PrisonerNumberOfChildren) = SyncPrisonerNumberOfChildrenResponse(
      id = numberOfChildren.prisonerNumberOfChildrenId,
      numberOfChildren = numberOfChildren.numberOfChildren,
      createdBy = numberOfChildren.createdBy,
      createdTime = numberOfChildren.createdTime,
      active = numberOfChildren.active,
    )
  }

  fun getNumberOfChildrenByPrisonerNumber(prisonerNumber: String): SyncPrisonerNumberOfChildrenResponse = getPrisonerNumberOfChildrenActive(
    prisonerNumber,
  )
    ?.let { from(it) }
    ?: throw EntityNotFoundException(String.format(NOT_FOUND_MESSAGE, prisonerNumber))

  @Transactional
  fun createOrUpdateNumberOfChildren(
    prisonerNumber: String,
    request: SyncUpdatePrisonerNumberOfChildrenRequest,
  ): SyncPrisonerNumberOfChildrenResponse {
    // Find existing numberOfChildren
    val existingCount = getPrisonerNumberOfChildrenActive(prisonerNumber)

    // If exists, deactivate it
    existingCount?.let {
      val deactivatedCount = it.copy(
        active = false,
      )
      numberOfChildrenRepository.save(deactivatedCount)
    }

    // Create new active numberOfChildren
    val newNumberOfChildren = PrisonerNumberOfChildren(
      prisonerNumber = prisonerNumber,
      numberOfChildren = request.numberOfChildren,
      createdBy = request.createdBy,
      createdTime = LocalDateTime.now(),
      active = true,
    )
    val saved = newNumberOfChildren.let { numberOfChildrenRepository.save(it) }
      ?: throw IllegalArgumentException("Cannot save number of children for prisoner")
    return SyncPrisonerNumberOfChildrenResponse(
      id = saved.prisonerNumberOfChildrenId,
      numberOfChildren = saved.numberOfChildren,
      createdBy = saved.createdBy,
      createdTime = saved.createdTime,
      active = saved.active,
    )
  }

  fun getPrisonerNumberOfChildrenActive(prisonerNumber: String) = numberOfChildrenRepository.findByPrisonerNumberAndActive(prisonerNumber, true)
}
