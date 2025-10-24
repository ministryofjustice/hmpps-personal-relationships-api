package uk.gov.justice.digital.hmpps.hmppscontactsapi.mapping.sync

import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.PrisonerContactEntity
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync.SyncCreatePrisonerContactRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.sync.SyncPrisonerContact
import java.time.LocalDateTime

fun SyncCreatePrisonerContactRequest.toEntity(): PrisonerContactEntity = PrisonerContactEntity(
  prisonerContactId = 0L,
  contactId = this.contactId,
  prisonerNumber = this.prisonerNumber,
  relationshipType = this.contactType,
  relationshipToPrisoner = this.relationshipType,
  nextOfKin = this.nextOfKin,
  emergencyContact = this.emergencyContact,
  comments = this.comments,
  active = this.active ?: true,
  approvedVisitor = this.approvedVisitor ?: false,
  currentTerm = this.currentTerm ?: true,
  createdBy = this.createdBy,
  createdTime = this.createdTime,
).also {
  if (this.approvedVisitor == true) {
    // Set approved fields if approvedVisitor is true
    it.approvedBy = this.createdBy
    it.approvedTime = LocalDateTime.now()
  } else {
    it.approvedBy = null
    it.approvedTime = null
  }
  it.expiryDate = this.expiryDate
  it.createdAtPrison = this.createdAtPrison
}

fun PrisonerContactEntity.toResponse(): SyncPrisonerContact = SyncPrisonerContact(
  id = this.prisonerContactId,
  contactId = this.contactId,
  prisonerNumber = this.prisonerNumber,
  contactType = this.relationshipType,
  relationshipType = this.relationshipToPrisoner,
  nextOfKin = this.nextOfKin,
  emergencyContact = this.emergencyContact,
  comments = this.comments,
  createdBy = this.createdBy,
  createdTime = this.createdTime,
  active = this.active,
  approvedVisitor = this.approvedVisitor,
  currentTerm = this.currentTerm,
  expiryDate = this.expiryDate,
  updatedTime = this.updatedTime,
  updatedBy = this.updatedBy,
  createdAtPrison = this.createdAtPrison,
)
