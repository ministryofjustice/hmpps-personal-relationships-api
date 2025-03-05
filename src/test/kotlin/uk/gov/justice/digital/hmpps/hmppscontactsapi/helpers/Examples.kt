package uk.gov.justice.digital.hmpps.hmppscontactsapi.helpers

import org.openapitools.jackson.nullable.JsonNullable
import uk.gov.justice.digital.hmpps.hmppscontactsapi.client.organisationsapi.model.OrganisationSummary
import uk.gov.justice.digital.hmpps.hmppscontactsapi.client.prisonersearch.Prisoner
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.ContactAddressDetailsEntity
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.ContactEmailEntity
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.ContactIdentityDetailsEntity
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.ContactPhoneDetailsEntity
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.ContactRestrictionDetailsEntity
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.EmploymentEntity
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.PrisonerContactRestrictionDetailsEntity
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.CreateContactAddressRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.PatchContactAddressRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.UpdateContactAddressRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.phone.CreateContactAddressPhoneRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.phone.UpdateContactAddressPhoneRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.ContactAddressDetails
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.ContactAddressPhoneDetails
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.ContactAddressResponse
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.ContactEmailDetails
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.ContactIdentityDetails
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.ContactPhoneDetails
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.ContactRestrictionDetails
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.EmploymentDetails
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.PrisonerContactRelationshipDetails
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.PrisonerContactRestrictionDetails
import java.time.LocalDate
import java.time.LocalDateTime

fun createContactPhoneDetailsEntity(
  id: Long = 99,
  contactId: Long = 45,
  phoneType: String = "HOME",
  phoneTypeDescription: String = "Home phone",
  phoneNumber: String = "123456789",
  extNumber: String? = "987654321",
  createdBy: String = "CREATOR",
  createdTime: LocalDateTime = LocalDateTime.of(2024, 2, 3, 4, 5, 6),
  updatedBy: String? = "AM",
  updatedTime: LocalDateTime? = LocalDateTime.of(2026, 5, 4, 3, 2, 1),
): ContactPhoneDetailsEntity = ContactPhoneDetailsEntity(
  id,
  contactId,
  phoneType,
  phoneTypeDescription,
  phoneNumber,
  extNumber,
  createdBy,
  createdTime,
  updatedBy,
  updatedTime,
)

fun createContactPhoneNumberDetails(
  id: Long = 99,
  contactId: Long = 45,
  phoneType: String = "HOME",
  phoneTypeDescription: String = "Home phone",
  phoneNumber: String = "123456789",
  extNumber: String? = "987654321",
  createdBy: String = "CREATOR",
  createdTime: LocalDateTime = LocalDateTime.of(2024, 2, 3, 4, 5, 6),
  updatedBy: String? = "AM",
  updatedTime: LocalDateTime? = LocalDateTime.of(2026, 5, 4, 3, 2, 1),
): ContactPhoneDetails = ContactPhoneDetails(
  id,
  contactId,
  phoneType,
  phoneTypeDescription,
  phoneNumber,
  extNumber,
  createdBy,
  createdTime,
  updatedBy,
  updatedTime,
)

fun createContactAddressDetailsEntity(
  id: Long = 0,
  contactId: Long = 0,
  addressType: String? = "HOME",
  addressTypeDescription: String? = "Home address",
  primaryAddress: Boolean = true,
  flat: String? = "Flat",
  property: String? = "Property",
  street: String? = "Street",
  area: String? = "Area",
  cityCode: String? = "CIT",
  cityDescription: String? = "City",
  countyCode: String? = "COUNT",
  countyDescription: String? = "County",
  postCode: String? = "POST CODE",
  countryCode: String? = "ENG",
  countryDescription: String? = "England",
  verified: Boolean = true,
  verifiedBy: String? = "VERIFIED",
  verifiedTime: LocalDateTime? = LocalDateTime.of(2021, 1, 1, 11, 15, 0),
  mailFlag: Boolean = true,
  startDate: LocalDate? = LocalDate.of(2020, 2, 3),
  endDate: LocalDate? = LocalDate.of(2050, 4, 5),
  noFixedAddress: Boolean = true,
  comments: String? = "Some comments",
  createdBy: String = "USER1",
  createdTime: LocalDateTime = LocalDateTime.of(2023, 2, 3, 11, 15, 15),
  updatedBy: String? = "AMEND_USER",
  updatedTime: LocalDateTime = LocalDateTime.of(2024, 5, 6, 12, 30, 30),
): ContactAddressDetailsEntity = ContactAddressDetailsEntity(
  id,
  contactId,
  addressType,
  addressTypeDescription,
  primaryAddress,
  flat,
  property,
  street,
  area,
  cityCode,
  cityDescription,
  countyCode,
  countyDescription,
  postCode,
  countryCode,
  countryDescription,
  verified,
  verifiedBy,
  verifiedTime,
  mailFlag,
  startDate,
  endDate,
  noFixedAddress,
  comments,
  createdBy,
  createdTime,
  updatedBy,
  updatedTime,
)

fun createContactAddressDetails(
  id: Long = 0,
  contactId: Long = 0,
  addressType: String? = "HOME",
  addressTypeDescription: String? = "Home address",
  primaryAddress: Boolean = true,
  flat: String? = "Flat",
  property: String? = "Property",
  street: String? = "Street",
  area: String? = "Area",
  cityCode: String? = "CIT",
  cityDescription: String? = "City",
  countyCode: String? = "COUNT",
  countyDescription: String? = "County",
  postCode: String? = "POST CODE",
  countryCode: String? = "ENG",
  countryDescription: String? = "England",
  verified: Boolean = true,
  verifiedBy: String? = "VERIFIED",
  verifiedTime: LocalDateTime? = LocalDateTime.of(2021, 1, 1, 11, 15, 0),
  mailFlag: Boolean = true,
  startDate: LocalDate? = LocalDate.of(2020, 2, 3),
  endDate: LocalDate? = LocalDate.of(2050, 4, 5),
  noFixedAddress: Boolean = true,
  comments: String? = "Some comments",
  phoneNumbers: List<ContactAddressPhoneDetails> = emptyList(),
  createdBy: String = "USER1",
  createdTime: LocalDateTime = LocalDateTime.of(2023, 2, 3, 11, 15, 15),
  updatedBy: String? = "AMEND_USER",
  updatedTime: LocalDateTime = LocalDateTime.of(2024, 5, 6, 12, 30, 30),
): ContactAddressDetails = ContactAddressDetails(
  id,
  contactId,
  addressType,
  addressTypeDescription,
  primaryAddress,
  flat,
  property,
  street,
  area,
  cityCode,
  cityDescription,
  countyCode,
  countyDescription,
  postCode,
  countryCode,
  countryDescription,
  verified,
  verifiedBy,
  verifiedTime,
  mailFlag,
  startDate,
  endDate,
  noFixedAddress,
  comments,
  phoneNumbers,
  createdBy,
  createdTime,
  updatedBy,
  updatedTime,
)

fun createContactEmailEntity(
  id: Long = 1,
  contactId: Long = 1,
  emailAddress: String = "test@example.com",
  createdBy: String = "USER",
  createdTime: LocalDateTime = LocalDateTime.now(),
  updatedBy: String? = "AMEND_USER",
  updatedTime: LocalDateTime? = LocalDateTime.now(),
) = ContactEmailEntity(
  id,
  contactId,
  emailAddress,
  createdBy,
  createdTime,
  updatedBy,
  updatedTime,
)

fun createContactEmailDetails(
  id: Long = 1,
  contactId: Long = 1,
  emailAddress: String = "test@example.com",
  createdBy: String = "USER",
  createdTime: LocalDateTime = LocalDateTime.now(),
  updatedBy: String? = "AMEND_USER",
  updatedTime: LocalDateTime? = LocalDateTime.now(),
) = ContactEmailDetails(
  id,
  contactId,
  emailAddress,
  createdBy,
  createdTime,
  updatedBy,
  updatedTime,
)

fun createContactIdentityDetailsEntity(
  id: Long = 1,
  contactId: Long = 1,
  identityType: String? = "PASS",
  identityTypeDescription: String? = "Passport",
  identityTypeIsActive: Boolean = false,
  identityValue: String? = "132456789",
  issuingAuthority: String? = "UK",
  createdBy: String = "CRE",
  createdTime: LocalDateTime = LocalDateTime.of(2024, 2, 2, 2, 2, 2),
  updatedBy: String? = "AMD",
  updatedTime: LocalDateTime? = LocalDateTime.of(2024, 3, 3, 3, 3, 3),
) = ContactIdentityDetailsEntity(
  id,
  contactId,
  identityType,
  identityTypeDescription,
  identityTypeIsActive,
  identityValue,
  issuingAuthority,
  createdBy,
  createdTime,
  updatedBy,
  updatedTime,
)

fun createContactIdentityDetails(
  id: Long = 1,
  contactId: Long = 1,
  identityType: String? = "PASS",
  identityTypeDescription: String? = "Passport",
  identityTypeIsActive: Boolean = false,
  identityValue: String? = "132456789",
  issuingAuthority: String? = "UK",
  createdBy: String = "CRE",
  createdTime: LocalDateTime = LocalDateTime.of(2024, 2, 2, 2, 2, 2),
  updatedBy: String? = "AMD",
  updatedTime: LocalDateTime? = LocalDateTime.of(2024, 3, 3, 3, 3, 3),
) = ContactIdentityDetails(
  id,
  contactId,
  identityType,
  identityTypeDescription,
  identityTypeIsActive,
  identityValue,
  issuingAuthority,
  createdBy,
  createdTime,
  updatedBy,
  updatedTime,
)

fun createPrisonerContactRelationshipDetails(
  id: Long = 1,
  contactId: Long = 99,
  prisonerNumber: String = "A1234BC",
  relationshipType: String = "S",
  relationshipTypeDescription: String = "Social",
  relationshipCode: String = "FRI",
  relationshipDescription: String = "Friend",
  emergencyContact: Boolean = false,
  nextOfKin: Boolean = false,
  isRelationshipActive: Boolean = true,
  isApprovedVisitor: Boolean = true,
  comments: String? = null,
) = PrisonerContactRelationshipDetails(
  id,
  contactId,
  prisonerNumber,
  relationshipType,
  relationshipTypeDescription,
  relationshipCode,
  relationshipDescription,
  emergencyContact,
  nextOfKin,
  isRelationshipActive,
  isApprovedVisitor,
  comments,
)

fun createContactRestrictionDetailsEntity(
  id: Long = 1,
  contactId: Long = 123,
  restrictionType: String = "BAN",
  restrictionTypeDescription: String = "Banned",
  startDate: LocalDate? = LocalDate.of(2020, 1, 1),
  expiryDate: LocalDate? = null,
  comments: String? = null,
  createdBy: String = "USER1",
  createdTime: LocalDateTime = LocalDateTime.now(),
  updatedBy: String? = null,
  updatedTime: LocalDateTime? = null,
): ContactRestrictionDetailsEntity = ContactRestrictionDetailsEntity(
  id,
  contactId,
  restrictionType,
  restrictionTypeDescription,
  startDate,
  expiryDate,
  comments,
  createdBy,
  createdTime,
  updatedBy,
  updatedTime,
)

fun createContactRestrictionDetails(
  id: Long = 1,
  contactId: Long = 123,
  restrictionType: String = "BAN",
  restrictionTypeDescription: String = "Banned",
  startDate: LocalDate? = LocalDate.of(2020, 1, 1),
  expiryDate: LocalDate? = null,
  comments: String? = null,
  enteredByUsername: String = "USER1",
  enteredByDisplayName: String = "User One",
  createdBy: String = "USER1",
  createdTime: LocalDateTime = LocalDateTime.now(),
  updatedBy: String? = null,
  updatedTime: LocalDateTime? = null,
): ContactRestrictionDetails = ContactRestrictionDetails(
  id,
  contactId,
  restrictionType,
  restrictionTypeDescription,
  startDate,
  expiryDate,
  comments,
  enteredByUsername,
  enteredByDisplayName,
  createdBy,
  createdTime,
  updatedBy,
  updatedTime,
)

fun createPrisonerContactRestrictionDetailsEntity(
  id: Long = 1,
  prisonerContactId: Long = 123,
  restrictionType: String = "BAN",
  restrictionTypeDescription: String = "Banned",
  startDate: LocalDate? = LocalDate.of(2020, 1, 1),
  expiryDate: LocalDate? = null,
  comments: String? = null,
  createdBy: String = "USER1",
  createdTime: LocalDateTime = LocalDateTime.now(),
  updatedBy: String? = null,
  updatedTime: LocalDateTime? = null,
): PrisonerContactRestrictionDetailsEntity = PrisonerContactRestrictionDetailsEntity(
  id,
  prisonerContactId,
  restrictionType,
  restrictionTypeDescription,
  startDate,
  expiryDate,
  comments,
  createdBy,
  createdTime,
  updatedBy,
  updatedTime,
)

fun createPrisonerContactRestrictionDetails(
  id: Long = 1,
  prisonerContactId: Long = 123,
  contactId: Long = 999,
  prisonerNumber: String = "A1234BC",
  restrictionType: String = "BAN",
  restrictionTypeDescription: String = "Banned",
  startDate: LocalDate? = LocalDate.of(2020, 1, 1),
  expiryDate: LocalDate? = null,
  comments: String? = null,
  enteredByUsername: String = "USER1",
  enteredByDisplayName: String = "User One",
  createdBy: String = "USER1",
  createdTime: LocalDateTime = LocalDateTime.now(),
  updatedBy: String? = null,
  updatedTime: LocalDateTime? = null,
): PrisonerContactRestrictionDetails = PrisonerContactRestrictionDetails(
  id,
  prisonerContactId,
  contactId,
  prisonerNumber,
  restrictionType,
  restrictionTypeDescription,
  startDate,
  expiryDate,
  comments,
  enteredByUsername,
  enteredByDisplayName,
  createdBy,
  createdTime,
  updatedBy,
  updatedTime,
)

fun createContactAddressRequest(
  addressType: String = "HOME",
  flat: String = "1B",
  property: String = "35",
  street: String = "Acacia Avenue",
  area: String = "Bulls Nose",
  postcode: String = "EC1 2NJ",
  createdBy: String = "CREATE_USER",
  createdTime: LocalDateTime = LocalDateTime.now(),
) = CreateContactAddressRequest(
  addressType = addressType,
  flat = flat,
  property = property,
  street = street,
  area = area,
  postcode = postcode,
  createdBy = createdBy,
)

fun updateContactAddressRequest(
  primaryAddress: Boolean = true,
  addressType: String = "HOME",
  flat: String = "1B",
  property: String = "35",
  street: String = "Acacia Avenue",
  area: String = "Bulls Nose",
  postcode: String = "EC1 2NJ",
  updatedBy: String = "AMEND_USER",
  updatedTime: LocalDateTime = LocalDateTime.now(),
) = UpdateContactAddressRequest(
  primaryAddress = primaryAddress,
  addressType = addressType,
  flat = flat,
  property = property,
  street = street,
  area = area,
  postcode = postcode,
  updatedBy = updatedBy,
)

fun patchContactAddressRequest() = PatchContactAddressRequest(
  primaryAddress = JsonNullable.of(true),
  addressType = JsonNullable.of("HOME"),
  flat = JsonNullable.of("1B"),
  property = JsonNullable.of("35"),
  street = JsonNullable.of("Acacia Avenue"),
  area = JsonNullable.of("Bulls Nose"),
  postcode = JsonNullable.of("EC1 2NJ"),
  updatedBy = "AMEND_USER",
)

fun contactAddressResponse(
  contactAddressId: Long,
  contactId: Long,
  addressType: String? = "HOME",
  primaryAddress: Boolean = true,
  flat: String? = null,
  property: String? = null,
  street: String? = null,
  area: String? = null,
  postcode: String? = null,
  createdBy: String = "CREATE_USER",
  createdTime: LocalDateTime = LocalDateTime.now(),
) = ContactAddressResponse(
  contactAddressId = contactAddressId,
  contactId = contactId,
  addressType = addressType,
  primaryAddress = primaryAddress,
  flat = flat,
  property = property,
  street = street,
  area = area,
  cityCode = null,
  countyCode = null,
  postcode = postcode,
  countryCode = null,
  verified = false,
  verifiedBy = null,
  verifiedTime = null,
  mailFlag = true,
  startDate = null,
  endDate = null,
  noFixedAddress = false,
  comments = null,
  createdBy = createdBy,
  createdTime = createdTime,
  updatedBy = null,
  updatedTime = null,
)

fun createContactAddressPhoneRequest(
  contactAddressId: Long,
  phoneType: String = "HOME",
  phoneNumber: String = "0876 878787",
  extNumber: String? = null,
  createdBy: String = "CREATE_USER",
) = CreateContactAddressPhoneRequest(
  contactAddressId = contactAddressId,
  phoneType = phoneType,
  phoneNumber = phoneNumber,
  extNumber = extNumber,
  createdBy = createdBy,
)

fun updateContactAddressPhoneRequest(
  phoneType: String = "HOME",
  phoneNumber: String = "0878 7666565",
  extNumber: String? = null,
  updatedBy: String = "AMEND_USER",
) = UpdateContactAddressPhoneRequest(
  phoneType = phoneType,
  phoneNumber = phoneNumber,
  extNumber = extNumber,
  updatedBy = updatedBy,
)

fun contactAddressPhoneResponse(
  contactAddressPhoneId: Long,
  contactAddressId: Long,
  contactPhoneId: Long,
  contactId: Long,
  phoneType: String = "HOME",
  phoneTypeDescription: String = "Home",
  phoneNumber: String = "0878 7666565",
  extNumber: String? = null,
  createdBy: String = "CREATE_USER",
  createdTime: LocalDateTime = LocalDateTime.now(),
  updatedBy: String = "AMEND_USER",
  updatedTime: LocalDateTime = LocalDateTime.now(),
) = ContactAddressPhoneDetails(
  contactAddressPhoneId = contactAddressPhoneId,
  contactAddressId = contactAddressId,
  contactPhoneId = contactPhoneId,
  contactId = contactId,
  phoneType = phoneType,
  phoneTypeDescription = phoneTypeDescription,
  phoneNumber = phoneNumber,
  extNumber = null,
  createdBy = createdBy,
  createdTime = createdTime,
  updatedBy = updatedBy,
  updatedTime = updatedTime,
)

fun prisoner(
  prisonerNumber: String = "A1324BC",
  prisonId: String? = null,
  prisonName: String? = null,
  lastName: String = "Last",
  firstName: String = "First",
  middleNames: String? = null,
) = Prisoner(
  prisonerNumber,
  prisonId,
  prisonName,
  lastName,
  firstName,
  middleNames,
)

fun createEmploymentEntity(
  id: Long = 1,
  contactId: Long = 1,
  organisationId: Long = 1,
  active: Boolean = true,
  createdBy: String = "USER",
  createdTime: LocalDateTime = LocalDateTime.now(),
  updatedBy: String? = "AMEND_USER",
  updatedTime: LocalDateTime? = LocalDateTime.now(),
) = EmploymentEntity(
  id,
  organisationId,
  contactId,
  active,
  createdBy,
  createdTime,
  updatedBy,
  updatedTime,
)

fun createEmploymentDetails(
  id: Long = 1,
  contactId: Long = 1,
  organisation: OrganisationSummary = createOrganisationSummary(),
  active: Boolean = true,
  createdBy: String = "USER",
  createdTime: LocalDateTime = LocalDateTime.now(),
  updatedBy: String? = "AMEND_USER",
  updatedTime: LocalDateTime? = LocalDateTime.now(),
) = EmploymentDetails(
  employmentId = id,
  contactId = contactId,
  employer = organisation,
  isActive = active,
  createdBy = createdBy,
  createdTime = createdTime,
  updatedBy = updatedBy,
  updatedTime = updatedTime,
)

fun createOrganisationSummary(
  id: Long = 1,
  organisationName: String = "Some name limited",
  organisationActive: Boolean = true,
  flat: String? = "Flat",
  property: String? = "Property",
  street: String? = "Street",
  area: String? = "Area",
  cityCode: String? = "123",
  cityDescription: String? = "City",
  countyCode: String? = "C.OUNTY",
  countyDescription: String? = "County",
  postCode: String? = "AB12 3CD",
  countryCode: String? = "COU",
  countryDescription: String? = "Country",
  businessPhoneNumber: String? = "0123456",
  businessPhoneNumberExtension: String? = "789",
) = OrganisationSummary(
  id,
  organisationName,
  organisationActive,
  flat,
  property,
  street,
  area,
  cityCode,
  cityDescription,
  countyCode,
  countyDescription,
  postCode,
  countryCode,
  countryDescription,
  businessPhoneNumber,
  businessPhoneNumberExtension,
)
