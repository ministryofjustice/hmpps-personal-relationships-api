package uk.gov.justice.digital.hmpps.hmppscontactsapi.service.migrate

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.ContactAddressEntity
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.ContactAddressPhoneEntity
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.ContactEmailEntity
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.ContactIdentityEntity
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.ContactPhoneEntity
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.ContactRestrictionEntity
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.ContactWithFixedIdEntity
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.EmploymentEntity
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.PrisonerContactEntity
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.PrisonerContactRestrictionEntity
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.migrate.MigrateContactRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.migrate.AddressAndPhones
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.migrate.ContactsAndRestrictions
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.migrate.ElementType
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.migrate.IdPair
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.migrate.MigrateContactResponse
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.ContactAddressPhoneRepository
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.ContactAddressRepository
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.ContactEmailRepository
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.ContactIdentityRepository
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.ContactPhoneRepository
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.ContactRestrictionRepository
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.ContactWithFixedIdRepository
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.EmploymentRepository
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.PrisonerContactRepository
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.PrisonerContactRestrictionRepository
import java.time.LocalDateTime

@Service
class ContactMigrationService(
  private val contactRepository: ContactWithFixedIdRepository,
  private val contactAddressRepository: ContactAddressRepository,
  private val contactAddressPhoneRepository: ContactAddressPhoneRepository,
  private val contactPhoneRepository: ContactPhoneRepository,
  private val contactEmailRepository: ContactEmailRepository,
  private val contactIdentityRepository: ContactIdentityRepository,
  private val contactRestrictionRepository: ContactRestrictionRepository,
  private val prisonerContactRepository: PrisonerContactRepository,
  private val prisonerContactRestrictionRepository: PrisonerContactRestrictionRepository,
  private val employmentRepository: EmploymentRepository,
) {
  companion object {
    private val logger = LoggerFactory.getLogger(this::class.java)
  }

  /**
   * Accept a full contact migration object, validate and create the equivalent entities
   * in the contacts database locally.
   *
   * Do NOT emit sync events for entities created, as these will already exist in NOMIS and
   * in the mapping tables - we are not generating new copies of this data.
   *
   * Assemble a response object to contain both the NOMIS and DPS IDs for each entity and
   * sub-entity saved to allow the mapping service to link the IDs for subsequent syncs.
   *
   * The PERSON_ID from NOMIS is used as the CONTACT_ID (primary key) in Contacts. Where
   * contacts are created in DPS, this will generate new CONTACT_IDs in a different range
   * to NOMIS, starting at 20,000,000.
   */
  @Transactional
  fun migrateContact(request: MigrateContactRequest): MigrateContactResponse {
    logger.info(
      "Migrate PERSON ID {} with {} addresses, {} phones, {} emails, {} identities, {} restrictions, {} employments, {} relationships",
      request.personId,
      request.addresses.size,
      request.phoneNumbers.size,
      request.emailAddresses.size,
      request.identifiers.size,
      request.restrictions.size,
      request.employments.size,
      request.contacts.size,
    )

    // Extract, transform and save the different entities from the request, returning the NOMIS and DPS IDs for each
    val contactPair = extractAndSaveContact(request)
    val contactId = contactPair.second.contactId
    val phoneNumberPairs = extractAndSavePhones(request, contactId)
    val addressPairs = extractAndSaveAddresses(request, contactId)
    val addressPhonePairs = extractAndSaveAddressPhones(request, contactId, addressPairs)
    val emailPairs = extractAndSaveEmails(request, contactId)
    val identityPairs = extractAndSaveIdentities(request, contactId)
    val restrictionPairs = extractAndSaveRestrictions(request, contactId)
    val employmentPairs = extractAndSaveEmployments(request, contactId)
    val prisonerContactPairs = extractAndSavePrisonerContacts(request, contactId)
    val prisonerContactRestrictionPairs =
      extractAndSavePrisonerContactRestrictions(request, contactId, prisonerContactPairs)

    return MigrateContactResponse(
      contact = IdPair(ElementType.CONTACT, contactPair.first, contactPair.second.contactId),
      lastName = contactPair.second.lastName,
      dateOfBirth = contactPair.second.dateOfBirth,
      phoneNumbers = phoneNumberPairs.map { IdPair(ElementType.PHONE, it.first, it.second.contactPhoneId) },
      addresses = buildAddressesAndPhonesResponse(addressPairs, addressPhonePairs),
      emailAddresses = emailPairs.map { IdPair(ElementType.EMAIL, it.first, it.second.contactEmailId) },
      identities = identityPairs.map { IdPair(ElementType.IDENTITY, it.first, it.second.contactIdentityId) },
      restrictions = restrictionPairs.map { IdPair(ElementType.RESTRICTION, it.first, it.second.contactRestrictionId) },
      relationships = buildContactsAndRestrictionsResponse(prisonerContactPairs, prisonerContactRestrictionPairs),
      employments = employmentPairs.map { IdPair(ElementType.EMPLOYMENT, it.first, it.second.employmentId) },
    )
  }

  fun extractAndSaveContact(req: MigrateContactRequest): Pair<Long, ContactWithFixedIdEntity> {
    if (contactRepository.existsById(req.personId)) {
      logger.info("Migration: Duplicate person ID received ${req.personId} - replacing it")
      removeExistingContactAndDetail(req.personId)
    }

    return Pair(
      req.personId,
      contactRepository.save(
        ContactWithFixedIdEntity(
          contactId = req.personId,
          title = req.title?.code,
          lastName = req.lastName,
          middleNames = req.middleName,
          firstName = req.firstName,
          dateOfBirth = req.dateOfBirth,
          deceasedDate = req.deceasedDate,
          createdBy = req.createUsername ?: "MIGRATION",
          createdTime = req.createDateTime ?: LocalDateTime.now(),
          staffFlag = req.staff,
          remitterFlag = req.remitter,
          gender = req.gender?.code,
          languageCode = req.language?.code,
          domesticStatus = req.domesticStatus?.code,
          interpreterRequired = req.interpreterRequired,
          updatedBy = req.modifyUsername,
          updatedTime = req.modifyDateTime,
        ),
      ),
    )
  }

  fun extractAndSavePhones(req: MigrateContactRequest, contactId: Long): List<Pair<Long, ContactPhoneEntity>> = req.phoneNumbers.map { requestPhone ->
    Pair(
      requestPhone.phoneId,
      contactPhoneRepository.save(
        ContactPhoneEntity(
          contactPhoneId = 0L,
          contactId = contactId,
          phoneType = requestPhone.type.code,
          phoneNumber = requestPhone.number,
          extNumber = requestPhone.extension,
          createdBy = requestPhone.createUsername ?: "MIGRATION",
          createdTime = requestPhone.createDateTime ?: LocalDateTime.now(),
          updatedBy = requestPhone.modifyUsername,
          updatedTime = requestPhone.modifyDateTime,
        ),
      ),
    )
  }

  fun extractAndSaveAddresses(req: MigrateContactRequest, contactId: Long): List<Pair<Long, ContactAddressEntity>> = req.addresses.map { addr ->
    Pair(
      addr.addressId,
      contactAddressRepository.save(
        ContactAddressEntity(
          contactAddressId = 0L,
          contactId = contactId,
          addressType = addr.type?.code,
          primaryAddress = addr.primaryAddress,
          flat = addr.flat,
          property = addr.premise,
          street = addr.street,
          area = addr.locality,
          cityCode = addr.city?.code,
          countyCode = addr.county?.code,
          postCode = addr.postCode,
          countryCode = addr.country?.code,
          verified = addr.validatedPAF,
          mailFlag = addr.mailAddress,
          startDate = addr.startDate,
          endDate = addr.endDate,
          noFixedAddress = addr.noFixedAddress,
          comments = addr.comment,
          createdBy = addr.createUsername ?: "MIGRATION",
          createdTime = addr.createDateTime ?: LocalDateTime.now(),
        ).also {
          it.updatedBy = addr.modifyUsername
          it.updatedTime = addr.modifyDateTime
        },
      ),
    )
  }

  fun extractAndSaveAddressPhones(
    req: MigrateContactRequest,
    contactId: Long,
    contactAddresses: List<Pair<Long, ContactAddressEntity>>,
  ): List<Pair<Long, List<Pair<Long, ContactAddressPhoneEntity>>>> {
    val phones = req.addresses.map { addr ->
      val thisContactAddress = contactAddresses.find { it.first == addr.addressId }

      Pair(
        addr.addressId,
        addr.phoneNumbers.map { phone ->
          val contactPhone = contactPhoneRepository.save(
            ContactPhoneEntity(
              contactPhoneId = 0L,
              contactId = contactId,
              phoneType = phone.type.code,
              phoneNumber = phone.number,
              extNumber = phone.extension,
              createdBy = phone.createUsername ?: "MIGRATION",
              createdTime = phone.createDateTime ?: LocalDateTime.now(),
              updatedBy = phone.modifyUsername,
              updatedTime = phone.modifyDateTime,
            ),
          )

          Pair(
            phone.phoneId,
            contactAddressPhoneRepository.save(
              ContactAddressPhoneEntity(
                contactAddressPhoneId = 0L,
                contactId = contactId,
                contactAddressId = thisContactAddress!!.second.contactAddressId,
                contactPhoneId = contactPhone.contactPhoneId,
                createdBy = phone.createUsername ?: "MIGRATION",
                createdTime = phone.createDateTime ?: LocalDateTime.now(),
                updatedBy = phone.modifyUsername,
                updatedTime = phone.modifyDateTime,
              ),
            ),
          )
        },
      )
    }
    return phones
  }

  fun extractAndSaveEmails(req: MigrateContactRequest, contactId: Long): List<Pair<Long, ContactEmailEntity>> = req.emailAddresses.map { requestEmail ->
    Pair(
      requestEmail.emailAddressId,
      contactEmailRepository.save(
        ContactEmailEntity(
          contactEmailId = 0L,
          contactId = contactId,
          emailAddress = requestEmail.email,
          createdBy = requestEmail.createUsername ?: "MIGRATION",
          createdTime = requestEmail.createDateTime ?: LocalDateTime.now(),
          updatedBy = requestEmail.modifyUsername,
          updatedTime = requestEmail.modifyDateTime,
        ),
      ),
    )
  }

  fun extractAndSaveIdentities(req: MigrateContactRequest, contactId: Long): List<Pair<Long, ContactIdentityEntity>> = req.identifiers.map { requestIdentifier ->
    Pair(
      requestIdentifier.sequence,
      contactIdentityRepository.save(
        ContactIdentityEntity(
          contactIdentityId = 0L,
          contactId = contactId,
          identityType = requestIdentifier.type.code,
          identityValue = requestIdentifier.identifier,
          issuingAuthority = requestIdentifier.issuedAuthority,
          createdBy = requestIdentifier.createUsername ?: "MIGRATION",
          createdTime = requestIdentifier.createDateTime ?: LocalDateTime.now(),
          updatedBy = requestIdentifier.modifyUsername,
          updatedTime = requestIdentifier.modifyDateTime,
        ),
      ),
    )
  }

  fun extractAndSaveRestrictions(
    req: MigrateContactRequest,
    contactId: Long,
  ): List<Pair<Long, ContactRestrictionEntity>> = req.restrictions.map { restriction ->
    Pair(
      restriction.id,
      contactRestrictionRepository.save(
        ContactRestrictionEntity(
          contactRestrictionId = 0L,
          contactId = contactId,
          restrictionType = restriction.type.code,
          startDate = restriction.effectiveDate,
          expiryDate = restriction.expiryDate,
          comments = restriction.comment,
          createdBy = restriction.createUsername ?: "MIGRATION",
          createdTime = restriction.createDateTime ?: LocalDateTime.now(),
          updatedBy = restriction.modifyUsername,
          updatedTime = restriction.modifyDateTime,
        ),
      ),
    )
  }

  fun extractAndSaveEmployments(
    req: MigrateContactRequest,
    contactId: Long,
  ): List<Pair<Long, EmploymentEntity>> = req.employments.map { employment ->
    Pair(
      employment.sequence,
      employmentRepository.save(
        EmploymentEntity(
          employmentId = 0L,
          contactId = contactId,
          organisationId = employment.corporate.id,
          active = employment.active,
          createdBy = employment.createUsername ?: "MIGRATION",
          createdTime = employment.createDateTime ?: LocalDateTime.now(),
          updatedBy = employment.modifyUsername,
          updatedTime = employment.modifyDateTime,
        ),
      ),
    )
  }

  fun buildAddressesAndPhonesResponse(
    addresses: List<Pair<Long, ContactAddressEntity>>,
    addressPhones: List<Pair<Long, List<Pair<Long, ContactAddressPhoneEntity>>>>,
  ) = addresses.map { addr ->
    val phonesForThisAddress = addressPhones.filter { it.first == addr.first }
    phonesForThisAddress.map { phone ->
      AddressAndPhones(
        address = IdPair(ElementType.ADDRESS, addr.first, addr.second.contactAddressId),
        phones = phone.second.map {
          IdPair(ElementType.ADDRESS_PHONE, it.first, it.second.contactAddressPhoneId)
        },
      )
    }
  }.flatten()

  fun buildContactsAndRestrictionsResponse(
    contacts: List<Pair<Long, PrisonerContactEntity>>,
    restrictions: List<Pair<Long, List<Pair<Long, PrisonerContactRestrictionEntity>>>>,
  ) = contacts.map { contact ->
    val restrictionsForThisContact = restrictions.filter { it.first == contact.first }
    restrictionsForThisContact.map { restriction ->
      ContactsAndRestrictions(
        relationship = IdPair(ElementType.PRISONER_CONTACT, contact.first, contact.second.prisonerContactId),
        restrictions = restriction.second.map {
          IdPair(ElementType.PRISONER_CONTACT_RESTRICTION, it.first, it.second.prisonerContactRestrictionId)
        },
      )
    }
  }.flatten()

  fun extractAndSavePrisonerContacts(req: MigrateContactRequest, contactId: Long) = req.contacts.map { relationship ->
    Pair(
      relationship.id,
      prisonerContactRepository.save(
        PrisonerContactEntity(
          prisonerContactId = 0L,
          contactId = contactId,
          prisonerNumber = relationship.prisonerNumber,
          relationshipType = relationship.contactType.code,
          relationshipToPrisoner = relationship.relationshipType.code,
          nextOfKin = relationship.nextOfKin,
          emergencyContact = relationship.emergencyContact,
          comments = relationship.comment,
          active = relationship.active,
          approvedVisitor = relationship.approvedVisitor,
          currentTerm = relationship.currentTerm,
          createdBy = relationship.createUsername ?: "MIGRATION",
          createdTime = relationship.createDateTime ?: LocalDateTime.now(),
        ).also {
          // with creating a contact via migration, always set approvedBy and approvedTime
          it.approvedBy = relationship.createUsername ?: "MIGRATION"
          it.approvedTime = relationship.createDateTime ?: LocalDateTime.now()
          it.updatedBy = relationship.modifyUsername
          it.updatedTime = relationship.modifyDateTime
          it.expiryDate = relationship.expiryDate
        },
      ),
    )
  }

  fun extractAndSavePrisonerContactRestrictions(
    req: MigrateContactRequest,
    contactId: Long,
    prisonerContactPairs: List<Pair<Long, PrisonerContactEntity>>,
  ) = req.contacts.map { relationship ->

    // We need to know the saved prisonerContactId for each of the relationship
    val thisRelationship = prisonerContactPairs.find { it.first == relationship.id }

    Pair(
      relationship.id,
      relationship.restrictions.map { restriction ->
        Pair(
          restriction.id,
          prisonerContactRestrictionRepository.save(
            PrisonerContactRestrictionEntity(
              prisonerContactRestrictionId = 0L,
              prisonerContactId = thisRelationship!!.second.prisonerContactId,
              restrictionType = restriction.restrictionType.code,
              startDate = restriction.startDate,
              expiryDate = restriction.expiryDate,
              comments = restriction.comment,
              createdBy = restriction.createUsername ?: "MIGRATION",
              createdTime = restriction.createDateTime ?: LocalDateTime.now(),
              updatedBy = restriction.modifyUsername,
              updatedTime = restriction.modifyDateTime,
            ),
          ),
        )
      },
    )
  }

  fun removeExistingContactAndDetail(contactId: Long) {
    contactAddressPhoneRepository.deleteAllByContactId(contactId)
    contactAddressRepository.deleteAllByContactId(contactId)
    contactPhoneRepository.deleteAllByContactId(contactId)
    contactEmailRepository.deleteAllByContactId(contactId)
    contactIdentityRepository.deleteAllByContactId(contactId)
    contactRestrictionRepository.deleteAllByContactId(contactId)
    employmentRepository.deleteAllByContactId(contactId)
    prisonerContactRestrictionRepository.deleteAllByContactId(contactId)
    prisonerContactRepository.deleteAllByContactId(contactId)
    contactRepository.deleteAllByContactId(contactId)
  }
}
