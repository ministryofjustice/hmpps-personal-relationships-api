package uk.gov.justice.digital.hmpps.hmppscontactsapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.LinkedPrisonerDetails
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.LinkedPrisonerRelationshipDetails
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.PrisonerContactSummaryRepository

@Service
class LinkedPrisonersService(
  private val prisonerContactSummaryRepository: PrisonerContactSummaryRepository,
  private val prisonerService: PrisonerService,
) {

  fun getLinkedPrisoners(contactId: Long): List<LinkedPrisonerDetails> {
    val relationshipsByPrisonerNumber =
      prisonerContactSummaryRepository.findByContactId(contactId).groupBy { it.prisonerNumber }
    val prisoners = if (relationshipsByPrisonerNumber.isNotEmpty()) prisonerService.getPrisoners(relationshipsByPrisonerNumber.keys) else emptyList()

    return relationshipsByPrisonerNumber
      .mapNotNull { (prisonerNumber, summaries) ->
        prisoners.find { it.prisonerNumber == prisonerNumber }
          ?.let { prisoner ->
            LinkedPrisonerDetails(
              prisonerNumber = prisonerNumber,
              lastName = prisoner.lastName,
              firstName = prisoner.firstName,
              middleNames = prisoner.middleNames,
              prisonId = prisoner.prisonId,
              prisonName = prisoner.prisonName,
              relationships = summaries.map { summary ->
                LinkedPrisonerRelationshipDetails(
                  prisonerContactId = summary.prisonerContactId,
                  relationshipTypeCode = summary.relationshipType,
                  relationshipTypeDescription = summary.relationshipTypeDescription,
                  relationshipToPrisonerCode = summary.relationshipToPrisoner,
                  relationshipToPrisonerDescription = summary.relationshipToPrisonerDescription,
                  isRelationshipActive = summary.active,
                )
              },
            )
          }
      }
  }
}
