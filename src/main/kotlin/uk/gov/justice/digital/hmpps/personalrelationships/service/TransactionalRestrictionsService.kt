package uk.gov.justice.digital.hmpps.personalrelationships.service

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.personalrelationships.config.User
import uk.gov.justice.digital.hmpps.personalrelationships.entity.ContactRestrictionEntity
import uk.gov.justice.digital.hmpps.personalrelationships.entity.PrisonerContactEntity
import uk.gov.justice.digital.hmpps.personalrelationships.entity.PrisonerContactRestrictionEntity
import uk.gov.justice.digital.hmpps.personalrelationships.model.ReferenceCodeGroup
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.restrictions.CreateContactRestrictionRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.restrictions.CreatePrisonerContactRestrictionRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.restrictions.UpdateContactRestrictionRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.restrictions.UpdatePrisonerContactRestrictionRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.ContactRestrictionDetails
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.PrisonerContactRestrictionDetails
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.ReferenceCode
import uk.gov.justice.digital.hmpps.personalrelationships.repository.ContactRepository
import uk.gov.justice.digital.hmpps.personalrelationships.repository.ContactRestrictionRepository
import uk.gov.justice.digital.hmpps.personalrelationships.repository.PrisonerContactRepository
import uk.gov.justice.digital.hmpps.personalrelationships.repository.PrisonerContactRestrictionRepository
import java.time.LocalDateTime

@Service
class TransactionalRestrictionsService(
  private val contactRestrictionRepository: ContactRestrictionRepository,
  private val contactRepository: ContactRepository,
  private val prisonerContactRepository: PrisonerContactRepository,
  private val prisonerContactRestrictionRepository: PrisonerContactRestrictionRepository,
  private val referenceCodeService: ReferenceCodeService,
) {

  @Transactional
  fun createContactGlobalRestriction(
    contactId: Long,
    request: CreateContactRestrictionRequest,
    user: User,
    enteredByDisplayName: String,
  ): ContactRestrictionDetails {
    validateContactExists(contactId)
    val type = referenceCodeService.validateReferenceCode(ReferenceCodeGroup.RESTRICTION, request.restrictionType, allowInactive = false)
    val created = contactRestrictionRepository.saveAndFlush(
      ContactRestrictionEntity(
        contactRestrictionId = 0,
        contactId = contactId,
        restrictionType = request.restrictionType,
        startDate = request.startDate,
        expiryDate = request.expiryDate,
        comments = request.comments,
        createdBy = user.username,
        createdTime = LocalDateTime.now(),
      ),
    )
    return contactRestrictionDetails(created, type, enteredByDisplayName)
  }

  @Transactional
  fun updateContactGlobalRestriction(
    contactId: Long,
    contactRestrictionId: Long,
    request: UpdateContactRestrictionRequest,
    user: User,
    enteredByDisplayName: String,
  ): ContactRestrictionDetails {
    validateContactExists(contactId)
    val contactRestriction = contactRestrictionRepository.findById(contactRestrictionId)
      .orElseThrow { EntityNotFoundException("Contact restriction ($contactRestrictionId) could not be found") }
    val type = referenceCodeService.validateReferenceCode(ReferenceCodeGroup.RESTRICTION, request.restrictionType, allowInactive = true)
    val updated = contactRestrictionRepository.saveAndFlush(
      contactRestriction.copy(
        restrictionType = request.restrictionType,
        startDate = request.startDate,
        expiryDate = request.expiryDate,
        comments = request.comments,
        updatedBy = user.username,
        updatedTime = LocalDateTime.now(),
      ),
    )
    return contactRestrictionDetails(updated, type, enteredByDisplayName)
  }

  @Transactional
  fun createPrisonerContactRestriction(
    prisonerContactId: Long,
    request: CreatePrisonerContactRestrictionRequest,
    user: User,
    enteredByDisplayName: String,
  ): PrisonerContactRestrictionDetails {
    val relationship = requirePrisonerContact(prisonerContactId)
    val type = referenceCodeService.validateReferenceCode(ReferenceCodeGroup.RESTRICTION, request.restrictionType, allowInactive = false)
    val created = prisonerContactRestrictionRepository.saveAndFlush(
      PrisonerContactRestrictionEntity(
        prisonerContactRestrictionId = 0,
        prisonerContactId = prisonerContactId,
        restrictionType = request.restrictionType,
        startDate = request.startDate,
        expiryDate = request.expiryDate,
        comments = request.comments,
        createdBy = user.username,
        createdTime = LocalDateTime.now(),
      ),
    )
    return prisonerContactRestrictionDetails(created, relationship, type, enteredByDisplayName)
  }

  @Transactional
  fun updatePrisonerContactRestriction(
    prisonerContactId: Long,
    prisonerContactRestrictionId: Long,
    request: UpdatePrisonerContactRestrictionRequest,
    user: User,
    enteredByDisplayName: String,
  ): PrisonerContactRestrictionDetails {
    val relationship = requirePrisonerContact(prisonerContactId)
    val prisonerContactRestriction = prisonerContactRestrictionRepository.findById(prisonerContactRestrictionId)
      .orElseThrow { EntityNotFoundException("Prisoner contact restriction ($prisonerContactRestrictionId) could not be found") }
    val type = referenceCodeService.validateReferenceCode(ReferenceCodeGroup.RESTRICTION, request.restrictionType, allowInactive = true)
    val updated = prisonerContactRestrictionRepository.saveAndFlush(
      prisonerContactRestriction.copy(
        restrictionType = request.restrictionType,
        startDate = request.startDate,
        expiryDate = request.expiryDate,
        comments = request.comments,
        updatedBy = user.username,
        updatedTime = LocalDateTime.now(),
      ),
    )
    return prisonerContactRestrictionDetails(updated, relationship, type, enteredByDisplayName)
  }

  private fun validateContactExists(contactId: Long) {
    contactRepository.findById(contactId)
      .orElseThrow { EntityNotFoundException("Contact ($contactId) could not be found") }
  }

  private fun requirePrisonerContact(prisonerContactId: Long): PrisonerContactEntity = prisonerContactRepository.findById(prisonerContactId)
    .orElseThrow { EntityNotFoundException("Prisoner contact ($prisonerContactId) could not be found") }

  private fun contactRestrictionDetails(
    entity: ContactRestrictionEntity,
    type: ReferenceCode,
    enteredByDisplayName: String,
  ) = ContactRestrictionDetails(
    contactRestrictionId = entity.contactRestrictionId,
    contactId = entity.contactId,
    restrictionType = entity.restrictionType,
    restrictionTypeDescription = type.description,
    startDate = entity.startDate,
    expiryDate = entity.expiryDate,
    comments = entity.comments,
    enteredByUsername = entity.updatedBy ?: entity.createdBy,
    enteredByDisplayName = enteredByDisplayName,
    createdBy = entity.createdBy,
    createdTime = entity.createdTime,
    updatedBy = entity.updatedBy,
    updatedTime = entity.updatedTime,
  )

  private fun prisonerContactRestrictionDetails(
    entity: PrisonerContactRestrictionEntity,
    relationship: PrisonerContactEntity,
    type: ReferenceCode,
    enteredByDisplayName: String,
  ) = PrisonerContactRestrictionDetails(
    prisonerContactRestrictionId = entity.prisonerContactRestrictionId,
    prisonerContactId = entity.prisonerContactId,
    contactId = relationship.contactId,
    prisonerNumber = relationship.prisonerNumber,
    restrictionType = entity.restrictionType,
    restrictionTypeDescription = type.description,
    startDate = entity.startDate,
    expiryDate = entity.expiryDate,
    comments = entity.comments,
    enteredByUsername = entity.updatedBy ?: entity.createdBy,
    enteredByDisplayName = enteredByDisplayName,
    createdBy = entity.createdBy,
    createdTime = entity.createdTime,
    updatedBy = entity.updatedBy,
    updatedTime = entity.updatedTime,
  )
}
