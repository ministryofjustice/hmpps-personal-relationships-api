package uk.gov.justice.digital.hmpps.personalrelationships.service

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.personalrelationships.entity.PrisonerContactSummaryEntity
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.PrisonerContactRelationshipDetails
import uk.gov.justice.digital.hmpps.personalrelationships.repository.PrisonerContactSummaryRepository

@Service
class PrisonerContactRelationshipService(
  private val prisonerContactSummaryRepository: PrisonerContactSummaryRepository,
  private val manageUsersService: ManageUsersService,
) {

  fun getById(prisonerContactId: Long): PrisonerContactRelationshipDetails = prisonerContactSummaryRepository.findById(prisonerContactId)
    .orElseThrow { EntityNotFoundException("prisoner contact relationship with id $prisonerContactId not found") }.toRelationshipModel()

  fun PrisonerContactSummaryEntity.toRelationshipModel(): PrisonerContactRelationshipDetails = PrisonerContactRelationshipDetails(
    prisonerContactId = this.prisonerContactId,
    contactId = this.contactId,
    prisonerNumber = this.prisonerNumber,
    relationshipTypeCode = this.relationshipType,
    relationshipTypeDescription = this.relationshipTypeDescription,
    relationshipToPrisonerCode = this.relationshipToPrisoner,
    relationshipToPrisonerDescription = this.relationshipToPrisonerDescription ?: "",
    isNextOfKin = this.nextOfKin,
    isEmergencyContact = this.emergencyContact,
    isRelationshipActive = this.active,
    isApprovedVisitor = this.approvedVisitor,
    approvedBy = getApprovedByUserName(this.approvedBy),
    comments = this.comments,
  )

  fun getApprovedByUserName(approvedBy: String?): String? = approvedBy?.let { manageUsersService.getUserByUsername(it)?.name ?: it }
}
