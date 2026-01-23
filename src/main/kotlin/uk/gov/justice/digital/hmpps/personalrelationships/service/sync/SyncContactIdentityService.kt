package uk.gov.justice.digital.hmpps.personalrelationships.service.sync

import jakarta.persistence.EntityNotFoundException
import jakarta.validation.ValidationException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.personalrelationships.mapping.sync.toEntity
import uk.gov.justice.digital.hmpps.personalrelationships.mapping.sync.toModel
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.sync.SyncCreateContactIdentityRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.sync.SyncUpdateContactIdentityRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.sync.SyncContactIdentity
import uk.gov.justice.digital.hmpps.personalrelationships.repository.ContactIdentityRepository
import uk.gov.justice.digital.hmpps.personalrelationships.repository.ContactRepository

@Service
@Transactional
class SyncContactIdentityService(
  val contactRepository: ContactRepository,
  val contactIdentityRepository: ContactIdentityRepository,
) {

  companion object {
    private val logger = LoggerFactory.getLogger(this::class.java)
  }

  @Transactional(readOnly = true)
  fun getContactIdentityById(contactIdentityId: Long): SyncContactIdentity {
    val contactIdentityEntity = contactIdentityRepository.findById(contactIdentityId)
      .orElseThrow { EntityNotFoundException("Contact identity with ID $contactIdentityId not found") }
    return contactIdentityEntity.toModel()
  }

  fun createContactIdentity(request: SyncCreateContactIdentityRequest): SyncContactIdentity {
    contactRepository.findById(request.contactId)
      .orElseThrow { EntityNotFoundException("Contact with ID ${request.contactId} not found") }
    return contactIdentityRepository.saveAndFlush(request.toEntity()).toModel()
  }

  fun updateContactIdentity(contactIdentityId: Long, request: SyncUpdateContactIdentityRequest): SyncContactIdentity {
    val contact = contactRepository.findById(request.contactId)
      .orElseThrow { EntityNotFoundException("Contact with ID ${request.contactId} not found") }

    val identityEntity = contactIdentityRepository.findById(contactIdentityId)
      .orElseThrow { EntityNotFoundException("Contact identity with ID $contactIdentityId not found") }

    if (contact.contactId != identityEntity.contactId) {
      logger.error("Contact identity update specified for a contact not linked to this identity")
      throw ValidationException("Contact ID ${contact.contactId} is not linked to the identity ${identityEntity.contactIdentityId}")
    }

    val changedContactIdentity = identityEntity.copy(
      contactId = request.contactId,
      identityType = request.identityType,
      identityValue = request.identityValue,
      issuingAuthority = request.issuingAuthority ?: identityEntity.issuingAuthority,
      updatedBy = request.updatedBy,
      updatedTime = request.updatedTime,
    )

    return contactIdentityRepository.saveAndFlush(changedContactIdentity).toModel()
  }

  fun deleteContactIdentity(contactIdentityId: Long): SyncContactIdentity {
    val rowToDelete = contactIdentityRepository.findById(contactIdentityId)
      .orElseThrow { EntityNotFoundException("Contact identity with ID $contactIdentityId not found") }
    contactIdentityRepository.deleteById(contactIdentityId)
    return rowToDelete.toModel()
  }
}
