package uk.gov.justice.digital.hmpps.hmppscontactsapi.service

import jakarta.persistence.EntityNotFoundException
import jakarta.transaction.Transactional
import jakarta.validation.ValidationException
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.ContactEntity
import uk.gov.justice.digital.hmpps.hmppscontactsapi.mapping.patch.mapToResponse
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.ReferenceCodeGroup
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.PatchContactRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.PatchContactResponse
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.ContactRepository
import java.time.LocalDateTime

@Service
class ContactPatchService(
  private val contactRepository: ContactRepository,
  private val referenceCodeService: ReferenceCodeService,
) {

  @Transactional
  fun patch(id: Long, request: PatchContactRequest): PatchContactResponse {
    val contact = contactRepository.findById(id)
      .orElseThrow { EntityNotFoundException("Contact not found") }

    validateLanguageCode(request)
    validateInterpreterRequiredType(request)
    validateDomesticStatusCode(request)
    validateStaffFlag(request)
    validateTitle(request)
    validateGender(request)

    val changedContact = contact.patchRequest(request)

    val savedContact = contactRepository.saveAndFlush(changedContact)
    return savedContact.mapToResponse()
  }

  private fun ContactEntity.patchRequest(
    request: PatchContactRequest,
  ): ContactEntity {
    val changedContact = this.copy(
      staffFlag = request.isStaff.orElse(this.staffFlag),
      domesticStatus = request.domesticStatusCode.orElse(this.domesticStatus),
      interpreterRequired = request.interpreterRequired.orElse(this.interpreterRequired),
      languageCode = request.languageCode.orElse(this.languageCode),
      dateOfBirth = request.dateOfBirth.orElse(this.dateOfBirth),
      title = request.titleCode.orElse(this.title),
      middleNames = request.middleNames.orElse(this.middleNames),
      gender = request.genderCode.orElse(this.gender),
      deceasedDate = request.deceasedDate.orElse(this.deceasedDate),
      updatedBy = request.updatedBy,
      updatedTime = LocalDateTime.now(),
    )

    return changedContact
  }

  private fun validateLanguageCode(request: PatchContactRequest) {
    if (request.languageCode.isPresent && request.languageCode.get() != null) {
      val code = request.languageCode.get()!!
      referenceCodeService.validateReferenceCode(ReferenceCodeGroup.LANGUAGE, code, allowInactive = true)
    }
  }

  private fun validateInterpreterRequiredType(request: PatchContactRequest) {
    if (request.interpreterRequired.isPresent && request.interpreterRequired.get() == null) {
      throw ValidationException("Unsupported interpreter required type null.")
    }
  }

  private fun validateStaffFlag(request: PatchContactRequest) {
    if (request.isStaff.isPresent && request.isStaff.get() == null) {
      throw ValidationException("Unsupported staff flag value null.")
    }
  }

  private fun validateDomesticStatusCode(request: PatchContactRequest) {
    if (request.domesticStatusCode.isPresent && request.domesticStatusCode.get() != null) {
      val code = request.domesticStatusCode.get()!!
      referenceCodeService.validateReferenceCode(ReferenceCodeGroup.DOMESTIC_STS, code, allowInactive = true)
    }
  }

  private fun validateTitle(request: PatchContactRequest) {
    if (request.titleCode.isPresent && request.titleCode.get() != null) {
      val code = request.titleCode.get()!!
      referenceCodeService.validateReferenceCode(ReferenceCodeGroup.TITLE, code, allowInactive = true)
    }
  }

  private fun validateGender(request: PatchContactRequest) {
    if (request.genderCode.isPresent && request.genderCode.get() != null) {
      val code = request.genderCode.get()!!
      referenceCodeService.validateReferenceCode(ReferenceCodeGroup.GENDER, code, allowInactive = true)
    }
  }
}
