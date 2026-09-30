package uk.gov.justice.digital.hmpps.personalrelationships.service

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personalrelationships.client.organisationsapi.model.OrganisationSummary
import uk.gov.justice.digital.hmpps.personalrelationships.entity.ContactAddressPhoneEntity
import uk.gov.justice.digital.hmpps.personalrelationships.entity.ContactEntity
import uk.gov.justice.digital.hmpps.personalrelationships.entity.EmploymentEntity
import uk.gov.justice.digital.hmpps.personalrelationships.entity.PrisonerContactEntity
import uk.gov.justice.digital.hmpps.personalrelationships.mapping.toModel
import uk.gov.justice.digital.hmpps.personalrelationships.model.internal.ContactCreationContext
import uk.gov.justice.digital.hmpps.personalrelationships.model.internal.TransactionalContactCreationResult
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.ContactAddressDetails
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.ContactAddressPhoneDetails
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.ContactCreationResult
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.ContactDetails
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.ContactEmailDetails
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.ContactIdentityDetails
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.ContactPhoneDetails
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.EmploymentDetails
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.PrisonerContactRelationshipDetails

@Component
class ContactCreationResultBuilder {
  fun build(
    snapshot: TransactionalContactCreationResult,
    context: ContactCreationContext,
  ): ContactCreationResult {
    val phoneNumbers = snapshot.phones.map { it.toModel() }
    val addresses = snapshot.addresses.map { address ->
      address.toModel(
        getAddressPhoneNumbers(
          address.contactAddressId,
          snapshot.addressPhones,
          phoneNumbers,
        ),
      )
    }
    val globalPhoneNumbers = phoneNumbers.filterNot { phone ->
      snapshot.addressPhones.any { addressPhone -> addressPhone.contactPhoneId == phone.contactPhoneId }
    }
    val employments = snapshot.employments.map { employment ->
      employment.toDetails(context.organisations.getValue(employment.organisationId))
    }

    return ContactCreationResult(
      createdContact = snapshot.contact.toDetails(
        titleDescription = context.title?.description,
        languageDescription = context.language?.description,
        domesticStatusDescription = context.domesticStatus?.description,
        genderDescription = context.gender?.description,
        addresses = addresses,
        phoneNumbers = globalPhoneNumbers,
        emailAddresses = snapshot.emails.map { it.toModel() },
        identities = snapshot.identities.map { it.toModel() },
        employments = employments,
      ),
      createdRelationship = snapshot.relationship?.let { relationship ->
        val relationshipReferenceData = checkNotNull(context.relationship) {
          "Relationship reference data is required for a created relationship"
        }
        relationship.toDetails(
          relationshipReferenceData.relationshipType.description,
          relationshipReferenceData.relationshipToPrisoner.description,
        )
      },
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

  private fun ContactEntity.toDetails(
    titleDescription: String?,
    languageDescription: String?,
    domesticStatusDescription: String?,
    genderDescription: String?,
    addresses: List<ContactAddressDetails>,
    phoneNumbers: List<ContactPhoneDetails>,
    emailAddresses: List<ContactEmailDetails>,
    identities: List<ContactIdentityDetails>,
    employments: List<EmploymentDetails>,
  ) = ContactDetails(
    id = id(),
    titleCode = title,
    titleDescription = titleDescription,
    lastName = lastName,
    firstName = firstName,
    middleNames = middleNames,
    dateOfBirth = dateOfBirth,
    isStaff = staffFlag,
    deceasedDate = deceasedDate,
    languageCode = languageCode,
    languageDescription = languageDescription,
    interpreterRequired = interpreterRequired,
    addresses = addresses,
    phoneNumbers = phoneNumbers,
    emailAddresses = emailAddresses,
    identities = identities,
    employments = employments,
    domesticStatusCode = domesticStatus,
    domesticStatusDescription = domesticStatusDescription,
    genderCode = gender,
    genderDescription = genderDescription,
    createdBy = createdBy,
    createdTime = createdTime,
  )

  private fun EmploymentEntity.toDetails(organisation: OrganisationSummary) = EmploymentDetails(
    employmentId = employmentId,
    contactId = contactId,
    employer = organisation,
    isActive = active,
    createdBy = createdBy,
    createdTime = createdTime,
    updatedBy = updatedBy,
    updatedTime = updatedTime,
  )

  private fun PrisonerContactEntity.toDetails(
    relationshipTypeDescription: String,
    relationshipToPrisonerDescription: String,
  ) = PrisonerContactRelationshipDetails(
    prisonerContactId = prisonerContactId,
    contactId = contactId,
    prisonerNumber = prisonerNumber,
    relationshipTypeCode = relationshipType,
    relationshipTypeDescription = relationshipTypeDescription,
    relationshipToPrisonerCode = relationshipToPrisoner,
    relationshipToPrisonerDescription = relationshipToPrisonerDescription,
    isEmergencyContact = emergencyContact,
    isNextOfKin = nextOfKin,
    isApprovedVisitor = approvedVisitor,
    isRelationshipActive = active,
    comments = comments,
    approvedBy = approvedBy,
  )
}
