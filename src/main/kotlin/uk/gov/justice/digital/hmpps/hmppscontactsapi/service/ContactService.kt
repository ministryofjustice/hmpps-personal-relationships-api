package uk.gov.justice.digital.hmpps.hmppscontactsapi.service

import jakarta.persistence.EntityNotFoundException
import jakarta.transaction.Transactional
import jakarta.validation.ValidationException
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.ContactAddressPhoneEntity
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.ContactEntity
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.PrisonerContactEntity
import uk.gov.justice.digital.hmpps.hmppscontactsapi.mapping.toEntity
import uk.gov.justice.digital.hmpps.hmppscontactsapi.mapping.toModel
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.ReferenceCodeGroup
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.AddContactRelationshipRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.ContactRelationship
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.ContactSearchRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.CreateContactRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.UpdateRelationshipRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.ContactAddressPhoneDetails
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.ContactCreationResult
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.ContactDetails
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.ContactPhoneDetails
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.ContactSearchResultItem
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.PrisonerContactRelationshipDetails
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.ContactAddressDetailsRepository
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.ContactAddressPhoneRepository
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.ContactEmailRepository
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.ContactIdentityDetailsRepository
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.ContactPhoneDetailsRepository
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.ContactRepository
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.ContactSearchRepository
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.PrisonerContactRepository
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.sync.EmploymentService
import java.time.LocalDateTime
import kotlin.jvm.optionals.getOrNull

@Service
class ContactService(
  private val contactRepository: ContactRepository,
  private val prisonerContactRepository: PrisonerContactRepository,
  private val prisonerService: PrisonerService,
  private val contactSearchRepository: ContactSearchRepository,
  private val contactAddressDetailsRepository: ContactAddressDetailsRepository,
  private val contactPhoneDetailsRepository: ContactPhoneDetailsRepository,
  private val contactAddressPhoneRepository: ContactAddressPhoneRepository,
  private val contactEmailRepository: ContactEmailRepository,
  private val contactIdentityDetailsRepository: ContactIdentityDetailsRepository,
  private val referenceCodeService: ReferenceCodeService,
  private val employmentService: EmploymentService,
) {
  companion object {
    private val logger = LoggerFactory.getLogger(this::class.java)
  }

  @Transactional
  fun createContact(request: CreateContactRequest): ContactCreationResult {
    if (request.relationship != null) {
      validateNewRelationship(request.relationship)
    }
    val newContact = request.toModel()
    val createdContact = contactRepository.saveAndFlush(newContact)
    val newRelationship = request.relationship?.toEntity(createdContact.id(), request.createdBy)
      ?.let { prisonerContactRepository.saveAndFlush(it) }

    logger.info("Created new contact {}", createdContact)
    newRelationship?.let { logger.info("Created new relationship {}", newRelationship) }
    return ContactCreationResult(
      enrichContact(createdContact),
      newRelationship?.let { enrichRelationship(newRelationship) },
    )
  }

  fun getContact(id: Long): ContactDetails? = contactRepository.findById(id).getOrNull()
    ?.let { enrichContact(it) }

  fun searchContacts(pageable: Pageable, request: ContactSearchRequest): Page<ContactSearchResultItem> = contactSearchRepository.searchContacts(request, pageable).toModel()

  @Transactional
  fun addContactRelationship(request: AddContactRelationshipRequest): PrisonerContactRelationshipDetails {
    validateNewRelationship(request.relationship)
    getContact(request.contactId) ?: throw EntityNotFoundException("Contact (${request.contactId}) could not be found")
    val newRelationship = request.relationship.toEntity(request.contactId, request.createdBy)
    prisonerContactRepository.saveAndFlush(newRelationship)
    return enrichRelationship(newRelationship)
  }

  private fun validateNewRelationship(relationship: ContactRelationship) {
    prisonerService.getPrisoner(relationship.prisonerNumber)
      ?: throw EntityNotFoundException("Prisoner (${relationship.prisonerNumber}) could not be found")
    referenceCodeService.validateReferenceCode(
      ReferenceCodeGroup.RELATIONSHIP_TYPE,
      relationship.relationshipType,
      allowInactive = false,
    )
    validateRelationshipToPrisoner(
      relationship.relationshipType,
      relationship.relationshipToPrisoner,
      allowInactive = false,
    )
  }

  private fun enrichContact(contactEntity: ContactEntity): ContactDetails {
    val phoneNumbers = contactPhoneDetailsRepository.findByContactId(contactEntity.id()).map { it.toModel() }
    val addressPhoneNumbers = contactAddressPhoneRepository.findByContactId(contactEntity.id())

    // Match address phone numbers with addresses
    val addresses = contactAddressDetailsRepository.findByContactId(contactEntity.id())
      .map { address ->
        address.toModel(
          getAddressPhoneNumbers(
            address.contactAddressId,
            addressPhoneNumbers,
            phoneNumbers,
          ),
        )
      }

    val emailAddresses = contactEmailRepository.findByContactId(contactEntity.id()).map { it.toModel() }
    val identities = contactIdentityDetailsRepository.findByContactId(contactEntity.id()).map { it.toModel() }
    val employments = employmentService.getEmploymentDetails(contactEntity.id())
    val languageDescription = contactEntity.languageCode?.let {
      referenceCodeService.getReferenceDataByGroupAndCode(
        ReferenceCodeGroup.LANGUAGE,
        it,
      )?.description
    }

    val domesticStatusDescription = contactEntity.domesticStatus?.let {
      referenceCodeService.getReferenceDataByGroupAndCode(
        ReferenceCodeGroup.DOMESTIC_STS,
        it,
      )?.description
    }

    val genderDescription = contactEntity.gender?.let {
      referenceCodeService.getReferenceDataByGroupAndCode(ReferenceCodeGroup.GENDER, it)?.description
    }

    val titleDescription = contactEntity.title?.let {
      referenceCodeService.getReferenceDataByGroupAndCode(ReferenceCodeGroup.TITLE, it)?.description
    }

    // Filter address-specific phone numbers out of the "global" phone number list
    val globalPhoneNumbers = phoneNumbers.filterNot { phone ->
      addressPhoneNumbers.any { addressPhone -> addressPhone.contactPhoneId == phone.contactPhoneId }
    }

    return ContactDetails(
      id = contactEntity.id(),
      title = contactEntity.title,
      titleDescription = titleDescription,
      lastName = contactEntity.lastName,
      firstName = contactEntity.firstName,
      middleNames = contactEntity.middleNames,
      dateOfBirth = contactEntity.dateOfBirth,
      isStaff = contactEntity.staffFlag,
      isDeceased = contactEntity.isDeceased,
      deceasedDate = contactEntity.deceasedDate,
      languageCode = contactEntity.languageCode,
      languageDescription = languageDescription,
      interpreterRequired = contactEntity.interpreterRequired,
      addresses = addresses,
      phoneNumbers = globalPhoneNumbers,
      emailAddresses = emailAddresses,
      identities = identities,
      employments = employments,
      domesticStatusCode = contactEntity.domesticStatus,
      domesticStatusDescription = domesticStatusDescription,
      gender = contactEntity.gender,
      genderDescription = genderDescription,
      createdBy = contactEntity.createdBy,
      createdTime = contactEntity.createdTime,
    )
  }

  private fun getAddressPhoneNumbers(
    contactAddressId: Long,
    addressPhoneNumbers: List<ContactAddressPhoneEntity>,
    phoneNumbers: List<ContactPhoneDetails>,
  ): List<ContactAddressPhoneDetails> = addressPhoneNumbers.filter { it.contactAddressId == contactAddressId }
    .mapNotNull { addressPhone ->
      phoneNumbers.find { it.contactPhoneId == addressPhone.contactPhoneId }?.let { phoneNumber ->
        ContactAddressPhoneDetails(
          contactAddressPhoneId = addressPhone.contactAddressPhoneId,
          contactPhoneId = addressPhone.contactPhoneId,
          contactAddressId = addressPhone.contactAddressId,
          contactId = addressPhone.contactId,
          phoneType = phoneNumber.phoneType,
          phoneTypeDescription = phoneNumber.phoneTypeDescription,
          phoneNumber = phoneNumber.phoneNumber,
          extNumber = phoneNumber.extNumber,
          createdBy = phoneNumber.createdBy,
          createdTime = phoneNumber.createdTime,
          updatedBy = phoneNumber.updatedBy,
          updatedTime = phoneNumber.updatedTime,
        )
      }
    }

  @Transactional
  fun updateContactRelationship(
    prisonerContactId: Long,
    request: UpdateRelationshipRequest,
  ): PrisonerContactRelationshipDetails {
    val prisonerContactEntity = getPrisonerContactEntity(prisonerContactId)

    validateRequest(request)
    validateRelationshipCodes(request, prisonerContactEntity)

    val changedPrisonerContact = prisonerContactEntity.applyUpdate(request)

    prisonerContactRepository.saveAndFlush(changedPrisonerContact)
    return enrichRelationship(prisonerContactEntity)
  }

  private fun validateRequest(request: UpdateRelationshipRequest) {
    unsupportedRelationshipType(request)
    unsupportedRelationshipToPrisoner(request)
    unsupportedApprovedToVisitFlag(request)
    unsupportedEmergencyContact(request)
    unsupportedNextOfKin(request)
    unsupportedRelationshipActive(request)
  }

  private fun getPrisonerContactEntity(prisonerContactId: Long): PrisonerContactEntity {
    val contact = prisonerContactRepository.findById(prisonerContactId)
      .orElseThrow { EntityNotFoundException("Prisoner contact with prisoner contact ID $prisonerContactId not found") }
    return contact
  }

  private fun PrisonerContactEntity.applyUpdate(
    request: UpdateRelationshipRequest,
  ) = this.copy(
    contactId = this.contactId,
    prisonerNumber = this.prisonerNumber,
    relationshipType = request.relationshipType.orElse(this.relationshipType),
    approvedVisitor = request.isApprovedVisitor.orElse(this.approvedVisitor),
    currentTerm = this.currentTerm,
    nextOfKin = request.isNextOfKin.orElse(this.nextOfKin),
    emergencyContact = request.isEmergencyContact.orElse(this.emergencyContact),
    active = request.isRelationshipActive.orElse(this.active),
    relationshipToPrisoner = request.relationshipToPrisoner.orElse(this.relationshipToPrisoner),
    comments = request.comments.orElse(this.comments),
  ).also {
    it.approvedBy = this.approvedBy
    it.approvedTime = this.approvedTime
    it.expiryDate = this.expiryDate
    it.createdAtPrison = this.createdAtPrison
    it.updatedBy = request.updatedBy
    it.updatedTime = LocalDateTime.now()
  }

  private fun validateRelationshipCodes(
    request: UpdateRelationshipRequest,
    preUpdateRelationship: PrisonerContactEntity,
  ) {
    if (request.relationshipType.isPresent && request.relationshipToPrisoner.isPresent) {
      // Changing both
      val relationshipType = request.relationshipType.get()
      referenceCodeService.validateReferenceCode(
        ReferenceCodeGroup.RELATIONSHIP_TYPE,
        relationshipType,
        allowInactive = true,
      )

      val relationshipToPrisoner = request.relationshipToPrisoner.get()
      validateRelationshipToPrisoner(relationshipType, relationshipToPrisoner, allowInactive = true)
    } else if (!request.relationshipType.isPresent && request.relationshipToPrisoner.isPresent) {
      // Changing only relationship to prisoner
      val relationshipType = preUpdateRelationship.relationshipType
      val relationshipToPrisoner = request.relationshipToPrisoner.get()
      validateRelationshipToPrisoner(relationshipType, relationshipToPrisoner, allowInactive = true)
    } else if (request.relationshipType.isPresent && !request.relationshipToPrisoner.isPresent) {
      // Changing only relationship type (this is only going to be valid if the type didn't actually change)
      val relationshipType = request.relationshipType.get()
      referenceCodeService.validateReferenceCode(
        ReferenceCodeGroup.RELATIONSHIP_TYPE,
        relationshipType,
        allowInactive = true,
      )

      val relationshipToPrisoner = preUpdateRelationship.relationshipToPrisoner
      validateRelationshipToPrisoner(relationshipType, relationshipToPrisoner, allowInactive = true)
    }
  }

  private fun validateRelationshipToPrisoner(
    relationshipType: String?,
    relationshipToPrisoner: String,
    allowInactive: Boolean,
  ) {
    referenceCodeService.validateReferenceCode(
      referenceCodeGroupForRelationshipType(relationshipType),
      relationshipToPrisoner,
      allowInactive,
    )
  }

  private fun referenceCodeGroupForRelationshipType(relationshipType: String?): ReferenceCodeGroup {
    val groupCodeForRelationship = when (relationshipType) {
      "S" -> ReferenceCodeGroup.SOCIAL_RELATIONSHIP
      "O" -> ReferenceCodeGroup.OFFICIAL_RELATIONSHIP
      else -> throw IllegalStateException("Unexpected relationshipType: $relationshipType")
    }
    return groupCodeForRelationship
  }

  private fun unsupportedRelationshipType(request: UpdateRelationshipRequest) {
    if (request.relationshipType.isPresent && request.relationshipType.get() == null) {
      throw ValidationException("Unsupported relationship type null.")
    }
  }

  private fun unsupportedRelationshipToPrisoner(request: UpdateRelationshipRequest) {
    if (request.relationshipToPrisoner.isPresent && request.relationshipToPrisoner.get() == null) {
      throw ValidationException("Unsupported relationship to prisoner null.")
    }
  }

  private fun unsupportedApprovedToVisitFlag(request: UpdateRelationshipRequest) {
    if (request.isApprovedVisitor.isPresent && request.isApprovedVisitor.get() == null) {
      throw ValidationException("Unsupported approved visitor value null.")
    }
  }

  private fun unsupportedEmergencyContact(request: UpdateRelationshipRequest) {
    if (request.isEmergencyContact.isPresent && request.isEmergencyContact.get() == null) {
      throw ValidationException("Unsupported emergency contact null.")
    }
  }

  private fun unsupportedNextOfKin(request: UpdateRelationshipRequest) {
    if (request.isNextOfKin.isPresent && request.isNextOfKin.get() == null) {
      throw ValidationException("Unsupported next of kin null.")
    }
  }

  private fun unsupportedRelationshipActive(request: UpdateRelationshipRequest) {
    if (request.isRelationshipActive.isPresent && request.isRelationshipActive.get() == null) {
      throw ValidationException("Unsupported relationship status null.")
    }
  }

  private fun enrichRelationship(relationship: PrisonerContactEntity): PrisonerContactRelationshipDetails = PrisonerContactRelationshipDetails(
    prisonerContactId = relationship.prisonerContactId,
    contactId = relationship.contactId,
    prisonerNumber = relationship.prisonerNumber,
    relationshipType = relationship.relationshipType,
    relationshipTypeDescription = referenceCodeService.getReferenceDataByGroupAndCode(
      ReferenceCodeGroup.RELATIONSHIP_TYPE,
      relationship.relationshipType,
    )?.description ?: relationship.relationshipType,
    relationshipToPrisonerCode = relationship.relationshipToPrisoner,
    relationshipToPrisonerDescription = referenceCodeService.getReferenceDataByGroupAndCode(
      referenceCodeGroupForRelationshipType(relationship.relationshipType),
      relationship.relationshipToPrisoner,
    )?.description ?: relationship.relationshipToPrisoner,
    emergencyContact = relationship.emergencyContact,
    nextOfKin = relationship.nextOfKin,
    isRelationshipActive = relationship.active,
    isApprovedVisitor = relationship.approvedVisitor,
    comments = relationship.comments,
  )
}
