package uk.gov.justice.digital.hmpps.hmppscontactsapi.mapping

import org.springframework.data.domain.Page
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.ContactEntity
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.ContactWithAddressEntity
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.PrisonerContactEntity
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.ContactRelationship
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.CreateContactRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.ContactSearchResultItem
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
  postcode = this.postCode,
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
)

fun CreateContactRequest.toModel() = ContactEntity(
  contactId = null,
  title = this.titleCode,
  firstName = this.firstName,
  lastName = this.lastName,
  middleNames = this.middleNames,
  dateOfBirth = this.dateOfBirth,
  deceasedDate = null,
  createdBy = this.createdBy,
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
