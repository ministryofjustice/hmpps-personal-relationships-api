package uk.gov.justice.digital.hmpps.hmppscontactsapi.service.sync

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.ContactAddressPhoneEntity
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.ContactPhoneEntity
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.sync.ReconcileAddress
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.sync.ReconcileAddressPhone
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.sync.ReconcileEmail
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.sync.ReconcileEmployment
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.sync.ReconcileIdentity
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.sync.ReconcilePhone
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.sync.ReconcilePrisonerRelationship
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.sync.ReconcileRelationship
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.sync.ReconcileRelationshipRestriction
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.sync.ReconcileRestriction
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.sync.SyncContactReconcile
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.sync.SyncPrisonerReconcile
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

@Transactional(readOnly = true)
@Service
class SyncContactReconciliationService(
  private val contactRepository: ContactWithFixedIdRepository,
  private val contactEmailRepository: ContactEmailRepository,
  private val contactEmploymentRepository: EmploymentRepository,
  private val contactIdentityRepository: ContactIdentityRepository,
  private val contactPhoneRepository: ContactPhoneRepository,
  private val contactAddressRepository: ContactAddressRepository,
  private val contactAddressPhoneRepository: ContactAddressPhoneRepository,
  private val contactRestrictionRepository: ContactRestrictionRepository,
  private val prisonerContactRepository: PrisonerContactRepository,
  private val prisonerContactRestrictionRepository: PrisonerContactRestrictionRepository,
) {
  fun getContactDetailsById(contactId: Long): SyncContactReconcile {
    val contactEntity = contactRepository.findById(contactId)
      .orElseThrow { EntityNotFoundException("Contact with ID $contactId not found") }

    // Get all the phone numbers and address phone numbers for this contact
    val phoneNumbers = contactPhoneRepository.findByContactId(contactId)
    val addressPhoneNumbers = contactAddressPhoneRepository.findByContactId(contactEntity.id())

    // Filter address-specific phone numbers out of the "global" phone number list
    val globalPhoneNumbers = phoneNumbers.filterNot { phone ->
      addressPhoneNumbers.any { addressPhone -> addressPhone.contactPhoneId == phone.contactPhoneId }
    }

    return SyncContactReconcile(
      contactId = contactEntity.id(),
      firstName = contactEntity.firstName,
      lastName = contactEntity.lastName,
      middleNames = contactEntity?.middleNames,
      dateOfBirth = contactEntity?.dateOfBirth,
      staffFlag = contactEntity.staffFlag,
      phones = globalPhoneNumbers.map { phone ->
        ReconcilePhone(
          contactPhoneId = phone.contactPhoneId,
          phoneType = phone.phoneType,
          phoneNumber = phone.phoneNumber,
          extNumber = phone.extNumber,
        )
      },
      addresses = contactAddressRepository.findByContactId(contactId).map { address ->
        ReconcileAddress(
          contactAddressId = address.contactAddressId,
          addressType = address.addressType,
          primaryAddress = address.primaryAddress,
          property = address.property,
          street = address.street,
          area = address.area,
          addressPhones = getAddressPhoneNumbers(address.contactAddressId, addressPhoneNumbers, phoneNumbers),
        )
      },
      emails = contactEmailRepository.findByContactId(contactId).map { email ->
        ReconcileEmail(
          contactEmailId = email.contactEmailId,
          emailAddress = email.emailAddress,
        )
      },
      identities = contactIdentityRepository.findByContactId(contactId).map { identity ->
        ReconcileIdentity(
          contactIdentityId = identity.contactIdentityId,
          identityType = identity.identityType,
          identityValue = identity.identityValue,
          issuingAuthority = identity.issuingAuthority,
        )
      },
      restrictions = contactRestrictionRepository.findByContactId(contactId).map { restriction ->
        ReconcileRestriction(
          contactRestrictionId = restriction.contactRestrictionId,
          restrictionType = restriction.restrictionType,
          startDate = restriction.startDate,
          expiryDate = restriction.expiryDate,
        )
      },
      employments = contactEmploymentRepository.findByContactId(contactId).map { employment ->
        ReconcileEmployment(
          employmentId = employment.employmentId,
          organisationId = employment.organisationId,
          active = employment.active,
        )
      },
      relationships = prisonerContactRepository.findAllByContactId(contactId)
        .filter { it.currentTerm }
        .map { relationship ->
          ReconcileRelationship(
            prisonerContactId = relationship.prisonerContactId,
            prisonerNumber = relationship.prisonerNumber,
            contactType = relationship.relationshipType,
            relationshipType = relationship.relationshipToPrisoner,
            nextOfKin = relationship.nextOfKin,
            emergencyContact = relationship.emergencyContact,
            approvedVisitor = relationship.approvedVisitor,
            active = relationship.active,
            relationshipRestrictions = prisonerContactRestrictionRepository.findAllByPrisonerContactId(relationship.prisonerContactId)
              .map { restriction ->
                ReconcileRelationshipRestriction(
                  prisonerContactRestrictionId = restriction.prisonerContactRestrictionId,
                  restrictionType = restriction.restrictionType,
                  startDate = restriction.startDate,
                  expiryDate = restriction.expiryDate,
                )
              },
          )
        },
    )
  }

  fun getContactsByPrisonerNumber(prisonerNumber: String): SyncPrisonerReconcile {
    val relationships = prisonerContactRepository.findAllByPrisonerNumber(prisonerNumber)
      .filter { it.currentTerm }
      .map { relationship ->
        val contactEntity = contactRepository.findById(relationship.contactId)
          .orElseThrow { EntityNotFoundException("Contact with ID ${relationship.contactId} not found. Reconcile for $prisonerNumber, prisonerContactId ${relationship.prisonerContactId}") }

        ReconcilePrisonerRelationship(
          contactId = relationship.contactId,
          prisonerContactId = relationship.prisonerContactId,
          firstName = contactEntity.firstName,
          lastName = contactEntity.lastName,
          prisonerNumber = relationship.prisonerNumber,
          relationshipTypeCode = relationship.relationshipType,
          relationshipToPrisoner = relationship.relationshipToPrisoner,
          nextOfKin = relationship.nextOfKin,
          emergencyContact = relationship.emergencyContact,
          approvedVisitor = relationship.approvedVisitor,
          active = relationship.active,
          restrictions = prisonerContactRestrictionRepository.findAllByPrisonerContactId(relationship.prisonerContactId)
            .map { restriction ->
              ReconcileRelationshipRestriction(
                prisonerContactRestrictionId = restriction.prisonerContactRestrictionId,
                restrictionType = restriction.restrictionType,
                startDate = restriction.startDate,
                expiryDate = restriction.expiryDate,
              )
            },
        )
      }

    return SyncPrisonerReconcile(relationships)
  }

  private fun getAddressPhoneNumbers(
    contactAddressId: Long,
    addressPhoneNumbers: List<ContactAddressPhoneEntity>,
    phoneNumbers: List<ContactPhoneEntity>,
  ): List<ReconcileAddressPhone> = addressPhoneNumbers.filter { it.contactAddressId == contactAddressId }
    .mapNotNull { addressPhone ->
      phoneNumbers.find { it.contactPhoneId == addressPhone.contactPhoneId }?.let { phoneNumber ->
        ReconcileAddressPhone(
          contactAddressPhoneId = addressPhone.contactAddressPhoneId,
          phoneType = phoneNumber.phoneType,
          phoneNumber = phoneNumber.phoneNumber,
          extNumber = phoneNumber.extNumber,
        )
      }
    }
}
