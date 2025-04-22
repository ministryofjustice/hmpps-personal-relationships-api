package uk.gov.justice.digital.hmpps.hmppscontactsapi.service

import jakarta.persistence.EntityNotFoundException
import jakarta.transaction.Transactional
import jakarta.validation.ValidationException
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppscontactsapi.config.User
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.ContactIdentityEntity
import uk.gov.justice.digital.hmpps.hmppscontactsapi.mapping.toModel
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.ReferenceCodeGroup
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.identity.CreateIdentityRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.identity.CreateMultipleIdentitiesRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.identity.UpdateIdentityRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.ContactIdentityDetails
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.ReferenceCode
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.ContactIdentityDetailsRepository
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.ContactIdentityRepository
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.ContactRepository
import uk.gov.justice.digital.hmpps.hmppscontactsapi.util.PNCValidator
import java.time.LocalDateTime

@Service
class ContactIdentityService(
  private val contactRepository: ContactRepository,
  private val contactIdentityRepository: ContactIdentityRepository,
  private val contactIdentityDetailsRepository: ContactIdentityDetailsRepository,
  private val referenceCodeService: ReferenceCodeService,
) {

  @Transactional
  fun create(contactId: Long, request: CreateIdentityRequest, user: User): ContactIdentityDetails {
    validateContactExists(contactId)
    return createAContactIdentity(
      contactId,
      request.identityType,
      request.identityValue,
      request.issuingAuthority,
      user.username,
    )
  }

  @Transactional
  fun createMultiple(contactId: Long, request: CreateMultipleIdentitiesRequest, user: User): List<ContactIdentityDetails> {
    validateContactExists(contactId)
    return request.identities.map {
      createAContactIdentity(
        contactId,
        it.identityType,
        it.identityValue,
        it.issuingAuthority,
        user.username,
      )
    }
  }

  private fun createAContactIdentity(
    contactId: Long,
    identityType: String,
    identityValue: String,
    issuingAuthority: String?,
    createdBy: String,
  ): ContactIdentityDetails {
    validatePNC(identityType, identityValue)
    val type = referenceCodeService.validateReferenceCode(
      ReferenceCodeGroup.ID_TYPE,
      identityType,
      allowInactive = false,
    )
    val created = contactIdentityRepository.saveAndFlush(
      ContactIdentityEntity(
        contactIdentityId = 0,
        contactId = contactId,
        identityType = identityType,
        identityValue = identityValue,
        issuingAuthority = issuingAuthority,
        createdBy = createdBy,
        createdTime = LocalDateTime.now(),
      ),
    )
    return created.toDomainWithType(type)
  }

  @Transactional
  fun update(contactId: Long, contactIdentityId: Long, request: UpdateIdentityRequest, user: User): ContactIdentityDetails {
    validateContactExists(contactId)
    val existing = validateExistingIdentity(contactIdentityId)
    val type = referenceCodeService.validateReferenceCode(ReferenceCodeGroup.ID_TYPE, request.identityType, allowInactive = true)
    validatePNC(request.identityType, request.identityValue)

    val updating = existing.copy(
      identityType = request.identityType,
      identityValue = request.identityValue,
      issuingAuthority = request.issuingAuthority,
      updatedBy = user.username,
      updatedTime = LocalDateTime.now(),
    )

    val updated = contactIdentityRepository.saveAndFlush(updating)

    return updated.toDomainWithType(type)
  }

  fun get(contactId: Long, contactIdentityId: Long): ContactIdentityDetails? = contactIdentityDetailsRepository.findByContactIdAndContactIdentityId(contactId, contactIdentityId)?.toModel()

  @Transactional
  fun delete(contactId: Long, contactIdentityId: Long) {
    validateContactExists(contactId)
    val existing = validateExistingIdentity(contactIdentityId)
    contactIdentityRepository.delete(existing)
  }

  private fun validateExistingIdentity(contactIdentityId: Long): ContactIdentityEntity {
    val existing = contactIdentityRepository.findById(contactIdentityId)
      .orElseThrow { EntityNotFoundException("Contact identity ($contactIdentityId) not found") }
    return existing
  }

  private fun validatePNC(identityType: String, identityValue: String) {
    if (identityType == "PNC" && !PNCValidator.isValid(identityValue)) {
      throw ValidationException("Identity value ($identityValue) is not a valid PNC Number")
    }
  }

  private fun validateContactExists(contactId: Long) {
    contactRepository.findById(contactId)
      .orElseThrow { EntityNotFoundException("Contact ($contactId) not found") }
  }

  private fun ContactIdentityEntity.toDomainWithType(
    type: ReferenceCode,
  ) = ContactIdentityDetails(
    contactIdentityId = this.contactIdentityId,
    contactId = this.contactId,
    identityType = this.identityType,
    identityTypeDescription = type.description,
    identityTypeIsActive = type.isActive,
    identityValue = this.identityValue,
    issuingAuthority = this.issuingAuthority,
    createdBy = this.createdBy,
    createdTime = this.createdTime,
    updatedBy = this.updatedBy,
    updatedTime = this.updatedTime,
  )
}
