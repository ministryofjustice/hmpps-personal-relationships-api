package uk.gov.justice.digital.hmpps.personalrelationships.mapping

import uk.gov.justice.digital.hmpps.personalrelationships.config.User
import uk.gov.justice.digital.hmpps.personalrelationships.entity.ContactEntity
import uk.gov.justice.digital.hmpps.personalrelationships.entity.ContactWithAddressEntity
import uk.gov.justice.digital.hmpps.personalrelationships.entity.PrisonerContactEntity
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.ContactRelationship
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.CreateContactRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.ContactSearchResultItem
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.ExistingRelationshipToPrisoner
import java.time.LocalDateTime

fun ContactWithAddressEntity.toModel(existingRelationships: List<ExistingRelationshipToPrisoner>?) = ContactSearchResultItem(
  id = this.contactId,
  lastName = this.lastName,
  firstName = this.firstName,
  middleNames = this.middleNames,
  dateOfBirth = this.dateOfBirth,
  deceasedDate = this.deceasedDate,
  createdBy = this.createdBy,
  createdTime = this.createdTime,
  flat = this.flat,
  property = this.property,
  street = this.street,
  area = this.area,
  cityCode = this.cityCode,
  cityDescription = this.cityDescription,
  countyCode = this.countyCode,
  countyDescription = this.countyDescription,
  postcode = this.postCode,
  countryCode = this.countryCode,
  countryDescription = this.countryDescription,
  mailAddress = this.mailFlag,
  startDate = this.startDate,
  endDate = this.endDate,
  noFixedAddress = this.noFixedAddress,
  comments = this.comments,
  existingRelationships = existingRelationships,
)

fun ContactRelationship.toEntity(
  contactId: Long,
  createdBy: String,
) = PrisonerContactEntity(
  0,
  contactId = contactId,
  prisonerNumber = this.prisonerNumber,
  relationshipType = this.relationshipTypeCode,
  relationshipToPrisoner = this.relationshipToPrisonerCode,
  nextOfKin = this.isNextOfKin,
  emergencyContact = this.isEmergencyContact,
  comments = this.comments,
  createdBy = createdBy,
  createdTime = LocalDateTime.now(),
  active = true,
  approvedVisitor = this.isApprovedVisitor,
  currentTerm = true,
).also { contactEntity ->
  // Set approval details if approved visitor
  contactEntity.approvedBy = createdBy
  contactEntity.approvedTime = LocalDateTime.now()
}

fun CreateContactRequest.toModel(user: User) = ContactEntity(
  contactId = null,
  title = this.titleCode,
  firstName = this.firstName,
  lastName = this.lastName,
  middleNames = this.middleNames,
  dateOfBirth = this.dateOfBirth,
  deceasedDate = null,
  createdBy = user.username,
  createdTime = LocalDateTime.now(),
  staffFlag = this.isStaff,
  remitterFlag = false,
  gender = this.genderCode,
  domesticStatus = this.domesticStatusCode,
  languageCode = this.languageCode,
  interpreterRequired = this.interpreterRequired,
  updatedBy = null,
  updatedTime = null,
)
