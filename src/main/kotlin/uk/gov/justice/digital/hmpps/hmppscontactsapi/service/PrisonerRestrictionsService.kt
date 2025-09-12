package uk.gov.justice.digital.hmpps.hmppscontactsapi.service

import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PagedModel
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.PrisonerRestrictionDetailsEntity
import uk.gov.justice.digital.hmpps.hmppscontactsapi.mapping.sync.mapRestrictionsWithEnteredBy
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.PrisonerRestrictionDetails
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.PrisonerRestrictionDetailsRepository

@Service
@Transactional
class PrisonerRestrictionsService(
  private val prisonerRestrictionDetailsRepository: PrisonerRestrictionDetailsRepository,
  private val manageUsersService: ManageUsersService,
) {

  fun getPrisonerRestrictions(
    prisonerNumber: String,
    currentTermOnly: Boolean,
    pageable: Pageable,
  ): PagedModel<PrisonerRestrictionDetails> {
    val allRestrictions = getPrisonerRestrictions(prisonerNumber)
      .filter { !currentTermOnly || it.currentTerm }
    val page = PageImpl(
      allRestrictions.drop(pageable.offset.toInt()).take(pageable.pageSize),
      pageable,
      allRestrictions.size.toLong(),
    )
    return PagedModel(page)
  }

  fun getPrisonerRestrictions(prisonerNumber: String): List<PrisonerRestrictionDetails> {
    val restrictionsWithEnteredBy: Iterable<Pair<PrisonerRestrictionDetailsEntity, String>> = prisonerRestrictionDetailsRepository.findByPrisonerNumber(prisonerNumber)
      .map { entity -> entity to (entity.updatedBy ?: entity.createdBy) }
    val enteredByMap = restrictionsWithEnteredBy
      .map { (_, authorisedUsername) -> authorisedUsername }
      .toSet().associateWith { authorisedUsername ->
        manageUsersService.getUserByUsername(authorisedUsername)?.name ?: authorisedUsername
      }
    return restrictionsWithEnteredBy.mapRestrictionsWithEnteredBy(prisonerNumber, enteredByMap)
  }
}
