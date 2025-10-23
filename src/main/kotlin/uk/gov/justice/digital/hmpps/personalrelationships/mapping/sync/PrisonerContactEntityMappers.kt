package uk.gov.justice.digital.hmpps.personalrelationships.mapping.sync

import uk.gov.justice.digital.hmpps.personalrelationships.entity.PrisonerContactEntity
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.sync.SyncCreatePrisonerContactRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.sync.SyncPrisonerContact
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
  // When creating a prisoner contact via sync, approved by and approved time are set directly from the request
  // as NOMIS do not store these separately
  it.approvedBy = this.createdBy
  it.approvedTime = this.createdTime
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
