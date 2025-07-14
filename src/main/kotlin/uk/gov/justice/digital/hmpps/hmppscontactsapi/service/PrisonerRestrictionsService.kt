package uk.gov.justice.digital.hmpps.hmppscontactsapi.service

import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PagedModel
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppscontactsapi.mapping.sync.mapEntityToResponse
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.PrisonerRestrictionDetails
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.PrisonerRestrictionsRepository

@Service
@Transactional
class PrisonerRestrictionsService(
  private val prisonerRestrictionsRepository: PrisonerRestrictionsRepository,
) {

  fun getPrisonerRestrictions(
    prisonerNumber: String,
    currentTermOnly: Boolean,
    pageable: Pageable,
  ): PagedModel<PrisonerRestrictionDetails> {
    val allRestrictions = prisonerRestrictionsRepository.findByPrisonerNumber(prisonerNumber)
      .filter { !currentTermOnly || it.currentTerm }
      .map { it.mapEntityToResponse() }
    val page = PageImpl(
      allRestrictions.drop(pageable.offset.toInt()).take(pageable.pageSize),
      pageable,
      allRestrictions.size.toLong(),
    )
    return PagedModel(page)
  }
}
