package uk.gov.justice.digital.hmpps.personalrelationships.mapping

import uk.gov.justice.digital.hmpps.personalrelationships.entity.ContactIdentityDetailsEntity
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.ContactIdentityDetails

fun ContactIdentityDetailsEntity.toModel(): ContactIdentityDetails = ContactIdentityDetails(
  contactIdentityId = this.contactIdentityId,
  contactId = this.contactId,
  identityType = this.identityType,
  identityTypeDescription = this.identityTypeDescription,
  identityTypeIsActive = this.identityTypeIsActive,
  identityValue = this.identityValue,
  issuingAuthority = this.issuingAuthority,
  createdBy = this.createdBy,
  createdTime = this.createdTime,
  updatedBy = this.updatedBy,
  updatedTime = this.updatedTime,
)
