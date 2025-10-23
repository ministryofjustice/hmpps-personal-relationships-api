package uk.gov.justice.digital.hmpps.hmppscontactsapi.service

import jakarta.persistence.EntityNotFoundException
import jakarta.validation.ValidationException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.never
import org.mockito.kotlin.same
import org.mockito.kotlin.whenever
import org.openapitools.jackson.nullable.JsonNullable
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.ContactAddressPhoneEntity
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.ContactEntity
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.DeletedPrisonerContactEntity
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.PrisonerContactEntity
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.PrisonerContactRestrictionEntity
import uk.gov.justice.digital.hmpps.hmppscontactsapi.exception.DuplicateRelationshipException
import uk.gov.justice.digital.hmpps.hmppscontactsapi.exception.RelationshipCannotBeRemovedDueToDependencyException
import uk.gov.justice.digital.hmpps.hmppscontactsapi.helpers.aUser
import uk.gov.justice.digital.hmpps.hmppscontactsapi.helpers.contactAddressResponse
import uk.gov.justice.digital.hmpps.hmppscontactsapi.helpers.createAddress
import uk.gov.justice.digital.hmpps.hmppscontactsapi.helpers.createContactAddressDetailsEntity
import uk.gov.justice.digital.hmpps.hmppscontactsapi.helpers.createContactEmailDetails
import uk.gov.justice.digital.hmpps.hmppscontactsapi.helpers.createContactEmailEntity
import uk.gov.justice.digital.hmpps.hmppscontactsapi.helpers.createContactIdentityDetails
import uk.gov.justice.digital.hmpps.hmppscontactsapi.helpers.createContactIdentityDetailsEntity
import uk.gov.justice.digital.hmpps.hmppscontactsapi.helpers.createContactPhoneDetailsEntity
import uk.gov.justice.digital.hmpps.hmppscontactsapi.helpers.createContactPhoneNumberDetails
import uk.gov.justice.digital.hmpps.hmppscontactsapi.helpers.createEmploymentDetails
import uk.gov.justice.digital.hmpps.hmppscontactsapi.helpers.createPhoneNumber
import uk.gov.justice.digital.hmpps.hmppscontactsapi.helpers.prisoner
import uk.gov.justice.digital.hmpps.hmppscontactsapi.mapping.toModel
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.ReferenceCodeGroup
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.internal.DeletedRelationshipIds
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.internal.DeletedResponse
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.AddContactRelationshipRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.ContactRelationship
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.CreateContactRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.PatchRelationshipRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.address.CreateContactAddressRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.email.EmailAddress
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.employment.Employment
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.identity.CreateMultipleIdentitiesRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.identity.IdentityDocument
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.ContactNameDetails
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.CreateAddressResponse
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.ReferenceCode
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.ContactAddressDetailsRepository
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.ContactAddressPhoneRepository
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.ContactEmailRepository
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.ContactIdentityDetailsRepository
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.ContactPhoneDetailsRepository
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.ContactRepository
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.DeletedPrisonerContactRepository
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.PrisonerContactRepository
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.PrisonerContactRestrictionRepository
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class ContactServiceTest {

  private val contactRepository: ContactRepository = mock()
  private val prisonerContactRepository: PrisonerContactRepository = mock()
  private val prisonerService: PrisonerService = mock()
  private val contactAddressDetailsRepository: ContactAddressDetailsRepository = mock()
  private val contactPhoneDetailsRepository: ContactPhoneDetailsRepository = mock()
  private val contactAddressPhoneRepository: ContactAddressPhoneRepository = mock()
  private val contactEmailRepository: ContactEmailRepository = mock()
  private val contactIdentityDetailsRepository: ContactIdentityDetailsRepository = mock()
  private val employmentService: EmploymentService = mock()
  private val referenceCodeService: ReferenceCodeService = mock()
  private val contactIdentityService: ContactIdentityService = mock()
  private val contactAddressService: ContactAddressService = mock()
  private val contactPhoneService: ContactPhoneService = mock()
  private val contactEmailService: ContactEmailService = mock()
  private val prisonerContactRestrictionRepository: PrisonerContactRestrictionRepository = mock()
  private val deletedPrisonerContactRepository: DeletedPrisonerContactRepository = mock()
  private val service = ContactService(
    contactRepository,
    prisonerContactRepository,
    prisonerService,
    contactAddressDetailsRepository,
    contactPhoneDetailsRepository,
    contactAddressPhoneRepository,
    contactEmailRepository,
    contactIdentityDetailsRepository,
    referenceCodeService,
    employmentService,
    contactIdentityService,
    contactAddressService,
    contactPhoneService,
    contactEmailService,
    prisonerContactRestrictionRepository,
    deletedPrisonerContactRepository,
  )

  private val aContactAddressDetailsEntity = createContactAddressDetailsEntity()

  @Nested
  inner class CreateContact {
    private val user = aUser("created")

    @Test
    fun `should create a contact with all fields successfully`() {
      val identities = listOf(
        IdentityDocument(
          identityType = "PNC",
          identityValue = "1923/1Z34567A",
        ),
      )
      val addressWithPhoneNumber = createAddress(addressType = "HOME", phoneNumbers = listOf(createPhoneNumber()))
      val phoneNumber = createPhoneNumber("BUS", "999", "1")
      val request = CreateContactRequest(
        titleCode = "mr",
        lastName = "last",
        firstName = "first",
        middleNames = "middle",
        dateOfBirth = LocalDate.of(1982, 6, 15),
        identities = identities,
        addresses = listOf(addressWithPhoneNumber),
        phoneNumbers = listOf(phoneNumber),
        emailAddresses = listOf(EmailAddress("test@example.com")),
        employments = listOf(
          Employment(organisationId = 1, isActive = true),
          Employment(organisationId = 2, isActive = false),
        ),
      )
      whenever(contactRepository.saveAndFlush(any())).thenAnswer { i -> (i.arguments[0] as ContactEntity).copy(contactId = 123) }
      whenever(contactAddressDetailsRepository.findByContactId(any())).thenReturn(listOf(aContactAddressDetailsEntity))
      whenever(contactIdentityService.createMultiple(any(), any(), any())).thenReturn(
        listOf(
          createContactIdentityDetails(),
        ),
      )
      val identityEntity1 =
        createContactIdentityDetailsEntity(id = 1, identityType = "PNC", identityValue = "1923/1Z34567A")
      whenever(contactIdentityDetailsRepository.findByContactId(123L)).thenReturn(listOf(identityEntity1))
      whenever(contactAddressService.create(any(), any(), any())).thenReturn(
        CreateAddressResponse(
          contactAddressResponse(
            999,
            123,
            phoneNumberIds = listOf(987),
          ),
          emptySet(),
        ),
      )
      whenever(contactPhoneService.createMultiple(any(), any(), any())).thenReturn(
        listOf(
          createContactPhoneNumberDetails(),
        ),
      )
      whenever(contactEmailService.createMultiple(any(), any(), any())).thenReturn(listOf(createContactEmailDetails()))

      val result = service.createContact(request, user)

      verify(contactIdentityService).createMultiple(123, CreateMultipleIdentitiesRequest(identities), user)
      val contactCaptor = argumentCaptor<ContactEntity>()
      verify(contactRepository).saveAndFlush(contactCaptor.capture())
      with(contactCaptor.firstValue) {
        assertThat(title).isEqualTo(request.titleCode)
        assertThat(lastName).isEqualTo(request.lastName)
        assertThat(firstName).isEqualTo(request.firstName)
        assertThat(middleNames).isEqualTo(request.middleNames)
        assertThat(dateOfBirth).isEqualTo(request.dateOfBirth)
        assertThat(languageCode).isEqualTo(request.languageCode)
        assertThat(interpreterRequired).isEqualTo(request.interpreterRequired)
        assertThat(gender).isEqualTo(request.genderCode)
        assertThat(domesticStatus).isEqualTo(request.domesticStatusCode)
        assertThat(staffFlag).isEqualTo(request.isStaff)
        assertThat(createdBy).isEqualTo(user.username)
        assertThat(createdTime).isNotNull()
      }
      with(result) {
        with(createdContact) {
          assertThat(titleCode).isEqualTo(request.titleCode)
          assertThat(lastName).isEqualTo(request.lastName)
          assertThat(firstName).isEqualTo(request.firstName)
          assertThat(middleNames).isEqualTo(request.middleNames)
          assertThat(dateOfBirth).isEqualTo(request.dateOfBirth)
          assertThat(languageCode).isEqualTo(request.languageCode)
          assertThat(interpreterRequired).isEqualTo(request.interpreterRequired)
          assertThat(genderCode).isEqualTo(request.genderCode)
          assertThat(domesticStatusCode).isEqualTo(request.domesticStatusCode)
          assertThat(isStaff).isEqualTo(request.isStaff)
          assertThat(createdBy).isEqualTo(user.username)
          assertThat(createdTime).isNotNull()
          assertThat(addresses).isEqualTo(listOf(aContactAddressDetailsEntity.toModel(emptyList())))
        }
      }
      assertThat(result.createdContact.identities).hasSize(1)
      assertThat(result.createdContact.identities[0].identityType).isEqualTo("PNC")
      assertThat(result.createdContact.identities[0].identityValue).isEqualTo("1923/1Z34567A")

      val contactAddressCaptor = argumentCaptor<CreateContactAddressRequest>()
      verify(contactAddressService).create(eq(123L), contactAddressCaptor.capture(), same(user))
      val createAddressRequest = contactAddressCaptor.firstValue
      with(createAddressRequest) {
        assertThat(addressType).isEqualTo(addressWithPhoneNumber.addressType)
        assertThat(primaryAddress).isEqualTo(addressWithPhoneNumber.primaryAddress)
        assertThat(flat).isEqualTo(addressWithPhoneNumber.flat)
        assertThat(property).isEqualTo(addressWithPhoneNumber.property)
        assertThat(street).isEqualTo(addressWithPhoneNumber.street)
        assertThat(area).isEqualTo(addressWithPhoneNumber.area)
        assertThat(cityCode).isEqualTo(addressWithPhoneNumber.cityCode)
        assertThat(countyCode).isEqualTo(addressWithPhoneNumber.countyCode)
        assertThat(postcode).isEqualTo(addressWithPhoneNumber.postcode)
        assertThat(countryCode).isEqualTo(addressWithPhoneNumber.countryCode)
        assertThat(verified).isEqualTo(addressWithPhoneNumber.verified)
        assertThat(mailFlag).isEqualTo(addressWithPhoneNumber.mailFlag)
        assertThat(startDate).isEqualTo(addressWithPhoneNumber.startDate)
        assertThat(endDate).isEqualTo(addressWithPhoneNumber.endDate)
        assertThat(noFixedAddress).isEqualTo(addressWithPhoneNumber.noFixedAddress)
        assertThat(phoneNumbers).isEqualTo(addressWithPhoneNumber.phoneNumbers)
        assertThat(comments).isEqualTo(addressWithPhoneNumber.comments)
      }

      verify(contactPhoneService).createMultiple(123L, user.username, listOf(phoneNumber))
      verify(contactEmailService).createMultiple(123L, user.username, listOf(EmailAddress("test@example.com")))
      verify(employmentService).createEmployment(123L, 1, true, user.username)
      verify(employmentService).createEmployment(123L, 2, false, user.username)
    }

    @Test
    fun `should create a contact with multiple identities successfully`() {
      val identities = listOf(
        IdentityDocument(
          identityType = "PNC",
          identityValue = "1923/1Z34567A",
        ),
        IdentityDocument(
          identityType = "DL",
          identityValue = "DL123456789",
        ),
      )
      val request = CreateContactRequest(
        titleCode = "mr",
        lastName = "last",
        firstName = "first",
        middleNames = "middle",
        dateOfBirth = LocalDate.of(1982, 6, 15),
        identities = identities,
      )
      whenever(contactRepository.saveAndFlush(any())).thenAnswer { i -> (i.arguments[0] as ContactEntity).copy(contactId = 123L) }
      whenever(contactAddressDetailsRepository.findByContactId(123L)).thenReturn(listOf(aContactAddressDetailsEntity))
      whenever(contactIdentityService.createMultiple(any(), any(), any())).thenReturn(
        listOf(
          createContactIdentityDetails(),
          createContactIdentityDetails(2L),
        ),
      )
      val identityEntity1 =
        createContactIdentityDetailsEntity(id = 1, identityType = "PNC", identityValue = "1923/1Z34567A")
      val identityEntity2 =
        createContactIdentityDetailsEntity(id = 2, identityType = "DL", identityValue = "DL123456789")
      whenever(contactIdentityDetailsRepository.findByContactId(123L)).thenReturn(
        listOf(
          identityEntity1,
          identityEntity2,
        ),
      )

      val result = service.createContact(request, user)

      verify(contactIdentityService).createMultiple(123L, CreateMultipleIdentitiesRequest(identities), user)
      val contactCaptor = argumentCaptor<ContactEntity>()
      verify(contactRepository).saveAndFlush(contactCaptor.capture())
      assertThat(contactCaptor.firstValue).isNotNull()
      assertThat(result.createdContact).isNotNull()
      assertThat(result.createdContact.identities).hasSize(2)
      assertThat(result.createdContact.identities[0].identityType).isEqualTo("PNC")
      assertThat(result.createdContact.identities[0].identityValue).isEqualTo("1923/1Z34567A")
      assertThat(result.createdContact.identities[1].identityType).isEqualTo("DL")
      assertThat(result.createdContact.identities[1].identityValue).isEqualTo("DL123456789")
    }

    @Test
    fun `should create a contact without optional fields successfully`() {
      val request = CreateContactRequest(
        titleCode = "mr",
        lastName = "last",
        firstName = "first",
        middleNames = "middle",
        dateOfBirth = null,
        isStaff = false,
        languageCode = null,
        interpreterRequired = false,
        domesticStatusCode = null,
        genderCode = null,
      )
      whenever(contactRepository.saveAndFlush(any())).thenAnswer { i -> (i.arguments[0] as ContactEntity).copy(contactId = 123) }

      val result = service.createContact(request, user)

      verify(contactIdentityService, never()).createMultiple(any(), any(), any())

      val contactCaptor = argumentCaptor<ContactEntity>()
      verify(contactRepository).saveAndFlush(contactCaptor.capture())
      with(contactCaptor.firstValue) {
        assertThat(title).isEqualTo(request.titleCode)
        assertThat(lastName).isEqualTo(request.lastName)
        assertThat(firstName).isEqualTo(request.firstName)
        assertThat(middleNames).isEqualTo(request.middleNames)
        assertNull(dateOfBirth)
        assertNull(languageCode)
        assertThat(interpreterRequired).isEqualTo(request.interpreterRequired)
        assertNull(gender)
        assertNull(domesticStatus)
        assertThat(staffFlag).isEqualTo(request.isStaff)
        assertThat(createdBy).isEqualTo(user.username)
        assertThat(createdTime).isNotNull()
      }
      with(result) {
        with(createdContact) {
          assertThat(titleCode).isEqualTo(request.titleCode)
          assertThat(lastName).isEqualTo(request.lastName)
          assertThat(firstName).isEqualTo(request.firstName)
          assertThat(middleNames).isEqualTo(request.middleNames)
          assertNull(dateOfBirth)
          assertNull(languageCode)
          assertThat(interpreterRequired).isEqualTo(request.interpreterRequired)
          assertNull(genderCode)
          assertNull(domesticStatusCode)
          assertThat(isStaff).isEqualTo(request.isStaff)
          assertThat(createdBy).isEqualTo(user.username)
          assertThat(createdTime).isNotNull()
        }
      }
    }

    @ParameterizedTest
    @CsvSource(
      value = [
        "S,SOCIAL_RELATIONSHIP",
        "O,OFFICIAL_RELATIONSHIP",
      ],
    )
    fun `should create a contact with a relationship successfully while validating the relationship type correctly`(
      relationshipType: String,
      expectedReferenceCodeGroup: ReferenceCodeGroup,
    ) {
      val relationshipRequest = ContactRelationship(
        prisonerNumber = "A1234BC",
        relationshipTypeCode = relationshipType,
        relationshipToPrisonerCode = "FRI",
        isNextOfKin = true,
        isEmergencyContact = true,
        isApprovedVisitor = true,
        comments = "some comments",
      )
      val request = CreateContactRequest(
        lastName = "last",
        firstName = "first",
        relationship = relationshipRequest,
      )
      whenever(prisonerService.getPrisoner(any())).thenReturn(
        prisoner(
          relationshipRequest.prisonerNumber,
          prisonId = "MDI",
        ),
      )
      whenever(contactRepository.saveAndFlush(any())).thenAnswer { i -> (i.arguments[0] as ContactEntity).copy(contactId = 123) }
      whenever(prisonerContactRepository.saveAndFlush(any())).thenAnswer { i -> i.arguments[0] }
      val referenceCode = ReferenceCode(1, expectedReferenceCodeGroup, "FRI", "Friend", 1, true)
      whenever(referenceCodeService.getReferenceDataByGroupAndCode(expectedReferenceCodeGroup, "FRI")).thenReturn(
        referenceCode,
      )
      whenever(
        referenceCodeService.validateReferenceCode(
          expectedReferenceCodeGroup,
          "FRI",
          allowInactive = false,
        ),
      ).thenReturn(referenceCode)

      service.createContact(request, user)

      verify(contactRepository).saveAndFlush(any())

      val prisonerContactCaptor = argumentCaptor<PrisonerContactEntity>()
      verify(prisonerContactRepository).saveAndFlush(prisonerContactCaptor.capture())
      verify(referenceCodeService).validateReferenceCode(expectedReferenceCodeGroup, "FRI", allowInactive = false)

      with(prisonerContactCaptor.firstValue) {
        assertThat(prisonerNumber).isEqualTo("A1234BC")
        assertThat(relationshipToPrisoner).isEqualTo("FRI")
        assertThat(nextOfKin).isEqualTo(true)
        assertThat(emergencyContact).isEqualTo(true)
        assertThat(approvedVisitor).isEqualTo(true)
        assertThat(comments).isEqualTo("some comments")
      }
    }

    @ParameterizedTest
    @CsvSource(value = ["true", "false"])
    fun `should create a contact with a relationship successfully with approved visitor details`(
      updatingApprovedVisitor: Boolean,
    ) {
      val relationshipRequest = ContactRelationship(
        prisonerNumber = "A1234BC",
        relationshipTypeCode = "S",
        relationshipToPrisonerCode = "FRI",
        isNextOfKin = true,
        isEmergencyContact = true,
        isApprovedVisitor = updatingApprovedVisitor,
        comments = "some comments",
      )
      val request = CreateContactRequest(
        lastName = "last",
        firstName = "first",
        relationship = relationshipRequest,
      )
      whenever(prisonerService.getPrisoner(any())).thenReturn(
        prisoner(
          relationshipRequest.prisonerNumber,
          prisonId = "MDI",
        ),
      )
      whenever(contactRepository.saveAndFlush(any())).thenAnswer { i -> (i.arguments[0] as ContactEntity).copy(contactId = 123) }
      whenever(prisonerContactRepository.saveAndFlush(any())).thenAnswer { i -> i.arguments[0] }
      val referenceCode = ReferenceCode(1, ReferenceCodeGroup.SOCIAL_RELATIONSHIP, "FRI", "Friend", 1, true)
      whenever(referenceCodeService.getReferenceDataByGroupAndCode(ReferenceCodeGroup.SOCIAL_RELATIONSHIP, "FRI")).thenReturn(
        referenceCode,
      )
      whenever(
        referenceCodeService.validateReferenceCode(
          ReferenceCodeGroup.SOCIAL_RELATIONSHIP,
          "FRI",
          allowInactive = false,
        ),
      ).thenReturn(referenceCode)

      service.createContact(request, user)

      verify(contactRepository).saveAndFlush(any())

      val prisonerContactCaptor = argumentCaptor<PrisonerContactEntity>()
      verify(prisonerContactRepository).saveAndFlush(prisonerContactCaptor.capture())

      with(prisonerContactCaptor.firstValue) {
        assertThat(prisonerNumber).isEqualTo("A1234BC")
        assertThat(relationshipToPrisoner).isEqualTo("FRI")
        assertThat(nextOfKin).isEqualTo(true)
        assertThat(emergencyContact).isEqualTo(true)
        assertThat(approvedVisitor).isEqualTo(updatingApprovedVisitor)
        assertThat(comments).isEqualTo("some comments")
        assertThat(approvedBy).isEqualTo(user.username)
        assertThat(approvedTime).isInThePast()
      }
    }

    @ParameterizedTest
    @CsvSource(
      value = [
        "S,SOCIAL_RELATIONSHIP",
        "O,OFFICIAL_RELATIONSHIP",
      ],
    )
    fun `should throw exception if relationship is invalid`(
      relationshipType: String,
      expectedReferenceCodeGroup: ReferenceCodeGroup,
    ) {
      val relationshipRequest = ContactRelationship(
        prisonerNumber = "A1234BC",
        relationshipToPrisonerCode = "FRI",
        isNextOfKin = true,
        isEmergencyContact = true,
        relationshipTypeCode = relationshipType,
        isApprovedVisitor = false,
        comments = "some comments",
      )
      val request = CreateContactRequest(
        lastName = "last",
        firstName = "first",
        relationship = relationshipRequest,
      )
      whenever(prisonerService.getPrisoner(any())).thenReturn(
        prisoner(
          relationshipRequest.prisonerNumber,
          prisonId = "MDI",
        ),
      )
      whenever(contactRepository.saveAndFlush(any())).thenAnswer { i -> (i.arguments[0] as ContactEntity).copy(contactId = 123) }
      whenever(prisonerContactRepository.saveAndFlush(any())).thenAnswer { i -> i.arguments[0] }
      val expectedException = ValidationException("Invalid")
      val referenceCode = ReferenceCode(1, expectedReferenceCodeGroup, "FRI", "Friend", 1, true)
      whenever(referenceCodeService.getReferenceDataByGroupAndCode(expectedReferenceCodeGroup, "FRI")).thenReturn(
        referenceCode,
      )
      whenever(
        referenceCodeService.validateReferenceCode(
          expectedReferenceCodeGroup,
          "FRI",
          allowInactive = false,
        ),
      ).thenThrow(expectedException)

      val exception = assertThrows<RuntimeException>("Bang!") {
        service.createContact(request, user)
      }

      assertThat(exception).isEqualTo(expectedException)
      verify(contactRepository, never()).saveAndFlush(any())
      verify(prisonerContactRepository, never()).saveAndFlush(any())
      verify(referenceCodeService).validateReferenceCode(expectedReferenceCodeGroup, "FRI", allowInactive = false)
    }

    @Test
    fun `should propagate exceptions creating a contact`() {
      val request = CreateContactRequest(
        lastName = "last",
        firstName = "first",
      )
      whenever(contactRepository.saveAndFlush(any())).thenThrow(RuntimeException("Bang!"))

      assertThrows<RuntimeException>("Bang!") {
        service.createContact(request, user)
      }
    }

    @Test
    fun `should propagate exceptions creating a contact with identities`() {
      val request = CreateContactRequest(
        lastName = "last",
        firstName = "first",
        identities = listOf(
          IdentityDocument(
            identityType = "PNC",
            identityValue = "1923/1Z34567A",
          ),
        ),
      )
      whenever(contactRepository.saveAndFlush(any())).thenAnswer { i -> (i.arguments[0] as ContactEntity).copy(contactId = 123) }
      whenever(contactAddressDetailsRepository.findByContactId(any())).thenReturn(listOf(aContactAddressDetailsEntity))
      whenever(contactIdentityService.createMultiple(any(), any(), any())).thenThrow(RuntimeException("Bang!"))

      assertThrows<RuntimeException>("Bang!") {
        service.createContact(request, user)
      }
    }

    @Test
    fun `should propagate exceptions creating a contact with addresses`() {
      val request = CreateContactRequest(
        lastName = "last",
        firstName = "first",
        addresses = listOf(createAddress()),
      )
      whenever(contactRepository.saveAndFlush(any())).thenAnswer { i -> (i.arguments[0] as ContactEntity).copy(contactId = 123) }
      whenever(contactAddressDetailsRepository.findByContactId(any())).thenReturn(listOf(aContactAddressDetailsEntity))
      whenever(contactAddressService.create(any(), any(), any())).thenThrow(RuntimeException("Bang!"))

      assertThrows<RuntimeException>("Bang!") {
        service.createContact(request, user)
      }
    }

    @Test
    fun `should propagate exceptions creating a contact with phone numbers`() {
      val request = CreateContactRequest(
        lastName = "last",
        firstName = "first",
        phoneNumbers = listOf(createPhoneNumber()),
      )
      whenever(contactRepository.saveAndFlush(any())).thenAnswer { i -> (i.arguments[0] as ContactEntity).copy(contactId = 123) }
      whenever(contactAddressDetailsRepository.findByContactId(any())).thenReturn(listOf(aContactAddressDetailsEntity))
      whenever(contactPhoneService.createMultiple(any(), any(), any())).thenThrow(RuntimeException("Bang!"))

      assertThrows<RuntimeException>("Bang!") {
        service.createContact(request, user)
      }
    }

    @Test
    fun `should propagate exceptions creating a contact with email addresses`() {
      val request = CreateContactRequest(
        lastName = "last",
        firstName = "first",
        emailAddresses = listOf(EmailAddress("test@example.com")),
      )
      whenever(contactRepository.saveAndFlush(any())).thenAnswer { i -> (i.arguments[0] as ContactEntity).copy(contactId = 123) }
      whenever(contactAddressDetailsRepository.findByContactId(any())).thenReturn(listOf(aContactAddressDetailsEntity))
      whenever(contactEmailService.createMultiple(any(), any(), any())).thenThrow(RuntimeException("Bang!"))

      assertThrows<RuntimeException>("Bang!") {
        service.createContact(request, user)
      }
    }

    @Test
    fun `should propagate exceptions creating a contact with employments`() {
      val request = CreateContactRequest(
        lastName = "last",
        firstName = "first",
        employments = listOf(Employment(1, true)),
      )
      whenever(contactRepository.saveAndFlush(any())).thenAnswer { i -> (i.arguments[0] as ContactEntity).copy(contactId = 123) }
      whenever(employmentService.createEmployment(any(), any(), any(), any())).thenThrow(RuntimeException("Bang!"))

      assertThrows<RuntimeException>("Bang!") {
        service.createContact(request, user)
      }
    }

    @Test
    fun `should propagate exceptions creating a contact relationship`() {
      val request = CreateContactRequest(
        lastName = "last",
        firstName = "first",
        relationship = ContactRelationship(
          prisonerNumber = "A1234BC",
          relationshipTypeCode = "S",
          relationshipToPrisonerCode = "FRI",
          isNextOfKin = true,
          isEmergencyContact = true,
          isApprovedVisitor = false,
          comments = "some comments",
        ),
      )
      whenever(prisonerService.getPrisoner(any())).thenReturn(prisoner("A1234BC", prisonId = "MDI"))
      whenever(contactRepository.saveAndFlush(any())).thenAnswer { i -> (i.arguments[0] as ContactEntity).copy(contactId = 123) }
      whenever(prisonerContactRepository.saveAndFlush(any())).thenThrow(RuntimeException("Bang!"))

      assertThrows<RuntimeException>("Bang!") {
        service.createContact(request, user)
      }

      verify(contactRepository).saveAndFlush(any())
      verify(prisonerContactRepository).saveAndFlush(any())
    }

    @Test
    fun `should throw EntityNotFoundException when prisoner can't be found and save nothing`() {
      val request = CreateContactRequest(
        lastName = "last",
        firstName = "first",
        relationship = ContactRelationship(
          prisonerNumber = "A1234BC",
          relationshipTypeCode = "S",
          relationshipToPrisonerCode = "FRI",
          isNextOfKin = true,
          isEmergencyContact = true,
          isApprovedVisitor = false,
          comments = "some comments",
        ),
      )
      whenever(prisonerService.getPrisoner(any())).thenReturn(null)
      whenever(contactRepository.saveAndFlush(any())).thenAnswer { i -> i.arguments[0] }
      whenever(prisonerContactRepository.saveAndFlush(any())).thenAnswer { i -> i.arguments[0] }

      assertThrows<EntityNotFoundException>("Prisoner number A1234BC - not found") {
        service.createContact(request, user)
      }

      verify(contactRepository, never()).saveAndFlush(any())
      verify(prisonerContactRepository, never()).saveAndFlush(any())
    }
  }

  @Nested
  inner class GetContact {
    private val contactId = 123456L

    @Test
    fun `should get a contact without dob successfully`() {
      whenever(contactAddressDetailsRepository.findByContactId(contactId)).thenReturn(
        listOf(
          aContactAddressDetailsEntity,
        ),
      )

      val entity = createContactEntity()
      whenever(contactRepository.findById(contactId)).thenReturn(Optional.of(entity))
      val contact = service.getContact(contactId)
      assertNotNull(contact)
      with(contact!!) {
        assertThat(id).isEqualTo(entity.contactId)
        assertThat(titleCode).isEqualTo(entity.title)
        assertThat(lastName).isEqualTo(entity.lastName)
        assertThat(firstName).isEqualTo(entity.firstName)
        assertThat(middleNames).isEqualTo(entity.middleNames)
        assertThat(dateOfBirth).isNull()
        assertThat(createdBy).isEqualTo(entity.createdBy)
        assertThat(createdTime).isEqualTo(entity.createdTime)
        assertThat(addresses).isEqualTo(listOf(aContactAddressDetailsEntity.toModel(emptyList())))
      }
    }

    @Test
    fun `should get a contact with phone numbers but exclude those attached to addresses`() {
      val aGeneralPhoneNumber = createContactPhoneDetailsEntity(id = 1, contactId = contactId)
      val aPhoneAttachedToAddress1 = createContactPhoneDetailsEntity(id = 2, contactId = contactId)
      val aPhoneAttachedToAddress2 = createContactPhoneDetailsEntity(id = 3, contactId = contactId)
      val anotherPhoneAttachedToAddress1 = createContactPhoneDetailsEntity(id = 4, contactId = contactId)
      val address1 = createContactAddressDetailsEntity(id = 1, contactId = contactId)
      val address2 = createContactAddressDetailsEntity(id = 2, contactId = contactId)

      whenever(contactAddressDetailsRepository.findByContactId(contactId)).thenReturn(listOf(address1, address2))
      whenever(contactPhoneDetailsRepository.findByContactId(contactId)).thenReturn(
        listOf(
          aGeneralPhoneNumber,
          aPhoneAttachedToAddress1,
          aPhoneAttachedToAddress2,
          anotherPhoneAttachedToAddress1,
        ),
      )
      whenever(contactAddressPhoneRepository.findByContactId(contactId)).thenReturn(
        listOf(
          ContactAddressPhoneEntity(1, contactId, 1, 2, createdBy = "TEST", createdTime = LocalDateTime.now()),
          ContactAddressPhoneEntity(1, contactId, 1, 4, createdBy = "TEST", createdTime = LocalDateTime.now()),
          ContactAddressPhoneEntity(1, contactId, 2, 3, createdBy = "TEST", createdTime = LocalDateTime.now()),
        ),
      )

      val entity = createContactEntity()
      whenever(contactRepository.findById(contactId)).thenReturn(Optional.of(entity))

      val contact = service.getContact(contactId)
      assertNotNull(contact)
      with(contact!!) {
        assertThat(id).isEqualTo(entity.contactId)

        // Should not include the 3 address-specific phone numbers
        assertThat(phoneNumbers).hasSize(1)
        assertThat(phoneNumbers[0].contactPhoneId).isEqualTo(1)

        assertThat(addresses).hasSize(2)
        assertThat(addresses[0].contactAddressId).isEqualTo(1)
        assertThat(addresses[0].phoneNumbers).hasSize(2)
        assertThat(addresses[0].phoneNumbers[0].contactPhoneId).isEqualTo(2)
        assertThat(addresses[0].phoneNumbers[1].contactPhoneId).isEqualTo(4)
        assertThat(addresses[1].phoneNumbers).hasSize(1)
        assertThat(addresses[1].contactAddressId).isEqualTo(2)
        assertThat(addresses[1].phoneNumbers[0].contactPhoneId).isEqualTo(3)
      }
    }

    @Test
    fun `should get a contact with email addresses`() {
      val emailAddressEntity1 = createContactEmailEntity(id = 1)
      val emailAddressEntity2 = createContactEmailEntity(id = 2)

      whenever(contactEmailRepository.findByContactId(contactId)).thenReturn(
        listOf(
          emailAddressEntity1,
          emailAddressEntity2,
        ),
      )

      val entity = createContactEntity()
      whenever(contactRepository.findById(contactId)).thenReturn(Optional.of(entity))

      val contact = service.getContact(contactId)
      assertNotNull(contact)
      with(contact!!) {
        assertThat(id).isEqualTo(entity.contactId)

        assertThat(emailAddresses).hasSize(2)
        assertThat(emailAddresses[0].contactEmailId).isEqualTo(1)
        assertThat(emailAddresses[1].contactEmailId).isEqualTo(2)
      }
    }

    @Test
    fun `should get a contact with identities`() {
      val identityEntity1 = createContactIdentityDetailsEntity(id = 1)
      val identityEntity2 = createContactIdentityDetailsEntity(id = 2)

      whenever(contactIdentityDetailsRepository.findByContactId(contactId)).thenReturn(
        listOf(
          identityEntity1,
          identityEntity2,
        ),
      )

      val entity = createContactEntity()
      whenever(contactRepository.findById(contactId)).thenReturn(Optional.of(entity))

      val contact = service.getContact(contactId)
      assertNotNull(contact)
      with(contact!!) {
        assertThat(id).isEqualTo(entity.contactId)

        assertThat(identities).hasSize(2)
        assertThat(identities[0].contactIdentityId).isEqualTo(1)
        assertThat(identities[1].contactIdentityId).isEqualTo(2)
      }
    }

    @Test
    fun `should get a contact with language code`() {
      val languageReference = ReferenceCode(1, ReferenceCodeGroup.LANGUAGE, "FRE-FRA", "French", 1, true)
      whenever(referenceCodeService.getReferenceDataByGroupAndCode(ReferenceCodeGroup.LANGUAGE, "FRE-FRA")).thenReturn(
        languageReference,
      )

      val entity = createContactEntity().copy(languageCode = "FRE-FRA", interpreterRequired = true)
      whenever(contactRepository.findById(contactId)).thenReturn(Optional.of(entity))

      val contact = service.getContact(contactId)

      assertNotNull(contact)
      with(contact!!) {
        assertThat(id).isEqualTo(entity.contactId)
        assertThat(languageCode).isEqualTo("FRE-FRA")
        assertThat(languageDescription).isEqualTo("French")
        assertThat(interpreterRequired).isTrue()
      }
    }

    @Test
    fun `should get a contact with a title code`() {
      val titleReference = ReferenceCode(1, ReferenceCodeGroup.TITLE, "MR", "Mr", 1, true)
      whenever(referenceCodeService.getReferenceDataByGroupAndCode(ReferenceCodeGroup.TITLE, "MR")).thenReturn(
        titleReference,
      )

      val entity = createContactEntity().copy(title = "MR")
      whenever(contactRepository.findById(contactId)).thenReturn(Optional.of(entity))

      val contact = service.getContact(contactId)

      assertNotNull(contact)
      with(contact!!) {
        assertThat(id).isEqualTo(entity.contactId)
        assertThat(titleCode).isEqualTo("MR")
        assertThat(titleDescription).isEqualTo("Mr")
      }
    }

    @Test
    fun `should get a contact if language code null and not lookup the null`() {
      val entity = createContactEntity().copy(languageCode = null)
      whenever(contactRepository.findById(contactId)).thenReturn(Optional.of(entity))

      val contact = service.getContact(contactId)
      assertNotNull(contact)
      with(contact!!) {
        assertThat(id).isEqualTo(entity.contactId)
        assertThat(languageCode).isNull()
        assertThat(languageDescription).isNull()
      }

      verify(referenceCodeService, never()).getReferenceDataByGroupAndCode(any(), any())
    }

    @Test
    fun `should get a contact with domestic status`() {
      whenever(referenceCodeService.getReferenceDataByGroupAndCode(ReferenceCodeGroup.DOMESTIC_STS, "S")).thenReturn(
        ReferenceCode(1, ReferenceCodeGroup.DOMESTIC_STS, "S", "Single", 1, true),
      )

      val entity = createContactEntity().copy(domesticStatus = "S")
      whenever(contactRepository.findById(contactId)).thenReturn(Optional.of(entity))

      val contact = service.getContact(contactId)
      assertNotNull(contact)
      with(contact!!) {
        assertThat(id).isEqualTo(entity.contactId)
        assertThat(domesticStatusCode).isEqualTo("S")
        assertThat(domesticStatusDescription).isEqualTo("Single")
      }
    }

    @Test
    fun `should get a contact with no domestic status and not look it up`() {
      val entity = createContactEntity().copy(domesticStatus = null)
      whenever(contactRepository.findById(contactId)).thenReturn(Optional.of(entity))

      val contact = service.getContact(contactId)
      assertNotNull(contact)
      with(contact!!) {
        assertThat(id).isEqualTo(entity.contactId)
        assertThat(domesticStatusCode).isNull()
        assertThat(domesticStatusDescription).isNull()
      }
      verify(referenceCodeService, never()).getReferenceDataByGroupAndCode(any(), any())
    }

    @Test
    fun `should get a contact with staff flag`() {
      val entity = createContactEntity().copy(staffFlag = true)
      whenever(contactRepository.findById(contactId)).thenReturn(Optional.of(entity))

      val contact = service.getContact(contactId)
      assertNotNull(contact)
      with(contact!!) {
        assertThat(id).isEqualTo(entity.contactId)
        assertThat(isStaff).isTrue()
      }
    }

    @Test
    fun `should get a contact with employments`() {
      val employments = listOf(createEmploymentDetails())
      whenever(employmentService.getEmploymentDetails(contactId)).thenReturn(employments)

      val entity = createContactEntity()
      whenever(contactRepository.findById(contactId)).thenReturn(Optional.of(entity))

      val contact = service.getContact(contactId)
      assertNotNull(contact)
      with(contact!!) {
        assertThat(id).isEqualTo(entity.contactId)

        assertThat(this.employments).isEqualTo(employments)
      }
    }

    @Test
    fun `should not blow up if contact not found`() {
      whenever(contactRepository.findById(contactId)).thenReturn(Optional.empty())
      val contact = service.getContact(contactId)
      assertNull(contact)
    }

    @Test
    fun `should propagate exceptions getting a contact`() {
      whenever(contactRepository.findById(contactId)).thenThrow(RuntimeException("Bang!"))

      assertThrows<RuntimeException>("Bang!") {
        service.getContact(contactId)
      }
    }
  }

  @Nested
  inner class AddContactRelationship {
    private val user = aUser("RELATIONSHIP_USER")
    private val contactId = 123456L
    private val relationship = ContactRelationship(
      prisonerNumber = "A1234BC",
      relationshipTypeCode = "S",
      relationshipToPrisonerCode = "MOT",
      isNextOfKin = true,
      isEmergencyContact = false,
      isApprovedVisitor = false,
      comments = "Foo",
    )
    private val request = AddContactRelationshipRequest(contactId, relationship)
    private val contact = ContactEntity(
      contactId = contactId,
      title = null,
      lastName = "last",
      middleNames = null,
      firstName = "first",
      dateOfBirth = null,
      deceasedDate = null,
      createdBy = "user",
      createdTime = LocalDateTime.now(),
    )

    @ParameterizedTest
    @CsvSource(value = ["true", "false"])
    fun `should save the contact relationship with correct approved visitor details`(updatingApprovedVisitor: Boolean) {
      val relationship = relationship.copy(isApprovedVisitor = updatingApprovedVisitor)
      val request = AddContactRelationshipRequest(contactId, relationship)

      whenever(prisonerService.getPrisoner(any())).thenReturn(
        prisoner(
          request.relationship.prisonerNumber,
          prisonId = "MDI",
        ),
      )
      whenever(contactRepository.findById(contactId)).thenReturn(Optional.of(contact))
      whenever(prisonerContactRepository.saveAndFlush(any())).thenAnswer { i -> i.arguments[0] }
      val referenceCode = ReferenceCode(1, ReferenceCodeGroup.SOCIAL_RELATIONSHIP, "MOT", "Mother", 1, true)
      whenever(
        referenceCodeService.getReferenceDataByGroupAndCode(
          ReferenceCodeGroup.SOCIAL_RELATIONSHIP,
          "MOT",
        ),
      ).thenReturn(referenceCode)
      whenever(
        referenceCodeService.validateReferenceCode(
          ReferenceCodeGroup.SOCIAL_RELATIONSHIP,
          "MOT",
          allowInactive = false,
        ),
      ).thenReturn(referenceCode)

      service.addContactRelationship(request, user)

      val prisonerContactCaptor = argumentCaptor<PrisonerContactEntity>()
      verify(prisonerContactRepository).saveAndFlush(prisonerContactCaptor.capture())
      with(prisonerContactCaptor.firstValue) {
        assertThat(prisonerNumber).isEqualTo("A1234BC")
        assertThat(relationshipToPrisoner).isEqualTo("MOT")
        assertThat(nextOfKin).isEqualTo(true)
        assertThat(emergencyContact).isEqualTo(false)
        assertThat(comments).isEqualTo("Foo")
        assertThat(createdBy).isEqualTo("RELATIONSHIP_USER")
        assertThat(approvedVisitor).isEqualTo(updatingApprovedVisitor)
        assertThat(approvedBy).isEqualTo("RELATIONSHIP_USER")
        assertThat(approvedTime).isInThePast()
      }
      verify(referenceCodeService).validateReferenceCode(
        ReferenceCodeGroup.SOCIAL_RELATIONSHIP,
        "MOT",
        allowInactive = false,
      )
    }

    @Test
    fun `should blow up if prisoner not found`() {
      whenever(prisonerService.getPrisoner(any())).thenReturn(null)

      assertThrows<EntityNotFoundException>("Prisoner (A1234BC) could not be found") {
        service.addContactRelationship(request, user)
      }
    }

    @Test
    fun `should blow up if contact not found`() {
      whenever(contactRepository.findById(contactId)).thenReturn(Optional.empty())

      assertThrows<EntityNotFoundException>("Contact ($contactId) could not be found") {
        service.addContactRelationship(request, user)
      }
    }

    @Test
    fun `should propagate exceptions adding a relationship`() {
      whenever(prisonerService.getPrisoner(any())).thenReturn(
        prisoner(
          request.relationship.prisonerNumber,
          prisonId = "MDI",
        ),
      )
      whenever(contactRepository.findById(contactId)).thenReturn(Optional.of(contact))
      whenever(prisonerContactRepository.saveAndFlush(any())).thenThrow(RuntimeException("Bang!"))

      assertThrows<RuntimeException>("Bang!") {
        service.addContactRelationship(request, user)
      }
    }
  }

  @Nested
  inner class UpdateContactRelationship {
    private val prisonerContactId = 2L

    private lateinit var prisonerContact: PrisonerContactEntity

    @BeforeEach
    fun before() {
      prisonerContact = createPrisonerContact()
    }

    @Nested
    inner class RelationshipTypeAndRelationshipToPrisoner {
      private val user = aUser("Admin")

      @ParameterizedTest
      @CsvSource(
        value = [
          "S,SOCIAL_RELATIONSHIP",
          "O,OFFICIAL_RELATIONSHIP",
        ],
      )
      fun `should update the contact relationship type using original relationship type code if not specified`(
        relationshipType: String,
        expectedReferenceCodeGroup: ReferenceCodeGroup,
      ) {
        val relationShipTypeCode = "FRI"
        prisonerContact = prisonerContact.copy(relationshipType = relationshipType).apply {
          approvedBy = "officer456"
          approvedTime = LocalDateTime.now()
          expiryDate = LocalDate.of(2025, 12, 31)
          createdAtPrison = "LONDON"
          updatedBy = "adminUser"
          updatedTime = LocalDateTime.now()
        }
        val request = PatchRelationshipRequest(
          relationshipTypeCode = JsonNullable.undefined(),
          relationshipToPrisonerCode = JsonNullable.of(relationShipTypeCode),
        )
        mockBrotherRelationshipReferenceCode()
        whenever(
          referenceCodeService.getReferenceDataByGroupAndCode(
            expectedReferenceCodeGroup,
            relationShipTypeCode,
          ),
        ).thenReturn(
          ReferenceCode(1, expectedReferenceCodeGroup, "FRI", "Friend", 1, true),
        )

        whenever(
          referenceCodeService.getReferenceDataByGroupAndCode(
            expectedReferenceCodeGroup,
            relationShipTypeCode,
          ),
        ).thenReturn(
          ReferenceCode(1, expectedReferenceCodeGroup, "FRI", "Friend", 1, true),
        )

        whenever(prisonerContactRepository.findById(prisonerContactId)).thenReturn(Optional.of(prisonerContact))
        whenever(prisonerContactRepository.saveAndFlush(any())).thenAnswer { i -> i.arguments[0] }

        service.updateContactRelationship(prisonerContactId, request, user)

        val prisonerContactCaptor = argumentCaptor<PrisonerContactEntity>()
        verify(referenceCodeService).validateReferenceCode(expectedReferenceCodeGroup, "FRI", allowInactive = true)
        verify(prisonerContactRepository).saveAndFlush(prisonerContactCaptor.capture())
        with(prisonerContactCaptor.firstValue) {
          // assert changed
          assertThat(relationshipToPrisoner).isEqualTo("FRI")
          assertThat(updatedBy).isEqualTo("Admin")
          assertThat(updatedTime).isInThePast()
          // assert unchanged
          assertThat(this.relationshipType).isEqualTo(relationshipType)
          assertThat(nextOfKin).isTrue()
          assertThat(emergencyContact).isTrue()
          assertThat(active).isTrue()
          assertThat(comments).isEqualTo("Updated relationship type to Brother")

          assertUnchangedFields()
        }
      }

      @ParameterizedTest
      @CsvSource(
        value = [
          "O,S,SOCIAL_RELATIONSHIP",
          "S,O,OFFICIAL_RELATIONSHIP",
        ],
      )
      fun `should update the contact relationship type using new relationship type code`(
        originalRelationshipType: String,
        newRelationshipType: String,
        expectedReferenceCodeGroup: ReferenceCodeGroup,
      ) {
        val relationshipToPrisonerCode = "FRI"
        prisonerContact = prisonerContact.copy(relationshipType = originalRelationshipType).apply {
          approvedBy = "officer456"
          approvedTime = LocalDateTime.now()
          expiryDate = LocalDate.of(2025, 12, 31)
          createdAtPrison = "LONDON"
          updatedBy = "adminUser"
          updatedTime = LocalDateTime.now()
        }
        val request = PatchRelationshipRequest(
          relationshipTypeCode = JsonNullable.of(newRelationshipType),
          relationshipToPrisonerCode = JsonNullable.of(relationshipToPrisonerCode),
        )
        mockBrotherRelationshipReferenceCode()
        whenever(
          referenceCodeService.getReferenceDataByGroupAndCode(
            expectedReferenceCodeGroup,
            relationshipToPrisonerCode,
          ),
        ).thenReturn(
          ReferenceCode(1, expectedReferenceCodeGroup, "FRI", "Friend", 1, true),
        )

        whenever(
          referenceCodeService.getReferenceDataByGroupAndCode(
            expectedReferenceCodeGroup,
            relationshipToPrisonerCode,
          ),
        ).thenReturn(
          ReferenceCode(1, expectedReferenceCodeGroup, "FRI", "Friend", 1, true),
        )

        whenever(prisonerContactRepository.findById(prisonerContactId)).thenReturn(Optional.of(prisonerContact))
        whenever(prisonerContactRepository.saveAndFlush(any())).thenAnswer { i -> i.arguments[0] }

        service.updateContactRelationship(prisonerContactId, request, user)

        val prisonerContactCaptor = argumentCaptor<PrisonerContactEntity>()
        verify(referenceCodeService).validateReferenceCode(expectedReferenceCodeGroup, "FRI", allowInactive = true)
        verify(prisonerContactRepository).saveAndFlush(prisonerContactCaptor.capture())
        with(prisonerContactCaptor.firstValue) {
          // assert changed
          assertThat(relationshipType).isEqualTo(newRelationshipType)
          assertThat(relationshipToPrisoner).isEqualTo("FRI")
          assertThat(updatedBy).isEqualTo("Admin")
          assertThat(updatedTime).isInThePast()
          // assert unchanged
          assertThat(nextOfKin).isTrue()
          assertThat(emergencyContact).isTrue()
          assertThat(active).isTrue()
          assertThat(comments).isEqualTo("Updated relationship type to Brother")

          assertUnchangedFields()
        }
      }

      @ParameterizedTest
      @CsvSource(
        value = [
          "O,S,SOCIAL_RELATIONSHIP",
          "S,O,OFFICIAL_RELATIONSHIP",
        ],
      )
      fun `should re-validate relationship to prisoner even if only relationship type is changing`(
        originalRelationshipType: String,
        newRelationshipType: String,
        expectedReferenceCodeGroup: ReferenceCodeGroup,
      ) {
        prisonerContact = prisonerContact.copy(relationshipType = originalRelationshipType).apply {
          approvedBy = "officer456"
          approvedTime = LocalDateTime.now()
          expiryDate = LocalDate.of(2025, 12, 31)
          createdAtPrison = "LONDON"
          updatedBy = "adminUser"
          updatedTime = LocalDateTime.now()
        }
        val request = PatchRelationshipRequest(
          relationshipTypeCode = JsonNullable.of(newRelationshipType),
          relationshipToPrisonerCode = JsonNullable.undefined(),
        )
        mockBrotherRelationshipReferenceCode()
        whenever(referenceCodeService.getReferenceDataByGroupAndCode(expectedReferenceCodeGroup, "BRO")).thenReturn(
          ReferenceCode(1, expectedReferenceCodeGroup, "BRO", "Brother", 1, true),
        )

        whenever(referenceCodeService.getReferenceDataByGroupAndCode(expectedReferenceCodeGroup, "BRO")).thenReturn(
          ReferenceCode(1, expectedReferenceCodeGroup, "BRO", "Brother", 1, true),
        )

        whenever(prisonerContactRepository.findById(prisonerContactId)).thenReturn(Optional.of(prisonerContact))
        whenever(prisonerContactRepository.saveAndFlush(any())).thenAnswer { i -> i.arguments[0] }

        service.updateContactRelationship(prisonerContactId, request, user)

        val prisonerContactCaptor = argumentCaptor<PrisonerContactEntity>()
        verify(referenceCodeService).validateReferenceCode(expectedReferenceCodeGroup, "BRO", allowInactive = true)
        verify(prisonerContactRepository).saveAndFlush(prisonerContactCaptor.capture())
        with(prisonerContactCaptor.firstValue) {
          // assert changed
          assertThat(relationshipType).isEqualTo(newRelationshipType)
          assertThat(updatedBy).isEqualTo("Admin")
          assertThat(updatedTime).isInThePast()
          // assert unchanged
          assertThat(relationshipToPrisoner).isEqualTo("BRO")
          assertThat(nextOfKin).isTrue()
          assertThat(emergencyContact).isTrue()
          assertThat(active).isTrue()
          assertThat(comments).isEqualTo("Updated relationship type to Brother")

          assertUnchangedFields()
        }
      }

      @Test
      fun `should reject a duplicate relationship if it is a different id`() {
        prisonerContact = prisonerContact.copy(relationshipToPrisoner = "BRO")
        val otherExistingRelationshipWithSisCode =
          prisonerContact.copy(prisonerContactId = 123456789, relationshipToPrisoner = "SIS")

        val request = PatchRelationshipRequest(
          relationshipToPrisonerCode = JsonNullable.of("SIS"),
        )
        mockBrotherRelationshipReferenceCode()
        whenever(
          referenceCodeService.getReferenceDataByGroupAndCode(
            ReferenceCodeGroup.SOCIAL_RELATIONSHIP,
            "SIS",
          ),
        ).thenReturn(
          ReferenceCode(1, ReferenceCodeGroup.SOCIAL_RELATIONSHIP, "SIS", "Sister", 1, true),
        )
        whenever(
          prisonerContactRepository.findDuplicateRelationships(
            prisonerContact.prisonerNumber,
            prisonerContact.contactId,
            "SIS",
          ),
        )
          .thenReturn(listOf(otherExistingRelationshipWithSisCode))
        whenever(prisonerContactRepository.findById(prisonerContactId)).thenReturn(Optional.of(prisonerContact))
        whenever(prisonerContactRepository.saveAndFlush(any())).thenAnswer { i -> i.arguments[0] }

        assertThrows<DuplicateRelationshipException> {
          service.updateContactRelationship(prisonerContactId, request, user)
        }
        verify(prisonerContactRepository, never()).saveAndFlush(any())
      }

      @Test
      fun `should skip duplicate relationship check if we're not changing it`() {
        prisonerContact = prisonerContact.copy(relationshipToPrisoner = "BRO")
        val otherExistingRelationshipWithSisCode =
          prisonerContact.copy(prisonerContactId = 123456789, relationshipToPrisoner = "BRO")

        val request = PatchRelationshipRequest(
          relationshipToPrisonerCode = JsonNullable.undefined(),
        )
        mockBrotherRelationshipReferenceCode()
        whenever(
          prisonerContactRepository.findDuplicateRelationships(
            prisonerContact.prisonerNumber,
            prisonerContact.contactId,
            "SIS",
          ),
        )
          .thenReturn(listOf(prisonerContact, otherExistingRelationshipWithSisCode))
        whenever(prisonerContactRepository.findById(prisonerContactId)).thenReturn(Optional.of(prisonerContact))
        whenever(prisonerContactRepository.saveAndFlush(any())).thenAnswer { i -> i.arguments[0] }
        service.updateContactRelationship(prisonerContactId, request, user)
        verify(prisonerContactRepository).saveAndFlush(any())
        verify(prisonerContactRepository, never()).findDuplicateRelationships(any(), any(), any())
      }

      @Test
      fun `should not flag as a duplicate if the only existing matching relationship is the one being updated`() {
        prisonerContact = prisonerContact.copy(relationshipToPrisoner = "BRO")

        val request = PatchRelationshipRequest(
          relationshipToPrisonerCode = JsonNullable.of("BRO"),
        )
        mockBrotherRelationshipReferenceCode()
        whenever(
          prisonerContactRepository.findDuplicateRelationships(
            prisonerContact.prisonerNumber,
            prisonerContact.contactId,
            "BRO",
          ),
        )
          .thenReturn(listOf(prisonerContact))
        whenever(prisonerContactRepository.findById(prisonerContactId)).thenReturn(Optional.of(prisonerContact))
        whenever(prisonerContactRepository.saveAndFlush(any())).thenAnswer { i -> i.arguments[0] }
        service.updateContactRelationship(prisonerContactId, request, user)
        verify(prisonerContactRepository).saveAndFlush(any())
      }

      @Test
      fun `should not update relationship to prisoner with null`() {
        val request = PatchRelationshipRequest(
          relationshipToPrisonerCode = JsonNullable.of(null),
        )

        whenever(prisonerContactRepository.findById(prisonerContactId)).thenReturn(Optional.of(prisonerContact))
        whenever(prisonerContactRepository.saveAndFlush(any())).thenAnswer { i -> i.arguments[0] }

        val exception = assertThrows<ValidationException> {
          service.updateContactRelationship(prisonerContactId, request, user)
        }
        assertThat(exception.message).isEqualTo("Unsupported relationship to prisoner null.")
      }

      @Test
      fun `should not update relationship type with null`() {
        val request = PatchRelationshipRequest(
          relationshipTypeCode = JsonNullable.of(null),
        )

        whenever(prisonerContactRepository.findById(prisonerContactId)).thenReturn(Optional.of(prisonerContact))
        whenever(prisonerContactRepository.saveAndFlush(any())).thenAnswer { i -> i.arguments[0] }

        val exception = assertThrows<ValidationException> {
          service.updateContactRelationship(prisonerContactId, request, user)
        }
        assertThat(exception.message).isEqualTo("Unsupported relationship type null.")
      }

      @Test
      fun `should not update relationship type with invalid type`() {
        val request = PatchRelationshipRequest(
          relationshipToPrisonerCode = JsonNullable.of("OOO"),
        )

        whenever(prisonerContactRepository.findById(prisonerContactId)).thenReturn(Optional.of(prisonerContact))
        whenever(prisonerContactRepository.saveAndFlush(any())).thenAnswer { i -> i.arguments[0] }
        val expectedException = ValidationException("Invalid")
        whenever(
          referenceCodeService.validateReferenceCode(
            ReferenceCodeGroup.SOCIAL_RELATIONSHIP,
            "OOO",
            allowInactive = true,
          ),
        ).thenThrow(expectedException)

        val exception = assertThrows<ValidationException> {
          service.updateContactRelationship(prisonerContactId, request, user)
        }
        assertThat(exception).isEqualTo(expectedException)
      }
    }

    @Nested
    inner class ApprovedVisitor {
      private val user = aUser("Admin")

      @ParameterizedTest
      @CsvSource(
        "true,true,officer456",
        "true,false, Admin",
        "false,true, Admin",
        "false,false,officer456",

      )
      fun `should update the approved visitor , approved by user and approved time`(updatingApprovedVisitor: Boolean, savedApprovedVisitorValue: Boolean, expectedApprovedBy: String) {
        val request = PatchRelationshipRequest(
          isApprovedVisitor = JsonNullable.of(updatingApprovedVisitor),
        )
        mockBrotherRelationshipReferenceCode()
        val contactEntity = prisonerContact.copy(approvedVisitor = savedApprovedVisitorValue).also {
          it.approvedBy = "officer456" // default approved by user
          it.approvedTime = LocalDateTime.now().minusDays(1)
        }
        whenever(prisonerContactRepository.findById(prisonerContactId)).thenReturn(Optional.of(contactEntity))
        whenever(prisonerContactRepository.saveAndFlush(any())).thenAnswer { i -> i.arguments[0] }

        service.updateContactRelationship(prisonerContactId, request, user)

        val prisonerContactCaptor = argumentCaptor<PrisonerContactEntity>()
        verify(prisonerContactRepository).saveAndFlush(prisonerContactCaptor.capture())
        with(prisonerContactCaptor.firstValue) {
          // assert changed
          assertThat(this.approvedVisitor).isEqualTo(updatingApprovedVisitor)
          assertThat(approvedBy).isEqualTo(expectedApprovedBy)
          assertThat(approvedTime).isInThePast()
          assertThat(updatedBy).isEqualTo("Admin")
          assertThat(updatedTime).isInThePast()
        }
      }

      @Test
      fun `should not update approved visitor with null`() {
        val request = PatchRelationshipRequest(
          isApprovedVisitor = JsonNullable.of(null),
        )

        whenever(prisonerContactRepository.findById(prisonerContactId)).thenReturn(Optional.of(prisonerContact))
        whenever(prisonerContactRepository.saveAndFlush(any())).thenAnswer { i -> i.arguments[0] }

        val exception = assertThrows<ValidationException> {
          service.updateContactRelationship(prisonerContactId, request, user)
        }
        assertThat(exception.message).isEqualTo("Unsupported approved visitor value null.")
      }
    }

    @Nested
    inner class NextOfKin {
      private val user = aUser("Admin")

      @Test
      fun `should update the next of kin`() {
        val request = PatchRelationshipRequest(
          isNextOfKin = JsonNullable.of(false),
        )
        mockBrotherRelationshipReferenceCode()
        whenever(prisonerContactRepository.findById(prisonerContactId)).thenReturn(Optional.of(prisonerContact))
        whenever(prisonerContactRepository.saveAndFlush(any())).thenAnswer { i -> i.arguments[0] }

        service.updateContactRelationship(prisonerContactId, request, user)

        val prisonerContactCaptor = argumentCaptor<PrisonerContactEntity>()
        verify(prisonerContactRepository).saveAndFlush(prisonerContactCaptor.capture())
        with(prisonerContactCaptor.firstValue) {
          // assert changed
          assertThat(nextOfKin).isFalse()
          assertThat(updatedBy).isEqualTo("Admin")
          assertThat(updatedTime).isInThePast()
          // assert unchanged
          assertThat(relationshipType).isEqualTo("S")
          assertThat(relationshipToPrisoner).isEqualTo("BRO")
          assertThat(emergencyContact).isTrue()
          assertThat(active).isTrue()
          assertThat(comments).isEqualTo("Updated relationship type to Brother")

          assertUnchangedFields()
        }
      }

      @Test
      fun `should not update relationship next of kin with null`() {
        val request = PatchRelationshipRequest(
          isNextOfKin = JsonNullable.of(null),
        )

        whenever(prisonerContactRepository.findById(prisonerContactId)).thenReturn(Optional.of(prisonerContact))
        whenever(prisonerContactRepository.saveAndFlush(any())).thenAnswer { i -> i.arguments[0] }

        val exception = assertThrows<ValidationException> {
          service.updateContactRelationship(prisonerContactId, request, user)
        }
        assertThat(exception.message).isEqualTo("Unsupported next of kin null.")
      }
    }

    @Nested
    inner class EmergencyContactStatus {
      private val user = aUser("Admin")

      @Test
      fun `should update the emergency contact status`() {
        val request = PatchRelationshipRequest(
          isEmergencyContact = JsonNullable.of(false),
        )
        mockBrotherRelationshipReferenceCode()
        whenever(prisonerContactRepository.findById(prisonerContactId)).thenReturn(Optional.of(prisonerContact))
        whenever(prisonerContactRepository.saveAndFlush(any())).thenAnswer { i -> i.arguments[0] }

        service.updateContactRelationship(prisonerContactId, request, user)

        val prisonerContactCaptor = argumentCaptor<PrisonerContactEntity>()
        verify(prisonerContactRepository).saveAndFlush(prisonerContactCaptor.capture())
        with(prisonerContactCaptor.firstValue) {
          // assert changed
          assertThat(emergencyContact).isFalse()
          assertThat(updatedBy).isEqualTo("Admin")
          assertThat(updatedTime).isInThePast()
          // assert unchanged
          assertThat(relationshipType).isEqualTo("S")
          assertThat(relationshipToPrisoner).isEqualTo("BRO")
          assertThat(nextOfKin).isTrue()
          assertThat(active).isTrue()
          assertThat(comments).isEqualTo("Updated relationship type to Brother")

          assertUnchangedFields()
        }
      }

      @Test
      fun `should not update relationship emergency contact with null`() {
        val request = PatchRelationshipRequest(
          isEmergencyContact = JsonNullable.of(null),
        )

        whenever(prisonerContactRepository.findById(prisonerContactId)).thenReturn(Optional.of(prisonerContact))
        whenever(prisonerContactRepository.saveAndFlush(any())).thenAnswer { i -> i.arguments[0] }

        val exception = assertThrows<ValidationException> {
          service.updateContactRelationship(prisonerContactId, request, user)
        }
        assertThat(exception.message).isEqualTo("Unsupported emergency contact null.")
      }
    }

    @Nested
    inner class RelationshipActiveStatus {
      private val user = aUser("Admin")

      @Test
      fun `should update the relationship active status`() {
        val request = PatchRelationshipRequest(
          isRelationshipActive = JsonNullable.of(false),
        )
        mockBrotherRelationshipReferenceCode()
        whenever(prisonerContactRepository.findById(prisonerContactId)).thenReturn(Optional.of(prisonerContact))
        whenever(prisonerContactRepository.saveAndFlush(any())).thenAnswer { i -> i.arguments[0] }

        service.updateContactRelationship(prisonerContactId, request, user)

        val prisonerContactCaptor = argumentCaptor<PrisonerContactEntity>()
        verify(prisonerContactRepository).saveAndFlush(prisonerContactCaptor.capture())
        with(prisonerContactCaptor.firstValue) {
          // assert changed
          assertThat(active).isFalse()
          assertThat(updatedBy).isEqualTo("Admin")
          assertThat(updatedTime).isInThePast()
          // assert unchanged
          assertThat(relationshipType).isEqualTo("S")
          assertThat(relationshipToPrisoner).isEqualTo("BRO")
          assertThat(nextOfKin).isTrue()
          assertThat(emergencyContact).isTrue()
          assertThat(comments).isEqualTo("Updated relationship type to Brother")

          assertUnchangedFields()
        }
      }

      @Test
      fun `should not update relationship active status with null`() {
        val request = PatchRelationshipRequest(
          isRelationshipActive = JsonNullable.of(null),
        )

        whenever(prisonerContactRepository.findById(prisonerContactId)).thenReturn(Optional.of(prisonerContact))
        whenever(prisonerContactRepository.saveAndFlush(any())).thenAnswer { i -> i.arguments[0] }

        val exception = assertThrows<ValidationException> {
          service.updateContactRelationship(prisonerContactId, request, user)
        }
        assertThat(exception.message).isEqualTo("Unsupported relationship status null.")
      }
    }

    @Nested
    inner class RelationshipComment {
      private val user = aUser("Admin")

      @Test
      fun `should update the contact relationship comment`() {
        val relationShipTypeCode = "FRI"
        val request = PatchRelationshipRequest(
          comments = JsonNullable.of("a comment"),
        )
        mockBrotherRelationshipReferenceCode()
        whenever(
          referenceCodeService.getReferenceDataByGroupAndCode(
            ReferenceCodeGroup.SOCIAL_RELATIONSHIP,
            relationShipTypeCode,
          ),
        ).thenReturn(
          ReferenceCode(1, ReferenceCodeGroup.SOCIAL_RELATIONSHIP, "FRI", "Friend", 1, true),
        )

        whenever(prisonerContactRepository.findById(prisonerContactId)).thenReturn(Optional.of(prisonerContact))
        whenever(prisonerContactRepository.saveAndFlush(any())).thenAnswer { i -> i.arguments[0] }

        service.updateContactRelationship(prisonerContactId, request, user)

        val prisonerContactCaptor = argumentCaptor<PrisonerContactEntity>()
        verify(prisonerContactRepository).saveAndFlush(prisonerContactCaptor.capture())
        with(prisonerContactCaptor.firstValue) {
          // assert changed
          assertThat(comments).isEqualTo("a comment")
          assertThat(updatedBy).isEqualTo("Admin")
          assertThat(updatedTime).isInThePast()
          // assert unchanged
          assertThat(relationshipType).isEqualTo("S")
          assertThat(relationshipToPrisoner).isEqualTo("BRO")
          assertThat(nextOfKin).isTrue()
          assertThat(emergencyContact).isTrue()
          assertThat(active).isTrue()

          assertUnchangedFields()
        }
      }

      @Test
      fun `should update the contact relationship comment to null`() {
        val relationShipTypeCode = "FRI"
        val request = PatchRelationshipRequest(
          comments = JsonNullable.of(null),
        )
        mockBrotherRelationshipReferenceCode()
        whenever(
          referenceCodeService.getReferenceDataByGroupAndCode(
            ReferenceCodeGroup.SOCIAL_RELATIONSHIP,
            relationShipTypeCode,
          ),
        ).thenReturn(
          ReferenceCode(1, ReferenceCodeGroup.SOCIAL_RELATIONSHIP, "FRI", "Friend", 1, true),
        )

        whenever(prisonerContactRepository.findById(prisonerContactId)).thenReturn(Optional.of(prisonerContact))
        whenever(prisonerContactRepository.saveAndFlush(any())).thenAnswer { i -> i.arguments[0] }

        service.updateContactRelationship(prisonerContactId, request, user)

        val prisonerContactCaptor = argumentCaptor<PrisonerContactEntity>()
        verify(prisonerContactRepository).saveAndFlush(prisonerContactCaptor.capture())
        with(prisonerContactCaptor.firstValue) {
          // assert changed
          assertThat(comments).isNull()
          assertThat(updatedBy).isEqualTo("Admin")
          assertThat(updatedTime).isInThePast()
          // assert unchanged
          assertThat(relationshipType).isEqualTo("S")
          assertThat(relationshipToPrisoner).isEqualTo("BRO")
          assertThat(nextOfKin).isTrue()
          assertThat(emergencyContact).isTrue()
          assertThat(active).isTrue()

          assertUnchangedFields()
        }
      }
    }

    @Test
    fun `should update when only updated by filed is provided`() {
      val user = aUser("Admin")
      val request = PatchRelationshipRequest()

      mockBrotherRelationshipReferenceCode()
      whenever(prisonerContactRepository.findById(prisonerContactId)).thenReturn(Optional.of(prisonerContact))
      whenever(prisonerContactRepository.saveAndFlush(any())).thenAnswer { i -> i.arguments[0] }

      service.updateContactRelationship(prisonerContactId, request, user)

      val prisonerContactCaptor = argumentCaptor<PrisonerContactEntity>()
      verify(prisonerContactRepository).saveAndFlush(prisonerContactCaptor.capture())
      with(prisonerContactCaptor.firstValue) {
        // assert changed

        assertThat(updatedBy).isEqualTo("Admin")
        assertThat(updatedTime).isInThePast()
        // assert unchanged
        assertThat(relationshipType).isEqualTo("S")
        assertThat(nextOfKin).isTrue()
        assertThat(relationshipToPrisoner).isEqualTo("BRO")
        assertThat(emergencyContact).isTrue()
        assertThat(active).isTrue()
        assertThat(comments).isEqualTo("Updated relationship type to Brother")

        assertUnchangedFields()
      }
    }

    @Test
    fun `should blow up if prisoner contact not found`() {
      val user = aUser("Admin")
      val request = updateRelationshipRequest()
      whenever(prisonerContactRepository.findById(prisonerContactId)).thenReturn(Optional.empty())

      val exception = assertThrows<EntityNotFoundException> {
        service.updateContactRelationship(prisonerContactId, request, user)
      }
      assertThat(exception.message).isEqualTo("Prisoner contact with prisoner contact ID 2 not found")
    }

    @Test
    fun `should propagate exceptions updating a prisoner contact relationship`() {
      val user = aUser("Admin")
      val request = updateRelationshipRequest()
      whenever(prisonerContactRepository.findById(prisonerContactId)).thenReturn(Optional.of(prisonerContact))
      whenever(prisonerContactRepository.saveAndFlush(any())).thenThrow(RuntimeException("Bang!"))

      assertThrows<RuntimeException>("Bang!") {
        service.updateContactRelationship(prisonerContactId, request, user)
      }
    }

    @Test
    fun `should get contact names with a title`() {
      val contactId: Long = 12345
      val contactEntity = createContactEntity().copy(
        contactId = contactId,
        title = "MR",
        firstName = "First",
        lastName = "Last",
        middleNames = "Middle Names",
      )
      val titleReference = ReferenceCode(1, ReferenceCodeGroup.TITLE, "MR", "Mr", 1, true)
      whenever(referenceCodeService.getReferenceDataByGroupAndCode(ReferenceCodeGroup.TITLE, "MR")).thenReturn(
        titleReference,
      )
      whenever(contactRepository.findById(contactId)).thenReturn(Optional.of(contactEntity))

      val names = service.getContactName(contactId)

      assertThat(names).isEqualTo(
        ContactNameDetails(
          titleCode = "MR",
          titleDescription = "Mr",
          firstName = "First",
          lastName = "Last",
          middleNames = "Middle Names",
        ),
      )
    }

    @Test
    fun `should get contact names without a title`() {
      val contactId: Long = 12345
      val contactEntity = createContactEntity().copy(
        contactId = contactId,
        title = null,
        firstName = "First",
        lastName = "Last",
        middleNames = null,
      )
      whenever(contactRepository.findById(contactId)).thenReturn(Optional.of(contactEntity))

      val names = service.getContactName(contactId)

      assertThat(names).isEqualTo(
        ContactNameDetails(
          titleCode = null,
          titleDescription = null,
          firstName = "First",
          lastName = "Last",
          middleNames = null,
        ),
      )
      verify(referenceCodeService, never()).getReferenceDataByGroupAndCode(any(), any())
    }

    private fun createPrisonerContact(): PrisonerContactEntity = PrisonerContactEntity(
      prisonerContactId = prisonerContactId,
      contactId = 1L,
      prisonerNumber = "A1234BC",
      relationshipType = "S",
      relationshipToPrisoner = "BRO",
      nextOfKin = true,
      emergencyContact = true,
      approvedVisitor = true,
      active = true,
      currentTerm = true,
      comments = "Updated relationship type to Brother",
      createdBy = "TEST",
      createdTime = LocalDateTime.now(),
    ).apply {
      approvedBy = "officer456"
      approvedTime = LocalDateTime.now()
      expiryDate = LocalDate.of(2025, 12, 31)
      createdAtPrison = "LONDON"
      updatedBy = "adminUser"
      updatedTime = LocalDateTime.now()
    }

    private fun PrisonerContactEntity.assertUnchangedFields() {
      assertThat(prisonerNumber).isEqualTo("A1234BC")
      assertThat(currentTerm).isTrue()
      assertThat(approvedVisitor).isTrue()
      assertThat(createdBy).isEqualTo("TEST")
      assertThat(createdTime).isInThePast()
      assertThat(approvedBy).isEqualTo("officer456")
      assertThat(approvedTime).isInThePast()
      assertThat(expiryDate).isEqualTo(LocalDate.of(2025, 12, 31))
      assertThat(createdAtPrison).isEqualTo("LONDON")
    }

    private fun updateRelationshipRequest(): PatchRelationshipRequest = PatchRelationshipRequest(
      relationshipToPrisonerCode = JsonNullable.of("MOT"),
      isEmergencyContact = JsonNullable.of(true),
      isNextOfKin = JsonNullable.of(true),
      isRelationshipActive = JsonNullable.of(false),
      comments = JsonNullable.of("Foo"),
    )
  }

  private fun createContactEntity() = ContactEntity(
    contactId = 123456L,
    title = null,
    lastName = "last",
    middleNames = "middle",
    firstName = "first",
    dateOfBirth = null,
    deceasedDate = null,
    createdBy = "user",
    createdTime = LocalDateTime.now(),
  )

  private fun mockBrotherRelationshipReferenceCode() {
    whenever(
      referenceCodeService.getReferenceDataByGroupAndCode(
        ReferenceCodeGroup.SOCIAL_RELATIONSHIP,
        "BRO",
      ),
    ).thenReturn(
      ReferenceCode(1, ReferenceCodeGroup.SOCIAL_RELATIONSHIP, "BRO", "Brother", 1, true),
    )
  }

  @Nested
  inner class DeleteContactRelationship {
    private val prisonerContactId = 897654L
    private val prisonerContactEntity = PrisonerContactEntity(
      prisonerContactId = prisonerContactId,
      contactId = 1L,
      prisonerNumber = "A1234BC",
      relationshipType = "S",
      relationshipToPrisoner = "BRO",
      nextOfKin = true,
      emergencyContact = true,
      approvedVisitor = true,
      active = true,
      currentTerm = true,
      comments = "Updated relationship type to Brother",
      createdBy = "TEST",
      createdTime = LocalDateTime.now(),
    ).apply {
      createdAtPrison = "FOO"
      approvedBy = "APP"
      approvedTime = LocalDateTime.of(2025, 1, 2, 10, 30)
      expiryDate = LocalDate.of(2025, 2, 3)
      updatedBy = "UPD"
      updatedTime = LocalDateTime.of(2025, 3, 4, 11, 45)
    }
    private val user = aUser("deleted")

    @Test
    fun `should delete the relationship and keep a history of it if there are no restrictions`() {
      whenever(prisonerContactRepository.findById(prisonerContactId)).thenReturn(Optional.of(prisonerContactEntity))
      whenever(prisonerContactRestrictionRepository.findAllByPrisonerContactId(prisonerContactId)).thenReturn(emptyList())
      whenever(deletedPrisonerContactRepository.saveAndFlush(any())).thenAnswer { i -> (i.arguments[0] as DeletedPrisonerContactEntity) }

      val result = service.deleteContactRelationship(prisonerContactId, user)

      assertThat(result).isEqualTo(DeletedResponse(ids = DeletedRelationshipIds(1, "A1234BC", prisonerContactId), wasUpdated = false))
      val deletedCaptor = argumentCaptor<DeletedPrisonerContactEntity>()
      verify(deletedPrisonerContactRepository).saveAndFlush(deletedCaptor.capture())
      assertThat(deletedCaptor.firstValue).usingRecursiveComparison().ignoringFields("deletedTime").isEqualTo(
        DeletedPrisonerContactEntity(
          deletedPrisonerContactId = 0,
          prisonerContactId = prisonerContactId,
          contactId = 1L,
          prisonerNumber = "A1234BC",
          relationshipType = "S",
          relationshipToPrisoner = "BRO",
          nextOfKin = true,
          emergencyContact = true,
          approvedVisitor = true,
          active = true,
          currentTerm = true,
          comments = "Updated relationship type to Brother",
          createdBy = "TEST",
          createdTime = prisonerContactEntity.createdTime,
          approvedBy = "APP",
          approvedTime = LocalDateTime.of(2025, 1, 2, 10, 30),
          expiryDate = LocalDate.of(2025, 2, 3),
          updatedBy = "UPD",
          updatedTime = LocalDateTime.of(2025, 3, 4, 11, 45),
          createdAtPrison = "FOO",
          deletedBy = "deleted",
          deletedTime = LocalDateTime.now(),
        ),
      )
    }

    @Test
    fun `should delete of the last non internal relationship remove date of birth`() {
      whenever(prisonerContactRepository.findById(prisonerContactId)).thenReturn(Optional.of(prisonerContactEntity))
      whenever(prisonerContactRestrictionRepository.findAllByPrisonerContactId(prisonerContactId)).thenReturn(emptyList())
      whenever(deletedPrisonerContactRepository.saveAndFlush(any())).thenAnswer { i -> (i.arguments[0] as DeletedPrisonerContactEntity) }
      whenever(prisonerContactRepository.findAllByContactId(prisonerContactEntity.contactId)).thenReturn(listOf(prisonerContactEntity.copy(prisonerContactId = 2L, relationshipToPrisoner = "POM")))
      val aContact = createContactEntity().copy(dateOfBirth = LocalDate.of(1990, 1, 1))
      whenever(contactRepository.findById(prisonerContactEntity.contactId)).thenReturn(Optional.of(aContact))

      val result = service.deleteContactRelationship(prisonerContactId, user)

      assertThat(result).isEqualTo(DeletedResponse(ids = DeletedRelationshipIds(1, "A1234BC", prisonerContactId), wasUpdated = true))
      val deletedDobCaptor = argumentCaptor<ContactEntity>()
      verify(contactRepository).saveAndFlush(deletedDobCaptor.capture())
      assertThat(deletedDobCaptor.firstValue).usingRecursiveComparison().ignoringFields("createdTime", "updatedTime").isEqualTo(
        aContact.copy(dateOfBirth = null, updatedBy = user.username),
      )

      val deletedCaptor = argumentCaptor<DeletedPrisonerContactEntity>()
      verify(deletedPrisonerContactRepository).saveAndFlush(deletedCaptor.capture())
      assertThat(deletedCaptor.firstValue).usingRecursiveComparison().ignoringFields("deletedTime").isEqualTo(
        DeletedPrisonerContactEntity(
          deletedPrisonerContactId = 0,
          prisonerContactId = prisonerContactId,
          contactId = 1L,
          prisonerNumber = "A1234BC",
          relationshipType = "S",
          relationshipToPrisoner = "BRO",
          nextOfKin = true,
          emergencyContact = true,
          approvedVisitor = true,
          active = true,
          currentTerm = true,
          comments = "Updated relationship type to Brother",
          createdBy = "TEST",
          createdTime = prisonerContactEntity.createdTime,
          approvedBy = "APP",
          approvedTime = LocalDateTime.of(2025, 1, 2, 10, 30),
          expiryDate = LocalDate.of(2025, 2, 3),
          updatedBy = "UPD",
          updatedTime = LocalDateTime.of(2025, 3, 4, 11, 45),
          createdAtPrison = "FOO",
          deletedBy = "deleted",
          deletedTime = LocalDateTime.now(),
        ),
      )
    }

    @Test
    fun `should throw exception deleting the relationship if it does not exist`() {
      whenever(prisonerContactRepository.findById(prisonerContactId)).thenReturn(Optional.empty())
      whenever(prisonerContactRestrictionRepository.findAllByPrisonerContactId(prisonerContactId)).thenReturn(emptyList())

      val exception = assertThrows<EntityNotFoundException> {
        service.deleteContactRelationship(prisonerContactId, user)
      }

      assertThat(exception.message).isEqualTo("Prisoner contact with prisoner contact ID $prisonerContactId not found")
    }

    @Test
    fun `should throw exception deleting the relationship if there are restrictions`() {
      whenever(prisonerContactRepository.findById(prisonerContactId)).thenReturn(Optional.of(prisonerContactEntity))
      whenever(prisonerContactRestrictionRepository.findAllByPrisonerContactId(prisonerContactId)).thenReturn(
        listOf(
          PrisonerContactRestrictionEntity(
            1,
            prisonerContactId,
            "BAN",
            LocalDate.now(),
            null,
            null,
            "FOO",
            LocalDateTime.now(),
          ),
        ),
      )

      val exception = assertThrows<RelationshipCannotBeRemovedDueToDependencyException> {
        service.deleteContactRelationship(prisonerContactId, user)
      }

      assertThat(exception.message).isEqualTo("Cannot delete relationship ($prisonerContactId) as there are dependent entities")
    }
  }
}
