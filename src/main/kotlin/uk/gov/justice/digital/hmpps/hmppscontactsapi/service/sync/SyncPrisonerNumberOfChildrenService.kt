package uk.gov.justice.digital.hmpps.hmppscontactsapi.service.sync

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.PrisonerNumberOfChildren
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync.SyncUpdatePrisonerNumberOfChildrenRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.sync.Status
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.sync.SyncPrisonerNumberOfChildrenData
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.sync.SyncPrisonerNumberOfChildrenResponse
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.PrisonerNumberOfChildrenRepository

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
  ): SyncPrisonerNumberOfChildrenData {
    // Find existing numberOfChildren
    val existingCount = getPrisonerNumberOfChildrenActive(prisonerNumber)

    // If an existing number of children value is found and differs from the request:
    //   - Deactivate the existing number of children value and create a new one
    // If no existing number of children value is found:
    //   - Create a new existing number of children value
    // If existing number of children value matches the request:
    //   - Return the existing status unchanged

    if (existingCount != null) {
      if (existingCount.numberOfChildren == request.numberOfChildren) {
        return SyncPrisonerNumberOfChildrenData(from(existingCount), Status.UNCHANGED)
      }

      val deactivatedCount = existingCount.copy(
        active = false,
      )
      numberOfChildrenRepository.save(deactivatedCount)
    }

    // Create new active numberOfChildren
    val newNumberOfChildren = PrisonerNumberOfChildren(
      prisonerNumber = prisonerNumber,
      numberOfChildren = request.numberOfChildren,
      createdBy = request.createdBy,
      createdTime = request.createdTime,
      active = true,
    )
    val saved = newNumberOfChildren.let { numberOfChildrenRepository.save(it) }
      ?: throw IllegalArgumentException("Cannot save number of children for prisoner")

    if (existingCount != null) {
      return SyncPrisonerNumberOfChildrenData(from(saved), Status.UPDATED, updatedId = existingCount.prisonerNumberOfChildrenId)
    }
    return SyncPrisonerNumberOfChildrenData(from(saved), Status.CREATED)
  }

  fun getPrisonerNumberOfChildrenActive(prisonerNumber: String) = numberOfChildrenRepository.findByPrisonerNumberAndActiveTrue(
    prisonerNumber,
  )
}
