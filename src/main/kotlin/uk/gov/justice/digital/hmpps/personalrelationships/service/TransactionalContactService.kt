package uk.gov.justice.digital.hmpps.personalrelationships.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.personalrelationships.config.User
import uk.gov.justice.digital.hmpps.personalrelationships.entity.ContactEntity
import uk.gov.justice.digital.hmpps.personalrelationships.mapping.toEntity
import uk.gov.justice.digital.hmpps.personalrelationships.mapping.toModel
import uk.gov.justice.digital.hmpps.personalrelationships.model.internal.TransactionalContactCreationResult
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.CreateContactRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.address.Address
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.address.CreateContactAddressRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.identity.CreateMultipleIdentitiesRequest
import uk.gov.justice.digital.hmpps.personalrelationships.repository.ContactAddressDetailsRepository
import uk.gov.justice.digital.hmpps.personalrelationships.repository.ContactAddressPhoneRepository
import uk.gov.justice.digital.hmpps.personalrelationships.repository.ContactEmailRepository
import uk.gov.justice.digital.hmpps.personalrelationships.repository.ContactIdentityDetailsRepository
import uk.gov.justice.digital.hmpps.personalrelationships.repository.ContactPhoneDetailsRepository
import uk.gov.justice.digital.hmpps.personalrelationships.repository.ContactRepository
import uk.gov.justice.digital.hmpps.personalrelationships.repository.PrisonerContactRepository

@Service
class TransactionalContactService(
  private val contactRepository: ContactRepository,
  private val prisonerContactRepository: PrisonerContactRepository,
  private val contactIdentityService: ContactIdentityService,
  private val contactAddressService: ContactAddressService,
  private val contactPhoneService: ContactPhoneService,
  private val contactEmailService: ContactEmailService,
  private val transactionalEmploymentService: TransactionalEmploymentService,
  private val contactAddressDetailsRepository: ContactAddressDetailsRepository,
  private val contactAddressPhoneRepository: ContactAddressPhoneRepository,
  private val contactPhoneDetailsRepository: ContactPhoneDetailsRepository,
  private val contactEmailRepository: ContactEmailRepository,
  private val contactIdentityDetailsRepository: ContactIdentityDetailsRepository,
) {

  @Transactional
  fun createContact(request: CreateContactRequest, user: User): TransactionalContactCreationResult {
    val createdContact = contactRepository.saveAndFlush(request.toModel(user))
    val createdRelationship = request.relationship?.toEntity(createdContact.id(), user.username)
      ?.let { prisonerContactRepository.saveAndFlush(it) }

    createIdentityInformation(createdContact, request, user)
    createAddresses(createdContact.id(), request.addresses, user)
    createPhoneNumbers(request, createdContact, user)
    createEmailAddresses(request, createdContact, user)
    createEmployments(request, createdContact, user)

    return TransactionalContactCreationResult(
      contact = createdContact,
      relationship = createdRelationship,
      addresses = contactAddressDetailsRepository.findByContactId(createdContact.id()),
      addressPhones = contactAddressPhoneRepository.findByContactId(createdContact.id()),
      phones = contactPhoneDetailsRepository.findByContactId(createdContact.id()),
      emails = contactEmailRepository.findByContactId(createdContact.id()),
      identities = contactIdentityDetailsRepository.findByContactId(createdContact.id()),
      employments = transactionalEmploymentService.getEmploymentEntities(createdContact.id()),
    )
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
      transactionalEmploymentService.createEmployment(
        createdContact.id(),
        employment.organisationId,
        employment.isActive,
        user.username,
      )
    }
  }
}
