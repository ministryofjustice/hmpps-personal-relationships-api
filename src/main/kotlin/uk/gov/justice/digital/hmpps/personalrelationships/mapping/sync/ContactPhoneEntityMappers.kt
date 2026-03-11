package uk.gov.justice.digital.hmpps.personalrelationships.mapping.sync

import uk.gov.justice.digital.hmpps.personalrelationships.entity.ContactPhoneEntity
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.sync.SyncCreateContactPhoneRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.sync.SyncContactPhone

fun ContactPhoneEntity.toModel(): SyncContactPhone = SyncContactPhone(
  contactPhoneId = this.contactPhoneId,
  contactId = this.contactId,
  phoneType = this.phoneType,
  phoneNumber = this.phoneNumber,
  extNumber = this.extNumber,
  createdBy = this.createdBy,
  createdTime = this.createdTime,
  updatedBy = this.updatedBy,
  updatedTime = this.updatedTime,
)

fun List<ContactPhoneEntity>.toModel() = map { it.toModel() }

fun SyncCreateContactPhoneRequest.toEntity() = ContactPhoneEntity(
  contactPhoneId = 0L,
  contactId = contactId,
  phoneType = phoneType,
  phoneNumber = phoneNumber,
  extNumber = extNumber,
  createdBy = createdBy,
  createdTime = createdTime,
)
