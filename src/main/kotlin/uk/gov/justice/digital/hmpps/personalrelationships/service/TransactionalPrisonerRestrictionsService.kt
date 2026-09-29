package uk.gov.justice.digital.hmpps.personalrelationships.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.personalrelationships.entity.PrisonerRestrictionDetailsEntity
import uk.gov.justice.digital.hmpps.personalrelationships.repository.PrisonerRestrictionDetailsRepository

@Service
@Transactional(readOnly = true)
class TransactionalPrisonerRestrictionsService(
  private val prisonerRestrictionDetailsRepository: PrisonerRestrictionDetailsRepository,
) {
  fun getPrisonerRestrictions(prisonerNumber: String): List<PrisonerRestrictionDetailsEntity> = prisonerRestrictionDetailsRepository.findByPrisonerNumber(prisonerNumber)
}
