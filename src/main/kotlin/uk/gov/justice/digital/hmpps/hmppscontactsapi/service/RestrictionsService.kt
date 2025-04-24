package uk.gov.justice.digital.hmpps.hmppscontactsapi.service

import jakarta.persistence.EntityNotFoundException
import jakarta.validation.ValidationException
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppscontactsapi.config.User
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.ContactRestrictionEntity
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.PrisonerContactEntity
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.PrisonerContactRestrictionEntity
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.ReferenceCodeGroup
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.restrictions.CreateContactRestrictionRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.restrictions.CreatePrisonerContactRestrictionRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.restrictions.UpdateContactRestrictionRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.restrictions.UpdatePrisonerContactRestrictionRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.ContactRestrictionDetails
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.PrisonerContactRestrictionDetails
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.PrisonerContactRestrictionsResponse
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.ReferenceCode
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.ContactRepository
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.ContactRestrictionDetailsRepository
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.ContactRestrictionRepository
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.PrisonerContactRepository
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.PrisonerContactRestrictionDetailsRepository
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.PrisonerContactRestrictionRepository
import java.time.LocalDate
import java.time.LocalDateTime

@Service
class RestrictionsService(
  private val contactRestrictionDetailsRepository: ContactRestrictionDetailsRepository,
  private val contactRestrictionRepository: ContactRestrictionRepository,
  private val contactRepository: ContactRepository,
  private val prisonerContactRepository: PrisonerContactRepository,
  private val prisonerContactRestrictionDetailsRepository: PrisonerContactRestrictionDetailsRepository,
  private val prisonerContactRestrictionRepository: PrisonerContactRestrictionRepository,
  private val referenceCodeService: ReferenceCodeService,
  private val manageUsersService: ManageUsersService,
) {

  fun getGlobalRestrictionsForContact(contactId: Long): List<ContactRestrictionDetails> {
    validateContactExists(contactId)
    val restrictionsWithEnteredBy = contactRestrictionDetailsRepository.findAllByContactId(contactId)
      .map { entity -> entity to (entity.updatedBy ?: entity.createdBy) }
    val enteredByMap = restrictionsWithEnteredBy
      .map { (_, enteredByUsername) -> enteredByUsername }
      .toSet().associateWith { enteredByUsername -> manageUsersService.getUserByUsername(enteredByUsername)?.name ?: enteredByUsername }
    return restrictionsWithEnteredBy.map { (entity, enteredByUsername) ->
      ContactRestrictionDetails(
        contactRestrictionId = entity.contactRestrictionId,
        contactId = entity.contactId,
        restrictionType = entity.restrictionType,
        restrictionTypeDescription = entity.restrictionTypeDescription,
        startDate = entity.startDate,
        expiryDate = entity.expiryDate,
        comments = entity.comments,
        enteredByUsername = enteredByUsername,
        enteredByDisplayName = enteredByMap[enteredByUsername] ?: enteredByUsername,
        createdBy = entity.createdBy,
        createdTime = entity.createdTime,
        updatedBy = entity.updatedBy,
        updatedTime = entity.updatedTime,
      )
    }
  }

  fun getPrisonerContactRestrictions(prisonerContactId: Long): PrisonerContactRestrictionsResponse {
    val prisonerContact = prisonerContactRepository.findById(prisonerContactId)
      .orElseThrow { EntityNotFoundException("Prisoner contact ($prisonerContactId) could not be found") }
    val restrictionsWithEnteredBy = prisonerContactRestrictionDetailsRepository.findAllByPrisonerContactId(
      prisonerContactId,
    ).map { entity -> entity to (entity.updatedBy ?: entity.createdBy) }
    val enteredByMap = restrictionsWithEnteredBy
      .map { (_, enteredByUsername) -> enteredByUsername }
      .toSet().associateWith { enteredByUsername -> manageUsersService.getUserByUsername(enteredByUsername)?.name ?: enteredByUsername }
    return PrisonerContactRestrictionsResponse(
      prisonerContactRestrictions = restrictionsWithEnteredBy.map { (entity, enteredByUsername) ->
        PrisonerContactRestrictionDetails(
          prisonerContactRestrictionId = entity.prisonerContactRestrictionId,
          prisonerContactId = prisonerContactId,
          contactId = prisonerContact.contactId,
          prisonerNumber = prisonerContact.prisonerNumber,
          restrictionType = entity.restrictionType,
          restrictionTypeDescription = entity.restrictionTypeDescription,
          startDate = entity.startDate,
          expiryDate = entity.expiryDate,
          comments = entity.comments,
          enteredByUsername = enteredByUsername,
          enteredByDisplayName = enteredByMap[enteredByUsername] ?: enteredByUsername,
          createdBy = entity.createdBy,
          createdTime = entity.createdTime,
          updatedBy = entity.updatedBy,
          updatedTime = entity.updatedTime,
        )
      },
      contactGlobalRestrictions = getGlobalRestrictionsForContact(prisonerContact.contactId),
    )
  }

  fun createContactGlobalRestriction(
    contactId: Long,
    request: CreateContactRestrictionRequest,
    user: User,
  ): ContactRestrictionDetails {
    validateContactExists(contactId)
    validateExpiryDateBeforeStartDate(request.startDate, request.expiryDate)

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
    return contactRestrictionDetails(created, type)
  }

  private fun validateExpiryDateBeforeStartDate(startDate: LocalDate, expiryDate: LocalDate?) {
    if (expiryDate != null && startDate.isAfter(expiryDate)) {
      throw ValidationException("Restriction start date should be before the restriction end date")
    }
  }

  fun updateContactGlobalRestriction(
    contactId: Long,
    contactRestrictionId: Long,
    request: UpdateContactRestrictionRequest,
    user: User,
  ): ContactRestrictionDetails {
    validateContactExists(contactId)
    validateExpiryDateBeforeStartDate(request.startDate, request.expiryDate)
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
    return contactRestrictionDetails(updated, type)
  }

  private fun contactRestrictionDetails(
    entity: ContactRestrictionEntity,
    type: ReferenceCode,
  ): ContactRestrictionDetails {
    val enteredByUsername = entity.updatedBy ?: entity.createdBy
    val enteredByDisplayName = manageUsersService.getUserByUsername(enteredByUsername)?.name ?: enteredByUsername
    return ContactRestrictionDetails(
      contactRestrictionId = entity.contactRestrictionId,
      contactId = entity.contactId,
      restrictionType = entity.restrictionType,
      restrictionTypeDescription = type.description,
      startDate = entity.startDate,
      expiryDate = entity.expiryDate,
      comments = entity.comments,
      enteredByUsername = enteredByUsername,
      enteredByDisplayName = enteredByDisplayName,
      createdBy = entity.createdBy,
      createdTime = entity.createdTime,
      updatedBy = entity.updatedBy,
      updatedTime = entity.updatedTime,
    )
  }

  fun createPrisonerContactRestriction(
    prisonerContactId: Long,
    request: CreatePrisonerContactRestrictionRequest,
    user: User,
  ): PrisonerContactRestrictionDetails {
    val relationship = prisonerContactRepository.findById(prisonerContactId)
      .orElseThrow { EntityNotFoundException("Prisoner contact ($prisonerContactId) could not be found") }
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
    return prisonerContactRestrictionDetails(created, relationship, type)
  }

  fun updatePrisonerContactRestriction(
    prisonerContactId: Long,
    prisonerContactRestrictionId: Long,
    request: UpdatePrisonerContactRestrictionRequest,
    user: User,
  ): PrisonerContactRestrictionDetails {
    val relationship = prisonerContactRepository.findById(prisonerContactId)
      .orElseThrow { EntityNotFoundException("Prisoner contact ($prisonerContactId) could not be found") }
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
    return prisonerContactRestrictionDetails(updated, relationship, type)
  }

  private fun prisonerContactRestrictionDetails(
    entity: PrisonerContactRestrictionEntity,
    relationship: PrisonerContactEntity,
    type: ReferenceCode,
  ): PrisonerContactRestrictionDetails {
    val enteredByUsername = entity.updatedBy ?: entity.createdBy
    val enteredByDisplayName = manageUsersService.getUserByUsername(enteredByUsername)?.name ?: enteredByUsername
    return PrisonerContactRestrictionDetails(
      prisonerContactRestrictionId = entity.prisonerContactRestrictionId,
      prisonerContactId = entity.prisonerContactId,
      contactId = relationship.contactId,
      prisonerNumber = relationship.prisonerNumber,
      restrictionType = entity.restrictionType,
      restrictionTypeDescription = type.description,
      startDate = entity.startDate,
      expiryDate = entity.expiryDate,
      comments = entity.comments,
      enteredByUsername = enteredByUsername,
      enteredByDisplayName = enteredByDisplayName,
      createdBy = entity.createdBy,
      createdTime = entity.createdTime,
      updatedBy = entity.updatedBy,
      updatedTime = entity.updatedTime,
    )
  }

  private fun validateContactExists(contactId: Long) {
    contactRepository.findById(contactId)
      .orElseThrow { EntityNotFoundException("Contact ($contactId) could not be found") }
  }
}
