package uk.gov.justice.digital.hmpps.hmppscontactsapi.mapping

import org.springframework.data.domain.Page
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.ContactEntity
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.ContactWithAddressEntity
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.PrisonerContactEntity
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.ContactRelationship
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.CreateContactRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.ContactSearchResultItem
import java.time.LocalDate
import java.time.LocalDateTime

fun ContactWithAddressEntity.toModel() = ContactSearchResultItem(
  id = this.contactId,
  lastName = this.lastName,
  firstName = this.firstName,
  middleNames = this.middleNames,
  dateOfBirth = this.dateOfBirth,
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
  postCode = this.postCode,
  countryCode = this.countryCode,
  countryDescription = this.countryDescription,
  mailAddress = this.mailFlag,
  startDate = this.startDate,
  endDate = this.endDate,
  noFixedAddress = this.noFixedAddress,
  comments = this.comments,
)

fun Page<ContactWithAddressEntity>.toModel(): Page<ContactSearchResultItem> = map { it.toModel() }

fun ContactRelationship.toEntity(
  contactId: Long,
  createdBy: String,
) = newPrisonerContact(
  contactId,
  this.prisonerNumber,
  this.relationshipType,
  this.relationshipToPrisoner,
  this.isNextOfKin,
  this.isEmergencyContact,
  this.comments,
  createdBy,
)

fun CreateContactRequest.toModel() = newContact(
  title = this.title,
  lastName = this.lastName,
  firstName = this.firstName,
  middleNames = this.middleNames,
  dateOfBirth = this.dateOfBirth,
  createdBy = this.createdBy,
)

private fun newContact(
  title: String?,
  firstName: String,
  lastName: String,
  middleNames: String?,
  dateOfBirth: LocalDate?,
  createdBy: String,
): ContactEntity = ContactEntity(
  contactId = null,
  title = title,
  firstName = firstName,
  lastName = lastName,
  middleNames = middleNames,
  dateOfBirth = dateOfBirth,
  deceasedDate = null,
  createdBy = createdBy,
  createdTime = LocalDateTime.now(),
  staffFlag = false,
  remitterFlag = false,
  gender = null,
  domesticStatus = null,
  languageCode = null,
  interpreterRequired = false,
  updatedBy = null,
  updatedTime = null,
)

private fun newPrisonerContact(
  contactId: Long,
  prisonerNumber: String,
  relationshipType: String,
  relationshipToPrisoner: String,
  nextOfKin: Boolean,
  emergencyContact: Boolean,
  comments: String?,
  createdBy: String,
): PrisonerContactEntity = PrisonerContactEntity(
  0,
  contactId = contactId,
  prisonerNumber = prisonerNumber,
  relationshipType = relationshipType,
  relationshipToPrisoner = relationshipToPrisoner,
  nextOfKin = nextOfKin,
  emergencyContact = emergencyContact,
  comments = comments,
  createdBy = createdBy,
  createdTime = LocalDateTime.now(),
  active = true,
  approvedVisitor = false,
  currentTerm = true,
)
