package uk.gov.justice.digital.hmpps.personalrelationships.service.sync

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.personalrelationships.entity.ContactPhoneEntity
import uk.gov.justice.digital.hmpps.personalrelationships.mapping.sync.toEntity
import uk.gov.justice.digital.hmpps.personalrelationships.mapping.sync.toModel
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.sync.SyncCreateContactAddressPhoneRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.sync.SyncUpdateContactAddressPhoneRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.sync.SyncContactAddressPhone
import uk.gov.justice.digital.hmpps.personalrelationships.repository.ContactAddressPhoneRepository
import uk.gov.justice.digital.hmpps.personalrelationships.repository.ContactAddressRepository
import uk.gov.justice.digital.hmpps.personalrelationships.repository.ContactPhoneRepository
import uk.gov.justice.digital.hmpps.personalrelationships.repository.ContactRepository

@Service
@Transactional
class SyncContactAddressPhoneService(
  val contactRepository: ContactRepository,
  val contactAddressRepository: ContactAddressRepository,
  val contactPhoneRepository: ContactPhoneRepository,
  val contactAddressPhoneRepository: ContactAddressPhoneRepository,
) {

  @Transactional(readOnly = true)
  fun getContactAddressPhoneById(contactAddressPhoneId: Long): SyncContactAddressPhone {
    // Get the address phone entity
    val contactAddressPhoneEntity = contactAddressPhoneRepository.findById(contactAddressPhoneId)
      .orElseThrow { EntityNotFoundException("Address-specific phone number with ID $contactAddressPhoneId not not found") }

    // Get the phone number details associated with it
    val contactPhoneEntity = contactPhoneRepository.findById(contactAddressPhoneEntity.contactPhoneId)
      .orElseThrow { EntityNotFoundException("Phone number with ID ${contactAddressPhoneEntity.contactPhoneId} not found") }

    // Build the response
    return contactAddressPhoneEntity.toModel(contactPhoneEntity)
  }

  fun createContactAddressPhone(request: SyncCreateContactAddressPhoneRequest): SyncContactAddressPhone {
    // Get the address this phone number should be created for
    val contactAddressEntity = contactAddressRepository.findById(request.contactAddressId)
      .orElseThrow { EntityNotFoundException("Contact address with ID ${request.contactAddressId} was not found") }

    // Save a new contact phone
    val phoneEntity = contactPhoneRepository.saveAndFlush(
      ContactPhoneEntity(
        contactPhoneId = 0L,
        contactId = contactAddressEntity.contactId!!,
        phoneType = request.phoneType,
        phoneNumber = request.phoneNumber,
        extNumber = request.extNumber,
        createdBy = request.createdBy,
        createdTime = request.createdTime,
      ),
    )

    // Save the join table row and return a response
    return contactAddressPhoneRepository.saveAndFlush(request.toEntity(phoneEntity)).toModel(phoneEntity)
  }

  fun updateContactAddressPhone(contactAddressPhoneId: Long, request: SyncUpdateContactAddressPhoneRequest): SyncContactAddressPhone {
    // Get the specific address-specific phone row
    val contactAddressPhone = contactAddressPhoneRepository.findById(contactAddressPhoneId)
      .orElseThrow { EntityNotFoundException("Contact address phone with ID $contactAddressPhoneId not found") }

    // Find the related phone number details
    val phoneEntity = contactPhoneRepository.findById(contactAddressPhone.contactPhoneId)
      .orElseThrow { EntityNotFoundException("Contact phone with ID ${contactAddressPhone.contactPhoneId} not found") }

    // Save the phone number changes  (no changes to the join table - the IDs stay the same)
    val updatedPhone = contactPhoneRepository.saveAndFlush(
      phoneEntity.copy(
        phoneType = request.phoneType,
        phoneNumber = request.phoneNumber,
        extNumber = request.extNumber,
        updatedBy = request.updatedBy,
        updatedTime = request.updatedTime,
      ),
    )

    return contactAddressPhone.toModel(updatedPhone)
  }

  fun deleteContactAddressPhone(contactAddressPhoneId: Long): SyncContactAddressPhone {
    // Find the specific addressPhone row
    val addressPhoneToDelete = contactAddressPhoneRepository.findById(contactAddressPhoneId)
      .orElseThrow { EntityNotFoundException("Address-specific phone number with ID $contactAddressPhoneId was not found") }

    val phoneToDelete = contactPhoneRepository.findById(addressPhoneToDelete.contactPhoneId)
      .orElseThrow { EntityNotFoundException("Contact phone with ID ${addressPhoneToDelete.contactPhoneId} was not found") }

    // Delete the phone number and join table row (leave the address)
    contactAddressPhoneRepository.deleteById(addressPhoneToDelete.contactAddressPhoneId)
    contactPhoneRepository.deleteById(phoneToDelete.contactPhoneId)

    // Build the response from the deleted row data
    return addressPhoneToDelete.toModel(phoneToDelete)
  }
}
