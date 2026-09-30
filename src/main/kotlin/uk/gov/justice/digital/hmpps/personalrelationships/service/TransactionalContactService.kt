package uk.gov.justice.digital.hmpps.personalrelationships.service

import jakarta.transaction.Transactional
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.personalrelationships.config.User
import uk.gov.justice.digital.hmpps.personalrelationships.entity.ContactAddressPhoneEntity
import uk.gov.justice.digital.hmpps.personalrelationships.entity.ContactEntity
import uk.gov.justice.digital.hmpps.personalrelationships.entity.PrisonerContactEntity
import uk.gov.justice.digital.hmpps.personalrelationships.mapping.toEntity
import uk.gov.justice.digital.hmpps.personalrelationships.mapping.toModel
import uk.gov.justice.digital.hmpps.personalrelationships.model.ReferenceCodeGroup
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.ContactRelationship
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.CreateContactRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.address.Address
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.address.CreateContactAddressRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.identity.CreateMultipleIdentitiesRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.ContactAddressPhoneDetails
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.ContactCreationResult
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.ContactDetails
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.ContactPhoneDetails
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.PrisonerContactRelationshipDetails
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.ReferenceCode
import uk.gov.justice.digital.hmpps.personalrelationships.repository.ContactAddressDetailsRepository
import uk.gov.justice.digital.hmpps.personalrelationships.repository.ContactAddressPhoneRepository
import uk.gov.justice.digital.hmpps.personalrelationships.repository.ContactAuditHistoryRepository
import uk.gov.justice.digital.hmpps.personalrelationships.repository.ContactEmailRepository
import uk.gov.justice.digital.hmpps.personalrelationships.repository.ContactIdentityDetailsRepository
import uk.gov.justice.digital.hmpps.personalrelationships.repository.ContactIdentityRepository
import uk.gov.justice.digital.hmpps.personalrelationships.repository.ContactPhoneDetailsRepository
import uk.gov.justice.digital.hmpps.personalrelationships.repository.ContactRepository
import uk.gov.justice.digital.hmpps.personalrelationships.repository.DeletedPrisonerContactRepository
import uk.gov.justice.digital.hmpps.personalrelationships.repository.PrisonerContactRepository
import uk.gov.justice.digital.hmpps.personalrelationships.repository.PrisonerContactRestrictionRepository
import kotlin.collections.forEach

@Service
class TransactionalContactService(
  private val contactAddressService: ContactAddressService,
  private val contactPhoneService: ContactPhoneService,
  private val contactEmailService: ContactEmailService,
  private val contactIdentityService: ContactIdentityService,
  private val contactRepository: ContactRepository,
  private val prisonerContactRepository: PrisonerContactRepository,
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
  fun createContact(request: CreateContactRequest, user: User): ContactCreationResult {
    validateNewRelationship(request.relationship!!)

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

  private fun validateNewRelationship(relationship: ContactRelationship) {
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

  // <editor-fold desc="Methods: Validation">

  private fun validateOptionalCode(code: String?, group: ReferenceCodeGroup): ReferenceCode? = code?.let { referenceCodeService.validateReferenceCode(group, it, false) }

  // </editor-fold>

  // <editor-fold desc="Methods: Create contact information">

  //TODO: Does this make API calls?
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

  //TODO: Does this make API calls?
  private fun createPhoneNumbers(
    request: CreateContactRequest,
    createdContact: ContactEntity,
    user: User,
  ) {
    if (request.phoneNumbers.isNotEmpty()) {
      contactPhoneService.createMultiple(createdContact.id(), user.username, request.phoneNumbers)
    }
  }

  //TODO: Does this make API calls?
  private fun createEmailAddresses(
    request: CreateContactRequest,
    createdContact: ContactEntity,
    user: User,
  ) {
    if (request.emailAddresses.isNotEmpty()) {
      contactEmailService.createMultiple(createdContact.id(), user.username, request.emailAddresses)
    }
  }

  // TODO: THIS MAKES API CALLS!!!!
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

  // </editor-fold>


  // <editor-fold desc="Methods: Enrich data">

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

// </editor-fold>

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
}
