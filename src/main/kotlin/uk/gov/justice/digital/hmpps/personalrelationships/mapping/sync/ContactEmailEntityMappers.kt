package uk.gov.justice.digital.hmpps.personalrelationships.mapping.sync

import uk.gov.justice.digital.hmpps.personalrelationships.entity.ContactEmailEntity
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.sync.SyncCreateContactEmailRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.sync.SyncContactEmail

fun ContactEmailEntity.toModel(): SyncContactEmail = SyncContactEmail(
  contactEmailId = this.contactEmailId,
  contactId = this.contactId,
  emailAddress = this.emailAddress,
  createdBy = this.createdBy,
  createdTime = this.createdTime,
  updatedBy = this.updatedBy,
  updatedTime = this.updatedTime,
)

fun List<ContactEmailEntity>.toModel() = map { it.toModel() }

fun SyncCreateContactEmailRequest.toEntity() = ContactEmailEntity(
  contactEmailId = 0L,
  contactId = contactId,
  emailAddress = emailAddress,
  createdBy = createdBy,
  createdTime = createdTime,
)
