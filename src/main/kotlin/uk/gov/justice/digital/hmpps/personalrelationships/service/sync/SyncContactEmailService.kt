package uk.gov.justice.digital.hmpps.personalrelationships.service.sync

import jakarta.persistence.EntityNotFoundException
import jakarta.validation.ValidationException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.personalrelationships.mapping.sync.toEntity
import uk.gov.justice.digital.hmpps.personalrelationships.mapping.sync.toModel
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.sync.SyncCreateContactEmailRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.sync.SyncUpdateContactEmailRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.sync.SyncContactEmail
import uk.gov.justice.digital.hmpps.personalrelationships.repository.ContactEmailRepository
import uk.gov.justice.digital.hmpps.personalrelationships.repository.ContactRepository

@Service
@Transactional
class SyncContactEmailService(
  val contactRepository: ContactRepository,
  val contactEmailRepository: ContactEmailRepository,
) {

  companion object {
    private val logger = LoggerFactory.getLogger(this::class.java)
  }

  @Transactional(readOnly = true)
  fun getContactEmailById(contactEmailId: Long): SyncContactEmail {
    val contactEmailEntity = contactEmailRepository.findById(contactEmailId)
      .orElseThrow { EntityNotFoundException("Contact email with ID $contactEmailId not found") }
    return contactEmailEntity.toModel()
  }

  fun createContactEmail(request: SyncCreateContactEmailRequest): SyncContactEmail {
    contactRepository.findById(request.contactId)
      .orElseThrow { EntityNotFoundException("Contact with ID ${request.contactId} not found") }
    return contactEmailRepository.saveAndFlush(request.toEntity()).toModel()
  }

  fun updateContactEmail(contactEmailId: Long, request: SyncUpdateContactEmailRequest): SyncContactEmail {
    val contact = contactRepository.findById(request.contactId)
      .orElseThrow { EntityNotFoundException("Contact with ID ${request.contactId} not found") }

    val emailEntity = contactEmailRepository.findById(contactEmailId)
      .orElseThrow { EntityNotFoundException("Contact email with ID $contactEmailId not found") }

    if (contact.contactId != emailEntity.contactId) {
      logger.error("Contact email update specified for a contact not linked to this email")
      throw ValidationException("Contact ID ${contact.contactId} is not linked to the email ${emailEntity.contactEmailId}")
    }

    val changedContactEmail = emailEntity.copy(
      contactId = request.contactId,
      emailAddress = request.emailAddress,
      updatedBy = request.updatedBy,
      updatedTime = request.updatedTime,
    )

    return contactEmailRepository.saveAndFlush(changedContactEmail).toModel()
  }

  fun deleteContactEmail(contactEmailId: Long): SyncContactEmail {
    val rowToDelete = contactEmailRepository.findById(contactEmailId)
      .orElseThrow { EntityNotFoundException("Contact email with ID $contactEmailId not found") }
    contactEmailRepository.deleteById(contactEmailId)
    return rowToDelete.toModel()
  }
}
