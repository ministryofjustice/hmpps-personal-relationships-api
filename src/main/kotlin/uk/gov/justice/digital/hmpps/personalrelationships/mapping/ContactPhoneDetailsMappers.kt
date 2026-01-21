package uk.gov.justice.digital.hmpps.personalrelationships.mapping

import uk.gov.justice.digital.hmpps.personalrelationships.entity.ContactPhoneDetailsEntity
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.ContactPhoneDetails

fun ContactPhoneDetailsEntity.toModel(): ContactPhoneDetails = ContactPhoneDetails(
  contactPhoneId = this.contactPhoneId,
  contactId = this.contactId,
  phoneType = this.phoneType,
  phoneTypeDescription = this.phoneTypeDescription,
  phoneNumber = this.phoneNumber,
  extNumber = this.extNumber,
  createdBy = this.createdBy,
  createdTime = this.createdTime,
  updatedBy = this.updatedBy,
  updatedTime = this.updatedTime,
)
