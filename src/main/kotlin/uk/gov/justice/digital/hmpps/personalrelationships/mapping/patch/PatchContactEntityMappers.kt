package uk.gov.justice.digital.hmpps.personalrelationships.mapping.patch

import uk.gov.justice.digital.hmpps.personalrelationships.entity.ContactEntity
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.PatchContactResponse

fun ContactEntity.mapToResponse(): PatchContactResponse = PatchContactResponse(
  id = this.id(),
  titleCode = this.title,
  firstName = this.firstName,
  lastName = this.lastName,
  middleNames = this.middleNames,
  dateOfBirth = this.dateOfBirth,
  createdBy = this.createdBy,
  createdTime = this.createdTime,
  isStaff = this.staffFlag,
  deceasedDate = this.deceasedDate,
  genderCode = this.gender,
  domesticStatusCode = this.domesticStatus,
  languageCode = this.languageCode,
  interpreterRequired = this.interpreterRequired,
  updatedBy = this.updatedBy,
  updatedTime = this.updatedTime,
)
