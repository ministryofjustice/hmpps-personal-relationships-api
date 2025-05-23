package uk.gov.justice.digital.hmpps.hmppscontactsapi.mapping

import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.PrisonerContactSummaryEntity
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.PrisonerContactSummary
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.RestrictionsSummary

fun PrisonerContactSummaryEntity.toModel(restrictionSummary: RestrictionsSummary): PrisonerContactSummary = PrisonerContactSummary(
  prisonerContactId = this.prisonerContactId,
  contactId = this.contactId,
  prisonerNumber = this.prisonerNumber,
  titleCode = this.title,
  titleDescription = this.titleDescription,
  lastName = this.lastName,
  firstName = this.firstName,
  middleNames = this.middleNames,
  dateOfBirth = this.dateOfBirth,
  deceasedDate = this.deceasedDate,
  relationshipTypeCode = this.relationshipType,
  relationshipTypeDescription = this.relationshipTypeDescription,
  relationshipToPrisonerCode = this.relationshipToPrisoner,
  relationshipToPrisonerDescription = this.relationshipToPrisonerDescription ?: "",
  flat = this.flat,
  property = this.property,
  street = this.street,
  area = this.area,
  cityCode = this.cityCode,
  cityDescription = this.cityDescription,
  countyCode = this.countyCode,
  countyDescription = this.countyDescription,
  noFixedAddress = this.noFixedAddress,
  postcode = this.postCode,
  countryCode = this.countryCode,
  countryDescription = this.countryDescription,
  primaryAddress = this.primaryAddress,
  mailAddress = this.mailFlag,
  phoneType = this.phoneType,
  phoneTypeDescription = this.phoneTypeDescription,
  phoneNumber = this.phoneNumber,
  extNumber = this.extNumber,
  isApprovedVisitor = this.approvedVisitor,
  isNextOfKin = this.nextOfKin,
  isEmergencyContact = this.emergencyContact,
  isRelationshipActive = this.active,
  currentTerm = this.currentTerm,
  comments = this.comments,
  isStaff = this.staffFlag,
  restrictionSummary = restrictionSummary,
)
