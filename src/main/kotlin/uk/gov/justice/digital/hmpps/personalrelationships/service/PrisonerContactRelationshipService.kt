package uk.gov.justice.digital.hmpps.personalrelationships.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.personalrelationships.entity.PrisonerContactSummaryEntity
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.PrisonerContactRelationshipDetails

@Service
class PrisonerContactRelationshipService(
  private val manageUsersService: ManageUsersService,
  private val transactionalPrisonerContactRelationshipService: TransactionalPrisonerContactRelationshipService,
) {
  fun getById(prisonerContactId: Long): PrisonerContactRelationshipDetails = transactionalPrisonerContactRelationshipService.getById(prisonerContactId).toRelationshipModel()

  private fun PrisonerContactSummaryEntity.toRelationshipModel(): PrisonerContactRelationshipDetails = PrisonerContactRelationshipDetails(
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

  /**
   *  Function to return summary relationships (type, approval, active) for a list
   *  of prisoner number and contact ID pairs. This is primarily used by the official
   *  visits service to check for issues with visitor approval since a visit was created.
   */

  private fun getApprovedByUserName(approvedBy: String?): String? = approvedBy?.let { manageUsersService.getUserByUsername(it)?.name ?: it }
}
