package uk.gov.justice.digital.hmpps.hmppscontactsapi.service

import jakarta.persistence.EntityNotFoundException
import jakarta.transaction.Transactional
import jakarta.validation.ValidationException
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.ContactEmailEntity
import uk.gov.justice.digital.hmpps.hmppscontactsapi.mapping.toModel
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.email.CreateEmailRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.email.CreateMultipleEmailsRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.email.UpdateEmailRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.ContactEmailDetails
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.ContactEmailRepository
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.ContactRepository
import java.time.LocalDateTime

@Service
class ContactEmailService(
  private val contactRepository: ContactRepository,
  private val contactEmailRepository: ContactEmailRepository,
) {

  companion object {
    // something @ something . something and only 1 @
    private val EMAIL_REGEX = Regex("^[^@]+@[^@]+\\.[^@]+\$")
  }

  @Transactional
  fun create(contactId: Long, request: CreateEmailRequest): ContactEmailDetails {
    validateContactExists(contactId)
    return createAnEmail(request.emailAddress, request.createdBy, contactId)
  }

  @Transactional
  fun createMultiple(contactId: Long, request: CreateMultipleEmailsRequest): List<ContactEmailDetails> {
    validateContactExists(contactId)
    return request.emailAddresses.map { createAnEmail(it.emailAddress, request.createdBy, contactId) }
  }

  private fun createAnEmail(
    emailAddress: String,
    createdBy: String,
    contactId: Long,
  ): ContactEmailDetails {
    validateEmailAddress(emailAddress)
    val created = contactEmailRepository.saveAndFlush(
      ContactEmailEntity(
        contactEmailId = 0,
        contactId = contactId,
        emailAddress = emailAddress,
        createdBy = createdBy,
      ),
    )
    return created.toDomainWithType()
  }

  @Transactional
  fun update(contactId: Long, contactEmailId: Long, request: UpdateEmailRequest): ContactEmailDetails {
    validateContactExists(contactId)
    validateEmailAddress(request.emailAddress)
    val existing = validateExistingEmail(contactEmailId)

    val updating = existing.copy(
      emailAddress = request.emailAddress,
      updatedBy = request.updatedBy,
      updatedTime = LocalDateTime.now(),
    )

    val updated = contactEmailRepository.saveAndFlush(updating)

    return updated.toDomainWithType()
  }

  fun get(contactId: Long, contactEmailId: Long): ContactEmailDetails? = contactEmailRepository.findByContactIdAndContactEmailId(contactId, contactEmailId)?.toModel()

  @Transactional
  fun delete(contactId: Long, contactEmailId: Long) {
    validateContactExists(contactId)
    val existing = validateExistingEmail(contactEmailId)
    contactEmailRepository.delete(existing)
  }

  private fun validateEmailAddress(emailAddress: String) {
    if (!emailAddress.matches(EMAIL_REGEX)) {
      throw ValidationException("Email address is invalid")
    }
  }

  private fun validateExistingEmail(contactEmailId: Long): ContactEmailEntity {
    val existing = contactEmailRepository.findById(contactEmailId)
      .orElseThrow { EntityNotFoundException("Contact email ($contactEmailId) not found") }
    return existing
  }

  private fun validateContactExists(contactId: Long) {
    contactRepository.findById(contactId)
      .orElseThrow { EntityNotFoundException("Contact ($contactId) not found") }
  }

  private fun ContactEmailEntity.toDomainWithType() = ContactEmailDetails(
    contactEmailId = this.contactEmailId,
    contactId = this.contactId,
    emailAddress = this.emailAddress,
    createdBy = this.createdBy,
    createdTime = this.createdTime,
    updatedBy = this.updatedBy,
    updatedTime = this.updatedTime,
  )
}
