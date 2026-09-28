package uk.gov.justice.digital.hmpps.personalrelationships.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.personalrelationships.config.User
import uk.gov.justice.digital.hmpps.personalrelationships.entity.PrisonerNumberOfChildren
import uk.gov.justice.digital.hmpps.personalrelationships.mapping.toModel
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.CreateOrUpdatePrisonerNumberOfChildrenRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.PrisonerNumberOfChildrenResponse
import uk.gov.justice.digital.hmpps.personalrelationships.repository.PrisonerNumberOfChildrenRepository
import java.time.LocalDateTime

@Service
class TransactionalPrisonerNumberOfChildrenService(
  private val prisonerNumberOfChildrenRepository: PrisonerNumberOfChildrenRepository,
) {

  @Transactional
  fun createOrUpdateNumberOfChildren(
    prisonerNumber: String,
    prisonerNumberOfChildrenActive: PrisonerNumberOfChildren?,
    request: CreateOrUpdatePrisonerNumberOfChildrenRequest,
    user: User,
  ): PrisonerNumberOfChildrenResponse {
    // Find existing numberOfChildren, If exists, deactivate it
    prisonerNumberOfChildrenActive?.let {
      val deactivatedNumberOfChildrenCount = it.copy(
        active = false,
      )
      prisonerNumberOfChildrenRepository.save(deactivatedNumberOfChildrenCount)
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
    return prisonerNumberOfChildrenRepository.save(newNumberOfChildren).toModel()
  }
}
