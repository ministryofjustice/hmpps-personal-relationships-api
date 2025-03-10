package uk.gov.justice.digital.hmpps.hmppscontactsapi.mapping

import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.ContactAddressDetailsEntity
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.ContactAddressEntity
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.CreateContactAddressRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.ContactAddressDetails
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.ContactAddressPhoneDetails
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.ContactAddressResponse
import java.time.LocalDateTime

fun ContactAddressDetailsEntity.toModel(phoneNumbers: List<ContactAddressPhoneDetails>): ContactAddressDetails = ContactAddressDetails(
  contactAddressId = this.contactAddressId,
  contactId = this.contactId,
  addressType = this.addressType,
  addressTypeDescription = this.addressTypeDescription,
  primaryAddress = this.primaryAddress,
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
  verified = this.verified,
  verifiedBy = this.verifiedBy,
  verifiedTime = this.verifiedTime,
  mailFlag = this.mailFlag,
  startDate = this.startDate,
  endDate = this.endDate,
  noFixedAddress = this.noFixedAddress,
  comments = this.comments,
  phoneNumbers = phoneNumbers,
  createdBy = this.createdBy,
  createdTime = this.createdTime,
  updatedBy = this.updatedBy,
  updatedTime = this.updatedTime,
)

fun CreateContactAddressRequest.toEntity(contactId: Long): ContactAddressEntity = ContactAddressEntity(
  contactAddressId = 0L,
  contactId = contactId,
  addressType = this.addressType,
  primaryAddress = this.primaryAddress,
  flat = this.flat,
  property = this.property,
  street = this.street,
  area = this.area,
  cityCode = this.cityCode,
  countyCode = this.countyCode,
  postCode = this.postcode,
  countryCode = this.countryCode,
  verified = this.verified ?: false,
  mailFlag = this.mailFlag ?: false,
  startDate = this.startDate,
  endDate = this.endDate,
  noFixedAddress = this.noFixedAddress ?: false,
  comments = this.comments,
  createdBy = this.createdBy,
  createdTime = LocalDateTime.now(),
)

fun ContactAddressEntity.toModel(contactAddressPhoneId: List<Long> = emptyList()) = ContactAddressResponse(
  contactAddressId = this.contactAddressId,
  contactId = this.contactId!!,
  addressType = this.addressType,
  primaryAddress = this.primaryAddress,
  phoneNumberIds = contactAddressPhoneId,
  flat = this.flat,
  property = this.property,
  street = this.street,
  area = this.area,
  cityCode = this.cityCode,
  countyCode = this.countyCode,
  postcode = this.postCode,
  countryCode = this.countryCode,
  verified = this.verified,
  mailFlag = this.mailFlag,
  startDate = this.startDate,
  endDate = this.endDate,
  noFixedAddress = this.noFixedAddress,
  comments = this.comments,
  createdBy = this.createdBy,
  createdTime = this.createdTime,
  updatedBy = this.updatedBy,
  updatedTime = this.updatedTime,
  verifiedBy = this.verifiedBy,
  verifiedTime = this.verifiedTime,
)
