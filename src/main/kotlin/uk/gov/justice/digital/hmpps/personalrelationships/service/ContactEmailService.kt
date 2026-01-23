package uk.gov.justice.digital.hmpps.personalrelationships.service

import jakarta.persistence.EntityNotFoundException
import jakarta.transaction.Transactional
import jakarta.validation.ValidationException
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.personalrelationships.config.User
import uk.gov.justice.digital.hmpps.personalrelationships.entity.ContactEmailEntity
import uk.gov.justice.digital.hmpps.personalrelationships.exception.DuplicateEmailException
import uk.gov.justice.digital.hmpps.personalrelationships.mapping.toModel
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.email.CreateEmailRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.email.EmailAddress
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.email.UpdateEmailRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.ContactEmailDetails
import uk.gov.justice.digital.hmpps.personalrelationships.repository.ContactEmailRepository
import uk.gov.justice.digital.hmpps.personalrelationships.repository.ContactRepository
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
  fun create(contactId: Long, request: CreateEmailRequest, user: User): ContactEmailDetails {
    validateContactExists(contactId)
    val existingLowercaseEmails = contactEmailRepository.findByContactId(contactId).map { it.emailAddress.lowercase() }
    return createAnEmail(request.emailAddress, user.username, contactId, existingLowercaseEmails)
  }

  @Transactional
  fun createMultiple(contactId: Long, createdBy: String, emailAddresses: List<EmailAddress>): List<ContactEmailDetails> {
    validateContactExists(contactId)
    val existingLowercaseEmails = contactEmailRepository.findByContactId(contactId).map { it.emailAddress.lowercase() }
    val requestedLowerCaseEmails = emailAddresses.map { it.emailAddress.lowercase() }
    if (requestedLowerCaseEmails.size != requestedLowerCaseEmails.toSet().size) {
      throw ValidationException("Request contains duplicate email addresses")
    }
    return emailAddresses.map { createAnEmail(it.emailAddress, createdBy, contactId, existingLowercaseEmails) }
  }

  private fun createAnEmail(
    emailAddress: String,
    createdBy: String,
    contactId: Long,
    existingLowercaseEmails: List<String>,
  ): ContactEmailDetails {
    validateEmailAddress(emailAddress, existingLowercaseEmails)
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
  fun update(contactId: Long, contactEmailId: Long, request: UpdateEmailRequest, user: User): ContactEmailDetails {
    validateContactExists(contactId)
    val existing = validateExistingEmail(contactEmailId)
    val existingLowercaseEmailsExcludingTheOneBeingUpdated = contactEmailRepository.findByContactId(contactId)
      .filter { it.contactEmailId != existing.contactEmailId }
      .map { it.emailAddress.lowercase() }
    validateEmailAddress(request.emailAddress, existingLowercaseEmailsExcludingTheOneBeingUpdated)

    val updating = existing.copy(
      emailAddress = request.emailAddress,
      updatedBy = user.username,
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

  private fun validateEmailAddress(emailAddress: String, otherEmails: List<String>) {
    if (!emailAddress.matches(EMAIL_REGEX)) {
      throw ValidationException("Email address is invalid")
    }
    if (emailAddress.lowercase() in otherEmails) {
      throw DuplicateEmailException(emailAddress)
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
