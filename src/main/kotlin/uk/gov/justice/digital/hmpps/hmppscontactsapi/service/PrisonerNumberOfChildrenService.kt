package uk.gov.justice.digital.hmpps.hmppscontactsapi.service

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppscontactsapi.config.User
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.PrisonerNumberOfChildren
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.CreateOrUpdatePrisonerNumberOfChildrenRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.PrisonerNumberOfChildrenResponse
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.PrisonerNumberOfChildrenRepository
import java.time.LocalDateTime

@Service
class PrisonerNumberOfChildrenService(
  private val prisonerNumberOfChildrenRepository: PrisonerNumberOfChildrenRepository,
  private val prisonerService: PrisonerService,
) {
  fun getNumberOfChildren(prisonerNumber: String): PrisonerNumberOfChildrenResponse = prisonerNumberOfChildrenActive(prisonerNumber)
    ?.toModel()
    ?: throw EntityNotFoundException("No number of children found for prisoner number: $prisonerNumber")

  /**
   * Creates a new number of children record for a prisoner or updates an existing one.
   * If a record exists:
   * - The existing record is moved to history by setting it as inactive
   * - A new active record is created
   * If no record exists:
   * - A new active record is created
   */
  @Transactional
  fun createOrUpdateNumberOfChildren(
    prisonerNumber: String,
    request: CreateOrUpdatePrisonerNumberOfChildrenRequest,
    user: User,
  ): PrisonerNumberOfChildrenResponse {
    prisonerService.getPrisoner(prisonerNumber)
      ?: throw EntityNotFoundException("Prisoner number $prisonerNumber - not found")

    // Find existing numberOfChildren, If exists, deactivate it
    val prisonerNumberOfChildrenActive = prisonerNumberOfChildrenActive(prisonerNumber)
    prisonerNumberOfChildrenActive?.let {
      val deactivatedNumberOfChildrenCount = it.copy(
        active = false,
      )
      prisonerNumberOfChildrenRepository.saveAndFlush(deactivatedNumberOfChildrenCount)
    }

    // Create new active numberOfChildren
    val newNumberOfChildren = PrisonerNumberOfChildren(
      prisonerNumber = prisonerNumber,
      numberOfChildren = request.numberOfChildren?.toString(),
      createdBy = user.username,
      createdTime = LocalDateTime.now(),
      active = true,
    )
    // Save and return the new numberOfChildren
    return prisonerNumberOfChildrenRepository.saveAndFlush(newNumberOfChildren).toModel()
  }

  private fun prisonerNumberOfChildrenActive(prisonerNumber: String) = prisonerNumberOfChildrenRepository.findByPrisonerNumberAndActiveTrue(
    prisonerNumber,
  )

  private fun PrisonerNumberOfChildren.toModel(): PrisonerNumberOfChildrenResponse = PrisonerNumberOfChildrenResponse(
    id = prisonerNumberOfChildrenId,
    numberOfChildren = numberOfChildren,
    active = active,
    createdTime = createdTime,
    createdBy = createdBy,
  )
}
