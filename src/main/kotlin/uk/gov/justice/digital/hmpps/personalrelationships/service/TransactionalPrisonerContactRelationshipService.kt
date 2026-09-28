package uk.gov.justice.digital.hmpps.personalrelationships.service

import jakarta.persistence.EntityNotFoundException
import jakarta.validation.ValidationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.personalrelationships.entity.PrisonerContactSummaryEntity
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.PrisonerContactRelationshipsRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.PrisonerContactRelationship
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.PrisonerContactRelationshipsResponse
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.SummaryRelationship
import uk.gov.justice.digital.hmpps.personalrelationships.repository.PrisonerContactRepository
import uk.gov.justice.digital.hmpps.personalrelationships.repository.PrisonerContactSummaryRepository

@Service
@Transactional(readOnly = true)
class TransactionalPrisonerContactRelationshipService(
  private val prisonerContactSummaryRepository: PrisonerContactSummaryRepository,
  private val prisonerContactRepository: PrisonerContactRepository,
) {
  fun getById(prisonerContactId: Long): PrisonerContactSummaryEntity = prisonerContactSummaryRepository.findById(prisonerContactId)
    .orElseThrow { EntityNotFoundException("prisoner contact relationship with id $prisonerContactId not found") }

  /**
   *  Function to return summary relationships (type, approval, active) for a list
   *  of prisoner number and contact ID pairs. This is primarily used by the official
   *  visits service to check for issues with visitor approval since a visit was created.
   */
  fun getSummaryRelationships(request: PrisonerContactRelationshipsRequest): PrisonerContactRelationshipsResponse {
    if (request.identifiers.isEmpty()) {
      throw ValidationException("No identifiers were provided in the request")
    }

    // Make sure the requests are unique combinations of prisonerNumber and contactId
    val uniqueRequests = request.identifiers.distinctBy { listOf(it.prisonerNumber, it.contactId) }

    // Get the relationships for the pairs of prisonerNumber and contactId
    val prisonerRelationships = prisonerContactRepository.getCurrentRelationshipsForPrisoners(uniqueRequests.map { it.prisonerNumber })
    val prisonerRelationshipMap = prisonerRelationships.groupBy { it.prisonerNumber }

    // Build the response object
    val responses = uniqueRequests.map { outerItem ->
      val relationshipsForPrisonerAndContact = prisonerRelationshipMap[outerItem.prisonerNumber]?.filter { innerItem ->
        innerItem.contactId == outerItem.contactId
      } ?: emptyList()

      val relationships = relationshipsForPrisonerAndContact.map { rel ->
        SummaryRelationship(
          prisonerContactId = rel.prisonerContactId,
          relationshipTypeCode = rel.relationshipType,
          relationshipToPrisonerCode = rel.relationshipToPrisoner,
          isApprovedVisitor = rel.approvedVisitor,
          isRelationshipActive = rel.active,
          currentTerm = rel.currentTerm,
        )
      }

      PrisonerContactRelationship(
        prisonerNumber = outerItem.prisonerNumber,
        contactId = outerItem.contactId,
        relationships = relationships,
      )
    }

    return PrisonerContactRelationshipsResponse(responses)
  }
}
