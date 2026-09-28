package uk.gov.justice.digital.hmpps.personalrelationships.service

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.personalrelationships.config.User
import uk.gov.justice.digital.hmpps.personalrelationships.mapping.toModel
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.CreateOrUpdatePrisonerNumberOfChildrenRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.PrisonerNumberOfChildrenResponse
import uk.gov.justice.digital.hmpps.personalrelationships.repository.PrisonerNumberOfChildrenRepository

@Service
class PrisonerNumberOfChildrenService(
  private val prisonerService: PrisonerService,
  private val prisonerNumberOfChildrenRepository: PrisonerNumberOfChildrenRepository,
  private val transactionalPrisonerNumberOfChildrenService: TransactionalPrisonerNumberOfChildrenService,
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
  fun createOrUpdateNumberOfChildren(
    prisonerNumber: String,
    request: CreateOrUpdatePrisonerNumberOfChildrenRequest,
    user: User,
  ): PrisonerNumberOfChildrenResponse {
    prisonerService.getPrisoner(prisonerNumber)
      ?: throw EntityNotFoundException("Prisoner number $prisonerNumber - not found")

    val prisonerNumberOfChildrenActive = prisonerNumberOfChildrenActive(prisonerNumber)

    return transactionalPrisonerNumberOfChildrenService.createOrUpdateNumberOfChildren(prisonerNumber, prisonerNumberOfChildrenActive, request, user)
  }

  private fun prisonerNumberOfChildrenActive(prisonerNumber: String) = prisonerNumberOfChildrenRepository.findByPrisonerNumberAndActiveTrue(
    prisonerNumber,
  )
}
