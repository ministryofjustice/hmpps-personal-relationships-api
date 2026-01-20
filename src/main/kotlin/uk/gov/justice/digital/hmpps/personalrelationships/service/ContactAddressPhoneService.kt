package uk.gov.justice.digital.hmpps.personalrelationships.service

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.personalrelationships.config.User
import uk.gov.justice.digital.hmpps.personalrelationships.entity.ContactAddressEntity
import uk.gov.justice.digital.hmpps.personalrelationships.entity.ContactAddressPhoneEntity
import uk.gov.justice.digital.hmpps.personalrelationships.entity.ContactEntity
import uk.gov.justice.digital.hmpps.personalrelationships.entity.ContactPhoneEntity
import uk.gov.justice.digital.hmpps.personalrelationships.model.ReferenceCodeGroup
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.phone.CreateContactAddressPhoneRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.phone.CreateMultiplePhoneNumbersRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.phone.PhoneNumber
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.phone.UpdateContactAddressPhoneRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.ContactAddressPhoneDetails
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.ReferenceCode
import uk.gov.justice.digital.hmpps.personalrelationships.repository.ContactAddressPhoneRepository
import uk.gov.justice.digital.hmpps.personalrelationships.repository.ContactAddressRepository
import uk.gov.justice.digital.hmpps.personalrelationships.repository.ContactPhoneRepository
import uk.gov.justice.digital.hmpps.personalrelationships.repository.ContactRepository
import uk.gov.justice.digital.hmpps.personalrelationships.util.validatePhoneNumber
import java.time.LocalDateTime

@Service
@Transactional
class ContactAddressPhoneService(
  private val contactRepository: ContactRepository,
  private val contactPhoneRepository: ContactPhoneRepository,
  private val contactAddressRepository: ContactAddressRepository,
  private val contactAddressPhoneRepository: ContactAddressPhoneRepository,
  private val referenceCodeService: ReferenceCodeService,
) {

  fun create(
    contactId: Long,
    contactAddressId: Long,
    request: CreateContactAddressPhoneRequest,
    user: User,
  ): ContactAddressPhoneDetails {
    validateContactExists(contactId)
    validateContactAddressExists(contactAddressId)
    return createAnAddressSpecificPhoneNumber(contactId, contactAddressId, request.phoneType, request.phoneNumber, request.extNumber, user.username)
  }

  fun createMultiple(
    contactId: Long,
    contactAddressId: Long,
    request: CreateMultiplePhoneNumbersRequest,
    user: User,
  ): List<ContactAddressPhoneDetails> {
    validateContactExists(contactId)
    validateContactAddressExists(contactAddressId)
    return request.phoneNumbers.map {
      createAnAddressSpecificPhoneNumber(contactId, contactAddressId, it.phoneType, it.phoneNumber, it.extNumber, user.username)
    }
  }

  @Transactional
  fun createMultipleAddressSpecificPhones(
    contactId: Long,
    contactAddressId: Long,
    createdBy: String,
    phoneNumbers: List<PhoneNumber>,
  ): List<Long> {
    validateContactExists(contactId)
    validateContactAddressExists(contactAddressId)
    return phoneNumbers.map {
      val contactAddressPhoneDetails = createAnAddressSpecificPhoneNumber(
        contactId,
        contactAddressId,
        it.phoneType,
        it.phoneNumber,
        it.extNumber,
        createdBy,
      )
      contactAddressPhoneDetails.contactAddressPhoneId
    }
  }

  private fun createAnAddressSpecificPhoneNumber(
    contactId: Long,
    contactAddressId: Long,
    phoneType: String,
    phoneNumber: String,
    extNumber: String?,
    createdBy: String,
  ): ContactAddressPhoneDetails {
    validatePhoneNumber(phoneNumber)
    val phoneTypeReference = referenceCodeService.validateReferenceCode(
      ReferenceCodeGroup.PHONE_TYPE,
      phoneType,
      allowInactive = false,
    )
    val createdTime = LocalDateTime.now()
    // Save the phone number
    val createdPhone = contactPhoneRepository.saveAndFlush(
      ContactPhoneEntity(
        contactPhoneId = 0,
        contactId = contactId,
        phoneType = phoneType,
        phoneNumber = phoneNumber,
        extNumber = extNumber,
        createdBy = createdBy,
        createdTime = createdTime,
      ),
    )

    // Save the contact address phone
    val createdAddressPhone = contactAddressPhoneRepository.saveAndFlush(
      ContactAddressPhoneEntity(
        contactAddressPhoneId = 0,
        contactId = contactId,
        contactAddressId = contactAddressId,
        contactPhoneId = createdPhone.contactPhoneId,
        createdBy = createdBy,
        createdTime = createdTime,
      ),
    )

    return createdAddressPhone.toModel(createdPhone, phoneTypeReference)
  }

  @Transactional(readOnly = true)
  fun get(contactId: Long, contactAddressPhoneId: Long): ContactAddressPhoneDetails {
    val addressPhone = validateContactAddressPhoneExists(contactAddressPhoneId)
    val phone = validatePhoneExists(addressPhone.contactPhoneId)
    val phoneTypeReference = referenceCodeService.getReferenceDataByGroupAndCode(ReferenceCodeGroup.PHONE_TYPE, phone.phoneType)
    return addressPhone.toModel(phone, phoneTypeReference)
  }

  fun update(
    contactId: Long,
    contactAddressPhoneId: Long,
    request: UpdateContactAddressPhoneRequest,
    user: User,
  ): ContactAddressPhoneDetails {
    validateContactExists(contactId)
    val existing = validateContactAddressPhoneExists(contactAddressPhoneId)
    val phone = validatePhoneExists(existing.contactPhoneId)
    val newPhoneType = referenceCodeService.validateReferenceCode(ReferenceCodeGroup.PHONE_TYPE, request.phoneType, allowInactive = true)

    validatePhoneNumber(request.phoneNumber)

    val updatingPhone = phone.copy(
      phoneType = request.phoneType,
      phoneNumber = request.phoneNumber,
      extNumber = request.extNumber,
      updatedBy = user.username,
      updatedTime = LocalDateTime.now(),
    )

    val updatedPhone = contactPhoneRepository.saveAndFlush(updatingPhone)

    val updatingContactAddressPhone = existing.copy(
      updatedBy = user.username,
      updatedTime = LocalDateTime.now(),
    )

    val updatedContactAddressPhone = contactAddressPhoneRepository.saveAndFlush(updatingContactAddressPhone)

    return updatedContactAddressPhone.toModel(updatedPhone, newPhoneType)
  }

  fun delete(contactId: Long, contactAddressPhoneId: Long): ContactAddressPhoneDetails {
    validateContactExists(contactId)
    val existingContactAddressPhone = validateContactAddressPhoneExists(contactAddressPhoneId)
    val existingPhone = validatePhoneExists(existingContactAddressPhone.contactPhoneId)
    val phoneTypeRef = referenceCodeService.getReferenceDataByGroupAndCode(ReferenceCodeGroup.PHONE_TYPE, existingPhone.phoneType)

    contactAddressPhoneRepository.deleteById(contactAddressPhoneId)
    contactPhoneRepository.deleteById(existingPhone.contactPhoneId)

    return existingContactAddressPhone.toModel(existingPhone, phoneTypeRef)
  }

  private fun validatePhoneExists(contactPhoneId: Long): ContactPhoneEntity = contactPhoneRepository.findById(contactPhoneId)
    .orElseThrow { EntityNotFoundException("Contact phone ($contactPhoneId) not found") }

  private fun validateContactExists(contactId: Long): ContactEntity = contactRepository.findById(contactId).orElseThrow { EntityNotFoundException("Contact ($contactId) not found") }

  private fun validateContactAddressExists(contactAddressId: Long): ContactAddressEntity = contactAddressRepository.findById(contactAddressId)
    .orElseThrow { EntityNotFoundException("Contact address ($contactAddressId) not found") }

  private fun validateContactAddressPhoneExists(contactAddressPhoneId: Long): ContactAddressPhoneEntity = contactAddressPhoneRepository.findById(contactAddressPhoneId)
    .orElseThrow { EntityNotFoundException("Contact address phone ($contactAddressPhoneId) not found") }

  private fun ContactAddressPhoneEntity.toModel(phone: ContactPhoneEntity, type: ReferenceCode?) = ContactAddressPhoneDetails(
    contactAddressPhoneId = this.contactAddressPhoneId,
    contactPhoneId = this.contactPhoneId,
    contactAddressId = this.contactAddressId,
    contactId = this.contactId,
    phoneType = phone.phoneType,
    phoneTypeDescription = type?.description ?: phone.phoneType,
    phoneNumber = phone.phoneNumber,
    extNumber = phone.extNumber,
    createdBy = this.createdBy,
    createdTime = this.createdTime,
    updatedBy = this.updatedBy,
    updatedTime = this.updatedTime,
  )
}
