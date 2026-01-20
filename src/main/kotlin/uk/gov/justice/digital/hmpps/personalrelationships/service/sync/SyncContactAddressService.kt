package uk.gov.justice.digital.hmpps.personalrelationships.service.sync

import jakarta.persistence.EntityNotFoundException
import jakarta.validation.ValidationException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.personalrelationships.mapping.sync.toEntity
import uk.gov.justice.digital.hmpps.personalrelationships.mapping.sync.toModel
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.sync.SyncCreateContactAddressRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.sync.SyncUpdateContactAddressRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.sync.SyncContactAddress
import uk.gov.justice.digital.hmpps.personalrelationships.repository.ContactAddressRepository
import uk.gov.justice.digital.hmpps.personalrelationships.repository.ContactRepository
import java.time.LocalDateTime

@Service
@Transactional
class SyncContactAddressService(
  private val contactRepository: ContactRepository,
  private val contactAddressRepository: ContactAddressRepository,
) {
  companion object {
    private val logger = LoggerFactory.getLogger(this::class.java)
  }

  @Transactional(readOnly = true)
  fun getContactAddressById(contactAddressId: Long): SyncContactAddress {
    val contactAddress = contactAddressRepository.findById(contactAddressId)
      .orElseThrow { EntityNotFoundException("Contact address with ID $contactAddressId not found") }
    return contactAddress.toModel()
  }

  fun createContactAddress(request: SyncCreateContactAddressRequest): SyncContactAddress {
    contactRepository.findById(request.contactId)
      .orElseThrow { EntityNotFoundException("Contact with ID ${request.contactId} not found") }
    return contactAddressRepository.saveAndFlush(request.toEntity()).toModel()
  }

  fun updateContactAddress(contactAddressId: Long, request: SyncUpdateContactAddressRequest): SyncContactAddress {
    val contact = contactRepository.findById(request.contactId)
      .orElseThrow { EntityNotFoundException("Contact with ID ${request.contactId} not found") }

    val contactAddress = contactAddressRepository.findById(contactAddressId)
      .orElseThrow { EntityNotFoundException("Contact address with ID $contactAddressId not found") }

    if (contact.contactId != contactAddress.contactId) {
      logger.error("Contact address update specified for a contact not linked to this address")
      throw ValidationException("Contact ID ${contact.contactId} is not linked to the address ${contactAddress.contactAddressId}")
    }

    val changedContactAddress = contactAddress.copy(
      primaryAddress = request.primaryAddress,
      addressType = request.addressType,
      flat = request.flat,
      property = request.property,
      street = request.street,
      area = request.area,
      cityCode = request.cityCode,
      countyCode = request.countyCode,
      countryCode = request.countryCode,
      postCode = request.postcode,
      verified = request.verified,
      mailFlag = request.mailFlag ?: false,
      startDate = request.startDate,
      endDate = request.endDate,
      noFixedAddress = request.noFixedAddress ?: false,
      comments = request.comments,
    ).also {
      it.updatedBy = request.updatedBy
      it.updatedTime = request.updatedTime
      if (!contactAddress.verified && request.verified) {
        it.verifiedBy = request.updatedBy
        it.verifiedTime = LocalDateTime.now()
      }
    }

    return contactAddressRepository.saveAndFlush(changedContactAddress).toModel()
  }

  fun deleteContactAddress(contactAddressId: Long): SyncContactAddress {
    val rowToDelete = contactAddressRepository.findById(contactAddressId)
      .orElseThrow { EntityNotFoundException("Contact address with ID $contactAddressId not found") }

    contactAddressRepository.deleteById(contactAddressId)

    return rowToDelete.toModel()
  }
}
