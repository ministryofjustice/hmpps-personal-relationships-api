package uk.gov.justice.digital.hmpps.personalrelationships.mapping

import uk.gov.justice.digital.hmpps.personalrelationships.entity.ContactEmailEntity
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.ContactEmailDetails

fun ContactEmailEntity.toModel(): ContactEmailDetails = ContactEmailDetails(
  this.contactEmailId,
  this.contactId,
  this.emailAddress,
  this.createdBy,
  this.createdTime,
  this.updatedBy,
  this.updatedTime,
)
