package uk.gov.justice.digital.hmpps.personalrelationships.service

import org.springframework.data.web.PagedModel
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.personalrelationships.entity.PrisonerContactRestrictionCountsEntity
import uk.gov.justice.digital.hmpps.personalrelationships.mapping.toModel
import uk.gov.justice.digital.hmpps.personalrelationships.model.internal.PrisonerContactSearchParams
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.PrisonerContactRelationshipCount
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.PrisonerContactSummary
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.RestrictionTypeDetails
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.RestrictionsSummary
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.RestrictionsSummary.Companion.NO_RESTRICTIONS
import uk.gov.justice.digital.hmpps.personalrelationships.repository.PrisonerContactRelationshipCountRepository
import uk.gov.justice.digital.hmpps.personalrelationships.repository.PrisonerContactRestrictionCountsRepository
import uk.gov.justice.digital.hmpps.personalrelationships.repository.PrisonerContactSearchRepository
import uk.gov.justice.digital.hmpps.personalrelationships.repository.PrisonerContactSummaryRepository
import kotlin.jvm.optionals.getOrNull

@Service
class PrisonerContactService(
  private val prisonerContactSearchRepository: PrisonerContactSearchRepository,
  private val prisonerContactRestrictionCountsRepository: PrisonerContactRestrictionCountsRepository,
  private val prisonerService: PrisonerService,
  private val prisonerContactRelationshipCountRepository: PrisonerContactRelationshipCountRepository,
  private val prisonerContactSummaryRepository: PrisonerContactSummaryRepository,
) {
  fun getAllContacts(params: PrisonerContactSearchParams): PagedModel<PrisonerContactSummary> {
    val searchPrisonerContacts = prisonerContactSearchRepository.searchPrisonerContacts(params)
    if (searchPrisonerContacts.isEmpty) {
      // if the prisoner has contacts then they must exist
      prisonerService.checkPrisonerExists(params.prisonerNumber)
    }
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

  fun getAllSummariesForPrisonerAndContact(prisonerNumber: String, contactId: Long): List<PrisonerContactSummary> {
    val relationshipsBetweenPrisonerAndContact =
      prisonerContactSummaryRepository.findByPrisonerNumberAndContactId(prisonerNumber, contactId)
    if (relationshipsBetweenPrisonerAndContact.isEmpty()) {
      prisonerService.checkPrisonerExists(prisonerNumber)
    }
    val prisonerContactIds = relationshipsBetweenPrisonerAndContact.toList().map { it.prisonerContactId }.toSet()
    val restrictionsByPrisonerContactId =
      prisonerContactRestrictionCountsRepository.findAllByPrisonerContactIdIn(prisonerContactIds)
        .groupBy { it.prisonerContactId }
    return relationshipsBetweenPrisonerAndContact
      .map { prisonerContactSummaryEntity ->
        prisonerContactSummaryEntity.toModel(
          toRestrictionSummary(
            restrictionsByPrisonerContactId[prisonerContactSummaryEntity.prisonerContactId] ?: emptyList(),
          ),
        )
      }
  }

  fun countContactRelationships(prisonerNumber: String): PrisonerContactRelationshipCount = prisonerContactRelationshipCountRepository.findById(prisonerNumber)
    .getOrNull()
    ?.let { PrisonerContactRelationshipCount(it.social, it.official) }
    ?: PrisonerContactRelationshipCount(0, 0)

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
