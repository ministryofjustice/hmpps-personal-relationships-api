package uk.gov.justice.digital.hmpps.personalrelationships.service

import jakarta.persistence.EntityNotFoundException
import jakarta.transaction.Transactional
import jakarta.validation.ValidationException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.personalrelationships.config.User
import uk.gov.justice.digital.hmpps.personalrelationships.entity.ContactAddressPhoneEntity
import uk.gov.justice.digital.hmpps.personalrelationships.entity.ContactEntity
import uk.gov.justice.digital.hmpps.personalrelationships.entity.DeletedPrisonerContactEntity
import uk.gov.justice.digital.hmpps.personalrelationships.entity.PrisonerContactEntity
import uk.gov.justice.digital.hmpps.personalrelationships.exception.DuplicateRelationshipException
import uk.gov.justice.digital.hmpps.personalrelationships.exception.RelationshipCannotBeRemovedDueToDependencyException
import uk.gov.justice.digital.hmpps.personalrelationships.mapping.toEntity
import uk.gov.justice.digital.hmpps.personalrelationships.mapping.toModel
import uk.gov.justice.digital.hmpps.personalrelationships.model.ReferenceCodeGroup
import uk.gov.justice.digital.hmpps.personalrelationships.model.internal.DeletedRelationshipIds
import uk.gov.justice.digital.hmpps.personalrelationships.model.internal.DeletedResponse
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.AddContactRelationshipRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.ContactRelationship
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.CreateContactRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.PatchRelationshipRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.address.Address
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.address.CreateContactAddressRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.identity.CreateMultipleIdentitiesRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.ContactAddressPhoneDetails
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.ContactAuditEntry
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.ContactCreationResult
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.ContactDetails
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.ContactNameDetails
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.ContactPhoneDetails
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.PrisonerContactRelationshipDetails
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.ReferenceCode
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.RelationshipDeletePlan
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.RelationshipsApproved
import uk.gov.justice.digital.hmpps.personalrelationships.repository.ContactAddressDetailsRepository
import uk.gov.justice.digital.hmpps.personalrelationships.repository.ContactAddressPhoneRepository
import uk.gov.justice.digital.hmpps.personalrelationships.repository.ContactAuditHistoryRepository
import uk.gov.justice.digital.hmpps.personalrelationships.repository.ContactEmailRepository
import uk.gov.justice.digital.hmpps.personalrelationships.repository.ContactIdentityDetailsRepository
import uk.gov.justice.digital.hmpps.personalrelationships.repository.ContactPhoneDetailsRepository
import uk.gov.justice.digital.hmpps.personalrelationships.repository.ContactRepository
import uk.gov.justice.digital.hmpps.personalrelationships.repository.DeletedPrisonerContactRepository
import uk.gov.justice.digital.hmpps.personalrelationships.repository.PrisonerContactRepository
import uk.gov.justice.digital.hmpps.personalrelationships.repository.PrisonerContactRestrictionRepository
import java.time.LocalDateTime
import kotlin.jvm.optionals.getOrNull

@Service
class ContactService(
  private val contactRepository: ContactRepository,
  private val prisonerContactRepository: PrisonerContactRepository,
  private val prisonerService: PrisonerService,
  private val contactAddressDetailsRepository: ContactAddressDetailsRepository,
  private val contactPhoneDetailsRepository: ContactPhoneDetailsRepository,
  private val contactAddressPhoneRepository: ContactAddressPhoneRepository,
  private val contactEmailRepository: ContactEmailRepository,
  private val contactIdentityDetailsRepository: ContactIdentityDetailsRepository,
  private val referenceCodeService: ReferenceCodeService,
  private val employmentService: EmploymentService,
  private val contactIdentityService: ContactIdentityService,
  private val contactAddressService: ContactAddressService,
  private val contactPhoneService: ContactPhoneService,
  private val contactEmailService: ContactEmailService,
  private val prisonerContactRestrictionRepository: PrisonerContactRestrictionRepository,
  private val deletedPrisonerContactRepository: DeletedPrisonerContactRepository,
  private val contactAuditHistoryRepository: ContactAuditHistoryRepository,
) {
  companion object {
    private val logger = LoggerFactory.getLogger(this::class.java)
  }

  private val internalOfficialTypes = listOf("POM", "COM", "CA", "RO", "CUSPO", "CUSPO2", "OFS", "PPA", "PROB")

  @Transactional
  fun createContact(request: CreateContactRequest, user: User): ContactCreationResult {
    if (request.relationship != null) {
      validateNewRelationship(request.relationship)
    }
    validateOptionalCode(request.titleCode, ReferenceCodeGroup.TITLE)
    validateOptionalCode(request.genderCode, ReferenceCodeGroup.GENDER)
    validateOptionalCode(request.languageCode, ReferenceCodeGroup.LANGUAGE)
    validateOptionalCode(request.domesticStatusCode, ReferenceCodeGroup.DOMESTIC_STS)

    val newContact = request.toModel(user)
    val createdContact = contactRepository.saveAndFlush(newContact)
    val newRelationship = request.relationship?.toEntity(createdContact.id(), user.username)
      ?.let { prisonerContactRepository.saveAndFlush(it) }

    createIdentityInformation(createdContact, request, user)
    createAddresses(createdContact.id(), request.addresses, user)
    createPhoneNumbers(request, createdContact, user)
    createEmailAddresses(request, createdContact, user)
    createEmployments(request, createdContact, user)

    logger.info("Created new contact {}", createdContact)
    newRelationship?.let { logger.info("Created new relationship {}", newRelationship) }
    return ContactCreationResult(
      enrichContact(createdContact),
      newRelationship?.let { enrichRelationship(newRelationship) },
    )
  }

  private fun createPhoneNumbers(
    request: CreateContactRequest,
    createdContact: ContactEntity,
    user: User,
  ) {
    if (request.phoneNumbers.isNotEmpty()) {
      contactPhoneService.createMultiple(createdContact.id(), user.username, request.phoneNumbers)
    }
  }

  private fun createEmailAddresses(
    request: CreateContactRequest,
    createdContact: ContactEntity,
    user: User,
  ) {
    if (request.emailAddresses.isNotEmpty()) {
      contactEmailService.createMultiple(createdContact.id(), user.username, request.emailAddresses)
    }
  }

  private fun createEmployments(
    request: CreateContactRequest,
    createdContact: ContactEntity,
    user: User,
  ) {
    request.employments.forEach { employment ->
      employmentService.createEmployment(
        createdContact.id(),
        employment.organisationId,
        employment.isActive,
        user.username,
      )
    }
  }

  fun getContact(id: Long): ContactDetails? = contactRepository.findById(id).getOrNull()
    ?.let { enrichContact(it) }

  private fun createAddresses(contactId: Long, addresses: List<Address>, user: User) {
    addresses.forEach { address ->
      contactAddressService.create(
        contactId,
        CreateContactAddressRequest(
          addressType = address.addressType,
          primaryAddress = address.primaryAddress,
          flat = address.flat,
          property = address.property,
          street = address.street,
          area = address.area,
          cityCode = address.cityCode,
          countyCode = address.countyCode,
          postcode = address.postcode,
          countryCode = address.countryCode,
          verified = address.verified,
          mailFlag = address.mailFlag,
          startDate = address.startDate,
          endDate = address.endDate,
          noFixedAddress = address.noFixedAddress,
          phoneNumbers = address.phoneNumbers,
          comments = address.comments,
        ),
        user,
      )
    }
  }

  fun getContactName(id: Long): ContactNameDetails? = contactRepository.findById(id).getOrNull()
    ?.let { contactEntity ->
      ContactNameDetails(
        titleCode = contactEntity.title,
        titleDescription = contactEntity.title?.let {
          referenceCodeService.getReferenceDataByGroupAndCode(ReferenceCodeGroup.TITLE, it)?.description
        },
        lastName = contactEntity.lastName,
        firstName = contactEntity.firstName,
        middleNames = contactEntity.middleNames,
      )
    }

  @Transactional
  fun addContactRelationship(request: AddContactRelationshipRequest, user: User): PrisonerContactRelationshipDetails {
    validateNewRelationship(request.relationship)
    getContact(request.contactId) ?: throw EntityNotFoundException("Contact (${request.contactId}) could not be found")
    if (prisonerContactRepository.findDuplicateRelationships(
        request.relationship.prisonerNumber,
        request.contactId,
        request.relationship.relationshipToPrisonerCode,
      ).isNotEmpty()
    ) {
      throw DuplicateRelationshipException(
        request.relationship.prisonerNumber,
        request.contactId,
        request.relationship.relationshipToPrisonerCode,
      )
    }
    val newRelationship = request.relationship.toEntity(request.contactId, user.username)
    prisonerContactRepository.saveAndFlush(newRelationship)
    return enrichRelationship(newRelationship)
  }

  private fun validateOptionalCode(code: String?, group: ReferenceCodeGroup): ReferenceCode? = code?.let { referenceCodeService.validateReferenceCode(group, it, false) }

  private fun validateNewRelationship(relationship: ContactRelationship) {
    prisonerService.getPrisoner(relationship.prisonerNumber)
      ?: throw EntityNotFoundException("Prisoner (${relationship.prisonerNumber}) could not be found")
    referenceCodeService.validateReferenceCode(
      ReferenceCodeGroup.RELATIONSHIP_TYPE,
      relationship.relationshipTypeCode,
      allowInactive = false,
    )
    validateRelationshipToPrisoner(
      relationship.relationshipTypeCode,
      relationship.relationshipToPrisonerCode,
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
      titleCode = contactEntity.title,
      titleDescription = titleDescription,
      lastName = contactEntity.lastName,
      firstName = contactEntity.firstName,
      middleNames = contactEntity.middleNames,
      dateOfBirth = contactEntity.dateOfBirth,
      isStaff = contactEntity.staffFlag,
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
      genderCode = contactEntity.gender,
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
    request: PatchRelationshipRequest,
    user: User,
  ): PrisonerContactRelationshipDetails {
    val prisonerContactEntity = requirePrisonerContactEntity(prisonerContactId)

    validateRequest(request)
    validateRelationshipCodes(request, prisonerContactEntity)
    // Reject duplicate relationships. Skip this check if we're not changing the relationship type
    // so that existing duplicates from NOMIS can still be updated such as removing approve to
    // visit or active status.
    if (request.relationshipToPrisonerCode.isPresent &&
      prisonerContactRepository.findDuplicateRelationships(
        prisonerContactEntity.prisonerNumber,
        prisonerContactEntity.contactId,
        request.relationshipToPrisonerCode.get(),
      ).any { it.prisonerContactId != prisonerContactId }
    ) {
      throw DuplicateRelationshipException(
        prisonerContactEntity.prisonerNumber,
        prisonerContactEntity.contactId,
        request.relationshipToPrisonerCode.get(),
      )
    }

    val changedPrisonerContact = prisonerContactEntity.applyUpdate(request, user)

    prisonerContactRepository.saveAndFlush(changedPrisonerContact)
    return enrichRelationship(prisonerContactEntity)
  }

  @Transactional
  fun deleteContactRelationship(prisonerContactId: Long, user: User): DeletedResponse {
    val relationship = requirePrisonerContactEntity(prisonerContactId)
    if (hasPrisonerContactRestrictions(prisonerContactId)) {
      throw RelationshipCannotBeRemovedDueToDependencyException(prisonerContactId)
    }
    prisonerContactRepository.delete(relationship)

    // Find all remaining relationships for this contact (excluding the one just deleted)
    val remainingRelationships = getRemainingRelationships(relationship, prisonerContactId)

    var wasUpdated = false
    if (remainingRelationships.isNotEmpty()) {
      // If all remaining relationships are internal staff types, remove DOB
      val allInternal = getAllInternal(remainingRelationships)
      if (allInternal) {
        contactRepository.findById(relationship.contactId).ifPresent {
          contactRepository.saveAndFlush(it.deleteDateOfBirth(user))
          wasUpdated = true
        }
      }
    }

    deletedPrisonerContactRepository.saveAndFlush(
      DeletedPrisonerContactEntity(
        deletedPrisonerContactId = 0,
        prisonerContactId = relationship.prisonerContactId,
        contactId = relationship.contactId,
        prisonerNumber = relationship.prisonerNumber,
        relationshipType = relationship.relationshipType,
        relationshipToPrisoner = relationship.relationshipToPrisoner,
        nextOfKin = relationship.nextOfKin,
        emergencyContact = relationship.emergencyContact,
        comments = relationship.comments,
        active = relationship.active,
        approvedVisitor = relationship.approvedVisitor,
        currentTerm = relationship.currentTerm,
        createdBy = relationship.createdBy,
        createdTime = relationship.createdTime,
        approvedBy = relationship.approvedBy,
        approvedTime = relationship.approvedTime,
        expiryDate = relationship.expiryDate,
        createdAtPrison = relationship.createdAtPrison,
        updatedBy = relationship.updatedBy,
        updatedTime = relationship.updatedTime,
        deletedBy = user.username,
        deletedTime = LocalDateTime.now(),
      ),
    )
    return DeletedResponse(
      ids = DeletedRelationshipIds(relationship.contactId, relationship.prisonerNumber, relationship.prisonerContactId),
      wasUpdated = wasUpdated,
    )
  }

  fun assessIfRelationshipCanBeDeleted(prisonerContactId: Long): RelationshipDeletePlan {
    val relationship = requirePrisonerContactEntity(prisonerContactId)
    val hasRestrictions = hasPrisonerContactRestrictions(prisonerContactId)
    val remainingRelationships = getRemainingRelationships(relationship, prisonerContactId)

    var shouldRemoveDob = false
    if (remainingRelationships.isNotEmpty()) {
      if (isDobExists(relationship.contactId)) {
        // If all remaining relationships are internal staff types,  DOB can be removed
        shouldRemoveDob = getAllInternal(remainingRelationships)
      }
    }

    return RelationshipDeletePlan(shouldRemoveDob, hasRestrictions)
  }

  private fun hasPrisonerContactRestrictions(prisonerContactId: Long) = prisonerContactRestrictionRepository.findAllByPrisonerContactId(prisonerContactId).isNotEmpty()

  private fun isDobExists(contactId: Long): Boolean {
    val contact = contactRepository.findById(contactId).get()
    return contact.dateOfBirth != null
  }

  private fun getAllInternal(remainingRelationships: List<PrisonerContactEntity>) = remainingRelationships.all { it.relationshipToPrisoner in internalOfficialTypes }

  private fun getRemainingRelationships(
    relationship: PrisonerContactEntity,
    prisonerContactId: Long,
  ) = prisonerContactRepository.findAllByContactId(relationship.contactId)
    .filter { it.prisonerContactId != prisonerContactId }

  private fun validateRequest(request: PatchRelationshipRequest) {
    unsupportedRelationshipType(request)
    unsupportedRelationshipToPrisoner(request)
    unsupportedApprovedToVisitFlag(request)
    unsupportedEmergencyContact(request)
    unsupportedNextOfKin(request)
    unsupportedRelationshipActive(request)
  }

  private fun requirePrisonerContactEntity(prisonerContactId: Long): PrisonerContactEntity = prisonerContactRepository.findById(prisonerContactId)
    .orElseThrow { EntityNotFoundException("Prisoner contact with prisoner contact ID $prisonerContactId not found") }

  private fun PrisonerContactEntity.applyUpdate(
    request: PatchRelationshipRequest,
    user: User,
  ) = this.copy(
    contactId = this.contactId,
    prisonerNumber = this.prisonerNumber,
    relationshipType = request.relationshipTypeCode.orElse(this.relationshipType),
    approvedVisitor = request.isApprovedVisitor.orElse(this.approvedVisitor).also {
      if (request.isApprovedVisitor.isPresent && request.isApprovedVisitor.get() != this.approvedVisitor) {
        logger.info("Approval status has been changed from DPS: for contactId=${this.contactId}, prisonerNumber=${this.prisonerNumber}: from ${this.approvedVisitor} to ${request.isApprovedVisitor.get()}, updated By ${user.username}")
      }
    },
    currentTerm = this.currentTerm,
    nextOfKin = request.isNextOfKin.orElse(this.nextOfKin),
    emergencyContact = request.isEmergencyContact.orElse(this.emergencyContact),
    active = request.isRelationshipActive.orElse(this.active),
    relationshipToPrisoner = request.relationshipToPrisonerCode.orElse(this.relationshipToPrisoner),
    comments = request.comments.orElse(this.comments),
  ).also {
    // when updating a relationship only update approvedBy/approvedTime if the approvedVisitor flag is being changed
    if (request.isApprovedVisitor.isPresent && this.approvedVisitor != request.isApprovedVisitor.get()) {
      it.approvedBy = user.username
      it.approvedTime = LocalDateTime.now()
    } else {
      it.approvedBy = this.approvedBy
      it.approvedTime = this.approvedTime
    }
    it.expiryDate = this.expiryDate
    it.createdAtPrison = this.createdAtPrison
    it.updatedBy = user.username
    it.updatedTime = LocalDateTime.now()
  }

  private fun validateRelationshipCodes(
    request: PatchRelationshipRequest,
    preUpdateRelationship: PrisonerContactEntity,
  ) {
    if (request.relationshipTypeCode.isPresent && request.relationshipToPrisonerCode.isPresent) {
      // Changing both
      val relationshipType = request.relationshipTypeCode.get()
      referenceCodeService.validateReferenceCode(
        ReferenceCodeGroup.RELATIONSHIP_TYPE,
        relationshipType,
        allowInactive = true,
      )

      val relationshipToPrisoner = request.relationshipToPrisonerCode.get()
      validateRelationshipToPrisoner(relationshipType, relationshipToPrisoner, allowInactive = true)
    } else if (!request.relationshipTypeCode.isPresent && request.relationshipToPrisonerCode.isPresent) {
      // Changing only relationship to prisoner
      val relationshipType = preUpdateRelationship.relationshipType
      val relationshipToPrisoner = request.relationshipToPrisonerCode.get()
      validateRelationshipToPrisoner(relationshipType, relationshipToPrisoner, allowInactive = true)
    } else if (request.relationshipTypeCode.isPresent && !request.relationshipToPrisonerCode.isPresent) {
      // Changing only relationship type (this is only going to be valid if the type didn't actually change)
      val relationshipType = request.relationshipTypeCode.get()
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

  private fun unsupportedRelationshipType(request: PatchRelationshipRequest) {
    if (request.relationshipTypeCode.isPresent && request.relationshipTypeCode.get() == null) {
      throw ValidationException("Unsupported relationship type null.")
    }
  }

  private fun unsupportedRelationshipToPrisoner(request: PatchRelationshipRequest) {
    if (request.relationshipToPrisonerCode.isPresent && request.relationshipToPrisonerCode.get() == null) {
      throw ValidationException("Unsupported relationship to prisoner null.")
    }
  }

  private fun unsupportedApprovedToVisitFlag(request: PatchRelationshipRequest) {
    if (request.isApprovedVisitor.isPresent && request.isApprovedVisitor.get() == null) {
      throw ValidationException("Unsupported approved visitor value null.")
    }
  }

  private fun unsupportedEmergencyContact(request: PatchRelationshipRequest) {
    if (request.isEmergencyContact.isPresent && request.isEmergencyContact.get() == null) {
      throw ValidationException("Unsupported emergency contact null.")
    }
  }

  private fun unsupportedNextOfKin(request: PatchRelationshipRequest) {
    if (request.isNextOfKin.isPresent && request.isNextOfKin.get() == null) {
      throw ValidationException("Unsupported next of kin null.")
    }
  }

  private fun unsupportedRelationshipActive(request: PatchRelationshipRequest) {
    if (request.isRelationshipActive.isPresent && request.isRelationshipActive.get() == null) {
      throw ValidationException("Unsupported relationship status null.")
    }
  }

  private fun createIdentityInformation(
    createdContact: ContactEntity,
    request: CreateContactRequest,
    user: User,
  ) {
    if (request.identities.isNotEmpty()) {
      contactIdentityService.createMultiple(
        createdContact.id(),
        CreateMultipleIdentitiesRequest(identities = request.identities),
        user,
      )
    }
  }

  private fun enrichRelationship(relationship: PrisonerContactEntity): PrisonerContactRelationshipDetails = PrisonerContactRelationshipDetails(
    prisonerContactId = relationship.prisonerContactId,
    contactId = relationship.contactId,
    prisonerNumber = relationship.prisonerNumber,
    relationshipTypeCode = relationship.relationshipType,
    relationshipTypeDescription = referenceCodeService.getReferenceDataByGroupAndCode(
      ReferenceCodeGroup.RELATIONSHIP_TYPE,
      relationship.relationshipType,
    )?.description ?: relationship.relationshipType,
    relationshipToPrisonerCode = relationship.relationshipToPrisoner,
    relationshipToPrisonerDescription = referenceCodeService.getReferenceDataByGroupAndCode(
      referenceCodeGroupForRelationshipType(relationship.relationshipType),
      relationship.relationshipToPrisoner,
    )?.description ?: relationship.relationshipToPrisoner,
    isEmergencyContact = relationship.emergencyContact,
    isNextOfKin = relationship.nextOfKin,
    isApprovedVisitor = relationship.approvedVisitor,
    isRelationshipActive = relationship.active,
    comments = relationship.comments,
    approvedBy = relationship.approvedBy,
  )

  @Transactional
  fun removeInternalOfficialContactsDateOfBirth(): List<Long> {
    val relationships = prisonerContactRepository.findAllContactsWithADobInRelationships(internalOfficialTypes)
    val allContactIds = relationships.map { it.contactId }.distinct()

    // Ignore contacts which also have social or official relationship that is not in the above list
    val ignoreContactIds = relationships.mapNotNull { entry ->
      when (entry.relationshipType) {
        "S" -> entry.contactId
        "O" -> {
          if (entry.relationshipToPrisoner !in internalOfficialTypes) {
            entry.contactId
          } else {
            null
          }
        }
        else -> null
      }
    }.distinct()

    val contactIdsToUpdate = allContactIds.filterNot { it in ignoreContactIds }

    contactIdsToUpdate.forEach { contactId ->
      val contact = contactRepository.findById(contactId).orElseThrow { EntityNotFoundException("Contact ($contactId) not found to unset DOB") }
      contactRepository.saveAndFlush(contact.copy(dateOfBirth = null))
    }

    return contactIdsToUpdate
  }

  private fun ContactEntity.deleteDateOfBirth(
    user: User,
  ): ContactEntity {
    val changedContact = this.copy(
      dateOfBirth = null,
      updatedBy = user.username,
      updatedTime = LocalDateTime.now(),
    )

    return changedContact
  }

  @Transactional
  fun approveRelationships(createdByList: List<String>, daysAgo: Long): List<RelationshipsApproved> {
    val createdAfter = LocalDateTime.now().minusDays(daysAgo)

    val relationshipsSaved = prisonerContactRepository.getRelationshipsToApprove(createdAfter, createdByList).map { r ->
      val copy = r.copy(approvedVisitor = true).also {
        // this is utility function only used by system user to approve relationships in the event of reconciliation jobs
        it.approvedBy = User.SYS_USER.username
        it.approvedTime = LocalDateTime.now()
      }
      prisonerContactRepository.saveAndFlush(copy)
    }

    return relationshipsSaved.map { r ->
      RelationshipsApproved(
        contactId = r.contactId,
        prisonerContactId = r.prisonerContactId,
        prisonerNumber = r.prisonerNumber,
        approvedToVisit = r.approvedVisitor,
      )
    }
  }

  @Transactional
  fun getContactHistory(contactId: Long): List<ContactAuditEntry>? {
    if (!contactRepository.existsById(contactId)) return null
    return contactAuditHistoryRepository.getContactHistory(contactId)
  }
}
