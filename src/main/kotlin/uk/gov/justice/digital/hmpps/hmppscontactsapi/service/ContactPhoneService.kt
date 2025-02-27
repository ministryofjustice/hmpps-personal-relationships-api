package uk.gov.justice.digital.hmpps.hmppscontactsapi.service

import jakarta.persistence.EntityNotFoundException
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.ContactPhoneEntity
import uk.gov.justice.digital.hmpps.hmppscontactsapi.mapping.toModel
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.ReferenceCodeGroup
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.phone.CreateMultiplePhoneNumbersRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.phone.CreatePhoneRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.phone.UpdatePhoneRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.ContactPhoneDetails
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.ReferenceCode
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.ContactAddressPhoneRepository
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.ContactPhoneDetailsRepository
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.ContactPhoneRepository
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.ContactRepository
import uk.gov.justice.digital.hmpps.hmppscontactsapi.util.validatePhoneNumber
import java.time.LocalDateTime

@Service
class ContactPhoneService(
  private val contactRepository: ContactRepository,
  private val contactPhoneRepository: ContactPhoneRepository,
  private val contactPhoneDetailsRepository: ContactPhoneDetailsRepository,
  private val contactAddressPhoneRepository: ContactAddressPhoneRepository,
  private val referenceCodeService: ReferenceCodeService,
) {

  @Transactional
  fun create(contactId: Long, request: CreatePhoneRequest): ContactPhoneDetails {
    validateContactExists(contactId)
    return createANewPhoneNumber(
      contactId,
      request.phoneType,
      request.phoneNumber,
      request.extNumber,
      request.createdBy,
    )
  }

  @Transactional
  fun createMultiple(contactId: Long, request: CreateMultiplePhoneNumbersRequest): List<ContactPhoneDetails> {
    validateContactExists(contactId)
    return request.phoneNumbers.map {
      createANewPhoneNumber(
        contactId,
        it.phoneType,
        it.phoneNumber,
        it.extNumber,
        request.createdBy,
      )
    }
  }

  private fun createANewPhoneNumber(
    contactId: Long,
    phoneType: String,
    phoneNumber: String,
    extNumber: String?,
    createdBy: String,
  ): ContactPhoneDetails {
    val type = referenceCodeService.validateReferenceCode(
      ReferenceCodeGroup.PHONE_TYPE,
      phoneType,
      allowInactive = false,
    )
    validatePhoneNumber(phoneNumber)
    val created = contactPhoneRepository.saveAndFlush(
      ContactPhoneEntity(
        contactPhoneId = 0,
        contactId = contactId,
        phoneType = phoneType,
        phoneNumber = phoneNumber,
        extNumber = extNumber,
        createdBy = createdBy,
        createdTime = LocalDateTime.now(),
      ),
    )
    return created.toDomainWithType(type)
  }

  fun get(contactId: Long, contactPhoneId: Long): ContactPhoneDetails? = contactPhoneDetailsRepository.findByContactIdAndContactPhoneId(contactId, contactPhoneId)?.toModel()

  @Transactional
  fun update(contactId: Long, contactPhoneId: Long, request: UpdatePhoneRequest): ContactPhoneDetails {
    validateContactExists(contactId)
    val existing = validateExistingPhone(contactPhoneId)
    val type =
      referenceCodeService.validateReferenceCode(ReferenceCodeGroup.PHONE_TYPE, request.phoneType, allowInactive = true)
    validatePhoneNumber(request.phoneNumber)

    val updating = existing.copy(
      phoneType = request.phoneType,
      phoneNumber = request.phoneNumber,
      extNumber = request.extNumber,
      updatedBy = request.updatedBy,
      updatedTime = LocalDateTime.now(),
    )

    val updated = contactPhoneRepository.saveAndFlush(updating)

    return updated.toDomainWithType(type)
  }

  @Transactional
  fun delete(contactId: Long, contactPhoneId: Long) {
    validateContactExists(contactId)
    val existing = validateExistingPhone(contactPhoneId)
    contactAddressPhoneRepository.deleteByContactPhoneId(existing.contactPhoneId)
    contactPhoneRepository.delete(existing)
  }

  private fun validateExistingPhone(contactPhoneId: Long): ContactPhoneEntity {
    val existing = contactPhoneRepository.findById(contactPhoneId)
      .orElseThrow { EntityNotFoundException("Contact phone ($contactPhoneId) not found") }
    return existing
  }

  private fun validateContactExists(contactId: Long) {
    contactRepository.findById(contactId)
      .orElseThrow { EntityNotFoundException("Contact ($contactId) not found") }
  }

  private fun ContactPhoneEntity.toDomainWithType(
    type: ReferenceCode,
  ) = ContactPhoneDetails(
    this.contactPhoneId,
    this.contactId,
    this.phoneType,
    type.description,
    this.phoneNumber,
    this.extNumber,
    this.createdBy,
    this.createdTime,
    this.updatedBy,
    this.updatedTime,
  )
}
