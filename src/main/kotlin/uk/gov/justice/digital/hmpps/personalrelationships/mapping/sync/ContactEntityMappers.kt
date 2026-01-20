package uk.gov.justice.digital.hmpps.personalrelationships.mapping.sync

import org.springframework.data.domain.Page
import uk.gov.justice.digital.hmpps.personalrelationships.entity.ContactWithFixedIdEntity
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.sync.SyncCreateContactRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.sync.SyncContact
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.sync.SyncContactId

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

fun ContactWithFixedIdEntity.toModelIds(): SyncContactId = SyncContactId(contactId = this.contactId)

fun Page<ContactWithFixedIdEntity>.toModelIds(): Page<SyncContactId> = map { it.toModelIds() }
