package uk.gov.justice.digital.hmpps.hmppscontactsapi.mapping.sync

import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.ContactWithFixedIdEntity
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync.SyncCreateContactRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.sync.SyncContact

fun ContactWithFixedIdEntity.mapEntityToSyncResponse(): SyncContact = SyncContact(
  id = this.contactId,
  title = this.title,
  firstName = this.firstName,
  lastName = this.lastName,
  middleName = this.middleNames,
  dateOfBirth = this.dateOfBirth,
  createdBy = this.createdBy,
  createdTime = this.createdTime,
  isStaff = this.staffFlag,
  remitter = this.remitterFlag,
  deceasedDate = this.deceasedDate,
  gender = this.gender,
  domesticStatus = this.domesticStatus,
  languageCode = this.languageCode,
  interpreterRequired = this.interpreterRequired,
  updatedBy = this.updatedBy,
  updatedTime = this.updatedTime,
)

fun SyncCreateContactRequest.mapSyncRequestToEntity() = ContactWithFixedIdEntity(
  contactId = this.personId,
  title = this.title,
  firstName = this.firstName,
  lastName = this.lastName,
  middleNames = this.middleName,
  dateOfBirth = this.dateOfBirth,
  createdBy = this.createdBy,
  deceasedDate = this.deceasedDate,
  createdTime = this.createdTime,
  staffFlag = this.isStaff,
  remitterFlag = this.remitter,
  gender = this.gender,
  domesticStatus = this.domesticStatus,
  languageCode = this.languageCode,
  interpreterRequired = this.interpreterRequired ?: false,
  updatedBy = null,
  updatedTime = null,
)
