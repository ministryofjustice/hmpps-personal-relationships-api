package uk.gov.justice.digital.hmpps.hmppscontactsapi.service

import jakarta.persistence.EntityNotFoundException
import org.springframework.data.web.PagedModel
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.PrisonerContactRestrictionCountsEntity
import uk.gov.justice.digital.hmpps.hmppscontactsapi.mapping.toModel
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.internal.PrisonerContactSearchParams
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.PrisonerContactSummary
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.RestrictionTypeDetails
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.RestrictionsSummary
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.RestrictionsSummary.Companion.NO_RESTRICTIONS
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.PrisonerContactRestrictionCountsRepository
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.PrisonerContactSearchRepository

@Service
class PrisonerContactService(
  private val prisonerContactSearchRepository: PrisonerContactSearchRepository,
  private val prisonerContactRestrictionCountsRepository: PrisonerContactRestrictionCountsRepository,
  private val prisonerService: PrisonerService,
) {
  fun getAllContacts(params: PrisonerContactSearchParams): PagedModel<PrisonerContactSummary> {
    prisonerService.getPrisoner(params.prisonerNumber)
      ?: throw EntityNotFoundException("Prisoner number ${params.prisonerNumber} - not found")
    val searchPrisonerContacts = prisonerContactSearchRepository.searchPrisonerContacts(params)
    val prisonerContactIds = searchPrisonerContacts.toList().map { it.prisonerContactId }.toSet()
    val restrictionsByPrisonerContactId =
      prisonerContactRestrictionCountsRepository.findAllByPrisonerContactIdIn(prisonerContactIds)
        .groupBy { it.prisonerContactId }
    return PagedModel(
      searchPrisonerContacts.map { prisonerContactSummaryEntity ->
        prisonerContactSummaryEntity.toModel(
          toRestrictionSummary(
            restrictionsByPrisonerContactId[prisonerContactSummaryEntity.prisonerContactId] ?: emptyList(),
          ),
        )
      },
    )
  }

  private fun toRestrictionSummary(restrictionCounts: List<PrisonerContactRestrictionCountsEntity>) = if (restrictionCounts.isEmpty()) {
    NO_RESTRICTIONS
  } else {
    RestrictionsSummary(
      restrictionCounts
        .filterNot { it.expired }
        .map { RestrictionTypeDetails(it.restrictionType, it.restrictionTypeDescription) }.toSet(),
      restrictionCounts
        .filterNot { it.expired }
        .sumOf { it.numberOfRestrictions },
      restrictionCounts
        .filter { it.expired }
        .sumOf { it.numberOfRestrictions },
    )
  }
}
