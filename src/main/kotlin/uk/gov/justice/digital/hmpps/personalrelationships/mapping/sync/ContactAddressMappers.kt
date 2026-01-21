package uk.gov.justice.digital.hmpps.personalrelationships.mapping.sync

import uk.gov.justice.digital.hmpps.personalrelationships.entity.ContactAddressEntity
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.sync.SyncCreateContactAddressRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.sync.SyncContactAddress

fun ContactAddressEntity.toModel(): SyncContactAddress = SyncContactAddress(
  contactAddressId = this.contactAddressId,
  contactId = this.contactId!!,
  addressType = this.addressType,
  primaryAddress = this.primaryAddress,
  flat = this.flat,
  property = this.property,
  street = this.street,
  area = this.area,
  cityCode = this.cityCode,
  countyCode = this.countyCode,
  postcode = this.postCode,
  countryCode = this.countryCode,
  verified = this.verified,
  verifiedBy = this.verifiedBy,
  verifiedTime = this.verifiedTime,
  mailFlag = this.mailFlag,
  startDate = this.startDate,
  endDate = this.endDate,
  noFixedAddress = this.noFixedAddress,
  comments = this.comments,
  createdBy = this.createdBy,
  createdTime = this.createdTime,
  updatedBy = this.updatedBy,
  updatedTime = this.updatedTime,
)

fun List<ContactAddressEntity>.toModel() = map { it.toModel() }

fun SyncCreateContactAddressRequest.toEntity(): ContactAddressEntity = ContactAddressEntity(
  contactAddressId = 0L,
  contactId = this.contactId,
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
  createdTime = this.createdTime,
)
