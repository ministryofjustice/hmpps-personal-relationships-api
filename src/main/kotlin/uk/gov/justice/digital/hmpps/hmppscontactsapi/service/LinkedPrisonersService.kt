package uk.gov.justice.digital.hmpps.hmppscontactsapi.service

import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.LinkedPrisonerDetails
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.PrisonerContactSummaryRepository

@Service
class LinkedPrisonersService(
  private val prisonerContactSummaryRepository: PrisonerContactSummaryRepository,
  private val prisonerService: PrisonerService,
) {

  companion object {
    private val logger = LoggerFactory.getLogger(this::class.java)
  }

  fun getLinkedPrisoners(contactId: Long, pageable: Pageable): Page<LinkedPrisonerDetails> {
    val relationships = prisonerContactSummaryRepository.findByContactId(contactId, pageable)
    val prisoners = if (relationships.content.isNotEmpty()) {
      prisonerService.getPrisoners(relationships.map { it.prisonerNumber }.toSet())
    } else {
      emptyList()
    }

    return relationships.map { relationship ->
      val prisoner = prisoners.find { it.prisonerNumber == relationship.prisonerNumber }
      if (prisoner == null) {
        logger.info("Couldn't find linked prisoner (${relationship.prisonerNumber}) for contact ($contactId)")
      }
      LinkedPrisonerDetails(
        prisonerNumber = relationship.prisonerNumber,
        lastName = prisoner?.lastName,
        firstName = prisoner?.firstName,
        middleNames = prisoner?.middleNames,
        prisonId = prisoner?.prisonId,
        prisonName = prisoner?.prisonName,
        prisonerContactId = relationship.prisonerContactId,
        relationshipTypeCode = relationship.relationshipType,
        relationshipTypeDescription = relationship.relationshipTypeDescription,
        relationshipToPrisonerCode = relationship.relationshipToPrisoner,
        relationshipToPrisonerDescription = relationship.relationshipToPrisonerDescription,
        isRelationshipActive = relationship.active,
      )
    }
  }
}
