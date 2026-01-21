package uk.gov.justice.digital.hmpps.personalrelationships.service

import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PagedModel
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.LinkedPrisonerDetails
import uk.gov.justice.digital.hmpps.personalrelationships.repository.PrisonerContactSummaryRepository

@Service
class LinkedPrisonersService(
  private val prisonerContactSummaryRepository: PrisonerContactSummaryRepository,
  private val prisonerService: PrisonerService,
) {

  companion object {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private const val MAX_IDS_FOR_PRISONER_SEARCH = 1000
  }

  fun getLinkedPrisoners(contactId: Long, page: Int, size: Int): PagedModel<LinkedPrisonerDetails> {
    val relationships = prisonerContactSummaryRepository.findByContactId(contactId)
    val prisoners = if (relationships.isNotEmpty()) {
      relationships.map { it.prisonerNumber }.toSet()
        .chunked(MAX_IDS_FOR_PRISONER_SEARCH)
        .flatMap { chunkOfPrisonerNumbers -> prisonerService.getPrisoners(chunkOfPrisonerNumbers.toSet()) }
    } else {
      emptyList()
    }

    val relationshipsWithPrisonerDetails = relationships.map { relationship ->
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
    }.sortedWith(compareBy({ it.lastName }, { it.firstName }, { it.middleNames }, { it.prisonerNumber }))

    val pageable = Pageable.ofSize(size).withPage(page)
    val totalSize = relationshipsWithPrisonerDetails.size
    val first = pageable.offset.toInt().coerceAtMost(totalSize)
    val last = (first + pageable.pageSize).coerceAtMost(totalSize)
    return PagedModel(PageImpl(relationshipsWithPrisonerDetails.subList(first, last), pageable, totalSize.toLong()))
  }
}
