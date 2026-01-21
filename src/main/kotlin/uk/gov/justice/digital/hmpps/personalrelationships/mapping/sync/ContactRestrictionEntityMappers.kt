package uk.gov.justice.digital.hmpps.personalrelationships.mapping.sync

import uk.gov.justice.digital.hmpps.personalrelationships.entity.ContactRestrictionEntity
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.sync.SyncCreateContactRestrictionRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.sync.SyncContactRestriction

fun ContactRestrictionEntity.toModel(): SyncContactRestriction = SyncContactRestriction(
  contactRestrictionId = this.contactRestrictionId,
  contactId = this.contactId!!,
  restrictionType = this.restrictionType,
  startDate = this.startDate,
  expiryDate = this.expiryDate,
  comments = this.comments,
  createdBy = this.createdBy,
  createdTime = this.createdTime,
  updatedBy = this.updatedBy,
  updatedTime = this.updatedTime,
)

fun List<ContactRestrictionEntity>.toModel() = map { it.toModel() }

fun SyncCreateContactRestrictionRequest.toEntity() = ContactRestrictionEntity(
  contactRestrictionId = 0L,
  contactId = contactId,
  restrictionType = restrictionType,
  startDate = startDate,
  expiryDate = expiryDate,
  comments = comments,
  createdBy = createdBy,
  createdTime = createdTime,
)
