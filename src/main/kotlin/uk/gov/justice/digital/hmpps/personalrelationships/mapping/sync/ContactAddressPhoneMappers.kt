package uk.gov.justice.digital.hmpps.personalrelationships.mapping.sync

import uk.gov.justice.digital.hmpps.personalrelationships.entity.ContactAddressPhoneEntity
import uk.gov.justice.digital.hmpps.personalrelationships.entity.ContactPhoneEntity
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.sync.SyncCreateContactAddressPhoneRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.sync.SyncContactAddressPhone

fun ContactAddressPhoneEntity.toModel(phoneEntity: ContactPhoneEntity) = SyncContactAddressPhone(
  contactAddressPhoneId = this.contactAddressPhoneId,
  contactAddressId = this.contactAddressId,
  contactPhoneId = this.contactPhoneId,
  contactId = this.contactId,
  phoneType = phoneEntity.phoneType,
  phoneNumber = phoneEntity.phoneNumber,
  extNumber = phoneEntity.extNumber,
  createdBy = this.createdBy,
  createdTime = this.createdTime,
  updatedBy = this.updatedBy,
  updatedTime = this.updatedTime,
)

fun SyncCreateContactAddressPhoneRequest.toEntity(phoneEntity: ContactPhoneEntity) = ContactAddressPhoneEntity(
  contactAddressPhoneId = 0L,
  contactAddressId = this.contactAddressId,
  contactPhoneId = phoneEntity.contactPhoneId,
  contactId = phoneEntity.contactId,
  createdBy = this.createdBy,
  createdTime = this.createdTime,
)
