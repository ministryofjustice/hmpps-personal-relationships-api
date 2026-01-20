package uk.gov.justice.digital.hmpps.personalrelationships.service.migrate

import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.reset
import org.mockito.MockitoAnnotations.openMocks
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.personalrelationships.entity.ContactAddressEntity
import uk.gov.justice.digital.hmpps.personalrelationships.entity.ContactEmailEntity
import uk.gov.justice.digital.hmpps.personalrelationships.entity.ContactIdentityEntity
import uk.gov.justice.digital.hmpps.personalrelationships.entity.ContactPhoneEntity
import uk.gov.justice.digital.hmpps.personalrelationships.entity.ContactRestrictionEntity
import uk.gov.justice.digital.hmpps.personalrelationships.entity.ContactWithFixedIdEntity
import uk.gov.justice.digital.hmpps.personalrelationships.entity.EmploymentEntity
import uk.gov.justice.digital.hmpps.personalrelationships.entity.PrisonerContactEntity
import uk.gov.justice.digital.hmpps.personalrelationships.entity.PrisonerContactRestrictionEntity
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.migrate.CodedValue
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.migrate.Corporate
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.migrate.MigrateAddress
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.migrate.MigrateContactRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.migrate.MigrateEmailAddress
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.migrate.MigrateEmployment
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.migrate.MigrateIdentifier
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.migrate.MigratePhoneNumber
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.migrate.MigratePrisonerContactRestriction
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.migrate.MigrateRelationship
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.migrate.MigrateRestriction
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.migrate.ElementType
import uk.gov.justice.digital.hmpps.personalrelationships.repository.ContactAddressPhoneRepository
import uk.gov.justice.digital.hmpps.personalrelationships.repository.ContactAddressRepository
import uk.gov.justice.digital.hmpps.personalrelationships.repository.ContactEmailRepository
import uk.gov.justice.digital.hmpps.personalrelationships.repository.ContactIdentityRepository
import uk.gov.justice.digital.hmpps.personalrelationships.repository.ContactPhoneRepository
import uk.gov.justice.digital.hmpps.personalrelationships.repository.ContactRestrictionRepository
import uk.gov.justice.digital.hmpps.personalrelationships.repository.ContactWithFixedIdRepository
import uk.gov.justice.digital.hmpps.personalrelationships.repository.EmploymentRepository
import uk.gov.justice.digital.hmpps.personalrelationships.repository.PrisonerContactRepository
import uk.gov.justice.digital.hmpps.personalrelationships.repository.PrisonerContactRestrictionRepository
import java.time.LocalDate
import java.time.LocalDateTime

class ContactMigrationServiceTest {
  private val contactRepository: ContactWithFixedIdRepository = mock()
  private val contactAddressRepository: ContactAddressRepository = mock()
  private val contactAddressPhoneRepository: ContactAddressPhoneRepository = mock()
  private val contactPhoneRepository: ContactPhoneRepository = mock()
  private val contactEmailRepository: ContactEmailRepository = mock()
  private val contactIdentityRepository: ContactIdentityRepository = mock()
  private val contactRestrictionRepository: ContactRestrictionRepository = mock()
  private val prisonerContactRepository: PrisonerContactRepository = mock()
  private val prisonerContactRestrictionRepository: PrisonerContactRestrictionRepository = mock()
  private val employmentRepository: EmploymentRepository = mock()

  val migrationService = ContactMigrationService(
    contactRepository,
    contactAddressRepository,
    contactAddressPhoneRepository,
    contactPhoneRepository,
    contactEmailRepository,
    contactIdentityRepository,
    contactRestrictionRepository,
    prisonerContactRepository,
    prisonerContactRestrictionRepository,
    employmentRepository,
  )

  private val aUsername = "J999J"
  private val aDateTime = LocalDateTime.of(2024, 1, 1, 13, 47)

  @BeforeEach
  fun setUp() {
    openMocks(this)
  }

  @Nested
  inner class MigrationOutcomes {
    @Test
    fun `should migrate a basic contact`() {
      val request = migrateRequest(personId = 1L)
      val contact = aContactEntity()

      whenever(contactRepository.existsById(1L)).thenReturn(false)
      whenever(contactRepository.save(any())).thenReturn(contact)

      val contactCaptor = argumentCaptor<ContactWithFixedIdEntity>()

      val result = migrationService.migrateContact(request)

      // IDs should be the same
      assertThat(result.contact.nomisId).isEqualTo(request.personId)
      assertThat(result.contact.dpsId).isEqualTo(request.personId)

      verify(contactRepository).save(contactCaptor.capture())

      with(contactCaptor.firstValue) {
        assertThat(this)
          .extracting("contactId", "lastName", "firstName", "createdBy", "createdTime", "updatedBy", "updatedTime")
          .contains(
            request.personId,
            request.lastName,
            request.firstName,
            request.createUsername,
            request.createDateTime,
            request.modifyUsername,
            request.modifyDateTime,
          )
      }
    }

    @Test
    fun `should identify duplicate requests for the same contact and replace them`() {
      val contactCaptor = argumentCaptor<ContactWithFixedIdEntity>()
      val contact = aContactEntity()

      whenever(contactRepository.existsById(1L)).thenReturn(false)
      whenever(contactRepository.save(any())).thenReturn(contact)

      val request = migrateRequest(personId = 1L)

      val result1 = migrationService.migrateContact(request)

      // IDs should be the same as the request
      assertThat(result1.contact.nomisId).isEqualTo(request.personId)
      assertThat(result1.contact.dpsId).isEqualTo(request.personId)

      // Reset mocks to ignore the first usage
      reset(contactRepository)
      whenever(contactRepository.save(any())).thenReturn(contact)
      whenever(contactRepository.existsById(1L)).thenReturn(true)

      // Duplicate the request
      migrationService.migrateContact(request)

      // IDs should be the same as the request
      assertThat(result1.contact.nomisId).isEqualTo(request.personId)
      assertThat(result1.contact.dpsId).isEqualTo(request.personId)

      // Verify that the contact and all sub-entities are deleted for duplicated requests
      verify(contactAddressPhoneRepository).deleteAllByContactId(request.personId)
      verify(contactAddressRepository).deleteAllByContactId(request.personId)
      verify(contactPhoneRepository).deleteAllByContactId(request.personId)
      verify(contactEmailRepository).deleteAllByContactId(request.personId)
      verify(contactIdentityRepository).deleteAllByContactId(request.personId)
      verify(contactRestrictionRepository).deleteAllByContactId(request.personId)
      verify(employmentRepository).deleteAllByContactId(request.personId)
      verify(prisonerContactRepository).deleteAllByContactId(request.personId)
      verify(prisonerContactRestrictionRepository).deleteAllByContactId(request.personId)
      verify(contactRepository).deleteAllByContactId(request.personId)

      // Will re-save the contact with the same person/contact ID
      verify(contactRepository).save(contactCaptor.capture())

      with(contactCaptor.firstValue) {
        assertThat(this.contactId).isEqualTo(request.personId)
      }
    }
  }

  @Nested
  inner class MigrationExtractFromRequest {

    @Test
    fun `should extract and save basic contact details`() {
      val request = migrateRequest(personId = 1L)
      val contact = aContactEntity()

      whenever(contactRepository.save(any())).thenReturn(contact)

      val contactCaptor = argumentCaptor<ContactWithFixedIdEntity>()

      val result = migrationService.extractAndSaveContact(request)

      assertThat(result.first).isEqualTo(request.personId)
      assertThat(result.second)
        .extracting("contactId", "lastName", "firstName")
        .contains(contact.contactId, contact.lastName, contact.firstName)

      verify(contactRepository).save(contactCaptor.capture())

      with(contactCaptor.firstValue) {
        assertThat(this)
          .extracting("contactId", "lastName", "firstName", "createdBy", "createdTime", "updatedBy", "updatedTime")
          .contains(
            1L,
            request.lastName,
            request.firstName,
            request.createUsername,
            request.createDateTime,
            request.modifyUsername,
            request.modifyDateTime,
          )
      }
    }

    @Test
    fun `should extract and save contact phone numbers`() {
      val request = migrateRequest(personId = 1L).copy(phoneNumbers = phoneNumbers())
      val responses = listOf(
        ContactPhoneEntity(
          contactId = 1L,
          contactPhoneId = 1L,
          phoneType = request.phoneNumbers[0].type.code,
          phoneNumber = request.phoneNumbers[0].number,
          createdBy = aUsername,
          createdTime = aDateTime,
        ),
        ContactPhoneEntity(
          contactId = 1L,
          contactPhoneId = 2L,
          phoneType = request.phoneNumbers[1].type.code,
          phoneNumber = request.phoneNumbers[1].number,
          createdBy = aUsername,
          createdTime = aDateTime,
        ),
      )

      whenever(contactPhoneRepository.save(any()))
        .thenReturn(responses[0])
        .thenReturn(responses[1])

      val contactPhoneCaptor = argumentCaptor<ContactPhoneEntity>()

      val result = migrationService.extractAndSavePhones(request, 1L)

      assertThat(result.size).isEqualTo(2)

      for (i in 0..1) {
        assertThat(result[i].first).isEqualTo(request.phoneNumbers[i].phoneId)
        assertThat(result[i].second)
          .extracting("contactId", "contactPhoneId", "phoneType", "phoneNumber")
          .contains(
            responses[i].contactId,
            responses[i].contactPhoneId,
            responses[i].phoneType,
            responses[i].phoneNumber,
          )
      }

      verify(contactPhoneRepository, times(2)).save(contactPhoneCaptor.capture())

      for (x in 0..1) {
        with(contactPhoneCaptor.allValues[x]) {
          assertThat(this)
            .extracting("contactId", "contactPhoneId", "createdBy", "createdTime")
            .contains(responses[x].contactId, 0L, aUsername, aDateTime)
        }
      }
    }

    @Test
    fun `should extract and save contact identifiers`() {
      val request = migrateRequest(personId = 1L).copy(identifiers = identifiers())

      val responses = listOf(
        ContactIdentityEntity(
          contactId = 1L,
          contactIdentityId = 1L,
          identityType = request.identifiers[0].type.code,
          identityValue = request.identifiers[0].identifier,
          issuingAuthority = request.identifiers[0].issuedAuthority,
          createdBy = aUsername,
          createdTime = aDateTime,
        ),
        ContactIdentityEntity(
          contactId = 1L,
          contactIdentityId = 2L,
          identityType = request.identifiers[1].type.code,
          identityValue = request.identifiers[1].identifier,
          issuingAuthority = request.identifiers[1].issuedAuthority,
          createdBy = aUsername,
          createdTime = aDateTime,
        ),
      )

      whenever(contactIdentityRepository.save(any()))
        .thenReturn(responses[0])
        .thenReturn(responses[1])

      val contactIdentityCaptor = argumentCaptor<ContactIdentityEntity>()

      val result = migrationService.extractAndSaveIdentities(request, 1L)

      assertThat(result.size).isEqualTo(2)

      for (i in 0..1) {
        assertThat(result[i].first).isEqualTo(request.identifiers[i].sequence)
        assertThat(result[i].second)
          .extracting("contactId", "contactIdentityId", "identityType", "identityValue", "issuingAuthority")
          .contains(
            responses[i].contactId,
            responses[i].contactIdentityId,
            responses[i].identityType,
            responses[i].identityValue,
            responses[i].issuingAuthority,
          )
      }

      verify(contactIdentityRepository, times(2)).save(contactIdentityCaptor.capture())
      for (x in 0..1) {
        with(contactIdentityCaptor.allValues[x]) {
          assertThat(this)
            .extracting("contactId", "contactIdentityId", "createdBy", "createdTime")
            .contains(responses[x].contactId, 0L, aUsername, aDateTime)
        }
      }
    }

    @Test
    fun `should extract and save contact addresses`() {
      val request = migrateRequest(personId = 1L).copy(addresses = addresses())

      val responses = listOf(
        ContactAddressEntity(
          contactAddressId = 1L,
          contactId = 1L,
          addressType = request.addresses[0].type?.code,
          property = request.addresses[0].premise,
          street = request.addresses[0].street,
          postCode = request.addresses[0].postCode,
          createdBy = aUsername,
          createdTime = aDateTime,
        ),
        ContactAddressEntity(
          contactAddressId = 2L,
          contactId = 1L,
          addressType = request.addresses[1].type?.code,
          property = request.addresses[1].premise,
          street = request.addresses[1].street,
          postCode = request.addresses[1].postCode,
          createdBy = aUsername,
          createdTime = aDateTime,
        ),
      )

      whenever(contactAddressRepository.save(any()))
        .thenReturn(responses[0])
        .thenReturn(responses[1])

      val contactAddressCaptor = argumentCaptor<ContactAddressEntity>()

      val result = migrationService.extractAndSaveAddresses(request, 1L)

      assertThat(result.size).isEqualTo(2)
      for (i in 0..1) {
        assertThat(result[i].first).isEqualTo(request.addresses[i].addressId)
        assertThat(result[i].second)
          .extracting("contactId", "contactAddressId", "addressType", "postCode")
          .contains(
            responses[i].contactId,
            responses[i].contactAddressId,
            responses[i].addressType,
            responses[i].postCode,
          )
      }

      verify(contactAddressRepository, times(2)).save(contactAddressCaptor.capture())
      for (x in 0..1) {
        with(contactAddressCaptor.allValues[x]) {
          assertThat(this)
            .extracting("contactId", "contactAddressId", "createdBy", "createdTime")
            .contains(responses[x].contactId, 0L, aUsername, aDateTime)
        }
      }
    }

    @Test
    fun `should extract and save contact emails`() {
      val request = migrateRequest(personId = 1L).copy(emailAddresses = emails())

      val responses = listOf(
        ContactEmailEntity(
          contactEmailId = 1L,
          contactId = 1L,
          emailAddress = request.emailAddresses[0].email,
          createdBy = aUsername,
          createdTime = aDateTime,
        ),
        ContactEmailEntity(
          contactEmailId = 2L,
          contactId = 1L,
          emailAddress = request.emailAddresses[1].email,
          createdBy = aUsername,
          createdTime = aDateTime,
        ),
      )

      whenever(contactEmailRepository.save(any()))
        .thenReturn(responses[0])
        .thenReturn(responses[1])

      val contactEmailCaptor = argumentCaptor<ContactEmailEntity>()

      val result = migrationService.extractAndSaveEmails(request, 1L)

      assertThat(result.size).isEqualTo(2)

      for (i in 0..1) {
        assertThat(result[i].first).isEqualTo(request.emailAddresses[i].emailAddressId)
        assertThat(result[i].second)
          .extracting("contactId", "contactEmailId", "emailAddress")
          .contains(
            responses[i].contactId,
            responses[i].contactEmailId,
            responses[i].emailAddress,
          )
      }

      verify(contactEmailRepository, times(2)).save(contactEmailCaptor.capture())

      for (x in 0..1) {
        with(contactEmailCaptor.allValues[x]) {
          assertThat(this)
            .extracting("contactId", "contactEmailId", "createdBy", "createdTime")
            .contains(responses[x].contactId, 0L, aUsername, aDateTime)
        }
      }
    }

    @Test
    fun `should extract and save contact (visitor) restrictions`() {
      val request = migrateRequest(personId = 1L).copy(restrictions = restrictions())

      val responses = listOf(
        ContactRestrictionEntity(
          contactRestrictionId = 1L,
          contactId = 1L,
          restrictionType = request.restrictions[0].type.code,
          startDate = request.restrictions[0].effectiveDate,
          expiryDate = request.restrictions[0].expiryDate,
          comments = request.restrictions[0].comment,
          createdBy = aUsername,
          createdTime = aDateTime,
        ),
        ContactRestrictionEntity(
          contactRestrictionId = 2L,
          contactId = 1L,
          restrictionType = request.restrictions[1].type.code,
          startDate = request.restrictions[1].effectiveDate,
          expiryDate = request.restrictions[1].expiryDate,
          comments = request.restrictions[1].comment,
          createdBy = aUsername,
          createdTime = aDateTime,
        ),
      )

      whenever(contactRestrictionRepository.save(any()))
        .thenReturn(responses[0])
        .thenReturn(responses[1])

      val contactRestrictionCaptor = argumentCaptor<ContactRestrictionEntity>()

      val result = migrationService.extractAndSaveRestrictions(request, 1L)

      assertThat(result.size).isEqualTo(2)

      for (i in 0..1) {
        assertThat(result[i].first).isEqualTo(request.restrictions[i].id)
        assertThat(result[i].second)
          .extracting("contactId", "contactRestrictionId", "restrictionType", "startDate", "expiryDate", "comments")
          .contains(
            responses[i].contactId,
            responses[i].contactRestrictionId,
            responses[i].restrictionType,
            responses[i].startDate,
            responses[i].expiryDate,
            responses[i].comments,
          )
      }

      verify(contactRestrictionRepository, times(2)).save(contactRestrictionCaptor.capture())

      for (x in 0..1) {
        with(contactRestrictionCaptor.allValues[x]) {
          assertThat(this)
            .extracting("contactId", "contactRestrictionId", "createdBy", "createdTime")
            .contains(responses[x].contactId, 0L, aUsername, aDateTime)
        }
      }
    }

    @Test
    fun `should extract and save contact employments`() {
      val request = migrateRequest(personId = 1L).copy(employments = employments())

      val responses = listOf(
        EmploymentEntity(
          employmentId = 1L,
          contactId = 1L,
          organisationId = request.employments[0].corporate.id,
          active = request.employments[0].active,
          createdBy = aUsername,
          createdTime = aDateTime,
          updatedBy = null,
          updatedTime = null,
        ),
        EmploymentEntity(
          employmentId = 2L,
          contactId = 1L,
          organisationId = request.employments[1].corporate.id,
          active = request.employments[1].active,
          createdBy = aUsername,
          createdTime = aDateTime,
          updatedBy = null,
          updatedTime = null,
        ),
      )

      whenever(employmentRepository.save(any()))
        .thenReturn(responses[0])
        .thenReturn(responses[1])

      val contactEmploymentCaptor = argumentCaptor<EmploymentEntity>()

      val result = migrationService.extractAndSaveEmployments(request, 1L)

      assertThat(result.size).isEqualTo(2)

      for (i in 0..1) {
        assertThat(result[i].first).isEqualTo(request.employments[i].sequence)
        assertThat(result[i].second)
          .extracting("contactId", "employmentId", "organisationId", "active")
          .contains(
            responses[i].contactId,
            responses[i].employmentId,
            responses[i].organisationId,
            responses[i].active,
          )
      }

      verify(employmentRepository, times(2)).save(contactEmploymentCaptor.capture())

      for (x in 0..1) {
        with(contactEmploymentCaptor.allValues[x]) {
          assertThat(this)
            .extracting("contactId", "employmentId", "createdBy", "createdTime")
            .contains(responses[x].contactId, 0L, aUsername, aDateTime)
        }
      }
    }

    @Test
    fun `should extract and save prisoner contact relationships`() {
      val request = migrateRequest(personId = 2L).copy(contacts = relationships())

      val responses = listOf(
        PrisonerContactEntity(
          prisonerContactId = 1L,
          contactId = 2L,
          relationshipType = request.contacts[0].contactType.code,
          relationshipToPrisoner = request.contacts[0].relationshipType.code,
          prisonerNumber = request.contacts[0].prisonerNumber,
          emergencyContact = request.contacts[0].emergencyContact,
          nextOfKin = request.contacts[0].nextOfKin,
          approvedVisitor = request.contacts[0].approvedVisitor,
          currentTerm = request.contacts[0].currentTerm,
          active = request.contacts[0].active,
          comments = request.contacts[0].comment,
          createdBy = aUsername,
          createdTime = aDateTime,
        ),
        PrisonerContactEntity(
          prisonerContactId = 2L,
          contactId = 2L,
          relationshipType = request.contacts[1].contactType.code,
          relationshipToPrisoner = request.contacts[1].relationshipType.code,
          prisonerNumber = request.contacts[1].prisonerNumber,
          emergencyContact = request.contacts[1].emergencyContact,
          nextOfKin = request.contacts[1].nextOfKin,
          approvedVisitor = request.contacts[1].approvedVisitor,
          currentTerm = request.contacts[1].currentTerm,
          active = request.contacts[1].active,
          comments = request.contacts[1].comment,
          createdBy = aUsername,
          createdTime = aDateTime,
        ),
      )

      whenever(prisonerContactRepository.save(any()))
        .thenReturn(responses[0])
        .thenReturn(responses[1])

      val prisonerContactCaptor = argumentCaptor<PrisonerContactEntity>()

      val result = migrationService.extractAndSavePrisonerContacts(request, 2L)

      assertThat(result.size).isEqualTo(2)

      for (i in 0..1) {
        assertThat(result[i].first).isEqualTo(request.contacts[i].id)
        assertThat(result[i].second)
          .extracting(
            "contactId",
            "prisonerContactId",
            "relationshipType",
            "relationshipToPrisoner",
            "prisonerNumber",
            "nextOfKin",
            "emergencyContact",
            "comments",
            "approvedVisitor",
          )
          .contains(
            responses[i].contactId,
            responses[i].prisonerContactId,
            responses[i].relationshipType,
            responses[i].relationshipToPrisoner,
            responses[i].prisonerNumber,
            responses[i].nextOfKin,
            responses[i].emergencyContact,
            responses[i].comments,
            responses[i].approvedVisitor,
          )
      }

      verify(prisonerContactRepository, times(2)).save(prisonerContactCaptor.capture())

      assertThat(prisonerContactCaptor.allValues[0])
        .extracting("contactId", "prisonerContactId", "createdBy", "createdTime", "approvedBy", "approvedTime")
        .contains(responses[0].contactId, 0L, aUsername, aDateTime, aUsername, aDateTime)
      assertThat(prisonerContactCaptor.allValues[1])
        .extracting("contactId", "prisonerContactId", "createdBy", "createdTime", "approvedBy", "approvedTime")
        .contains(responses[1].contactId, 0L, aUsername, aDateTime, null, null)
    }

    @Test
    fun `should extract and save contact address phone numbers`() {
      // TODO: Similar to the test below - with embedded list
    }

    @Test
    fun `should extract and save prisoner contact restrictions`() {
      val request = migrateRequest(personId = 1L).copy(contacts = relationshipsWithRestrictions())

      val relationshipResponse = PrisonerContactEntity(
        prisonerContactId = 1L,
        contactId = 1L,
        relationshipType = request.contacts[0].contactType.code,
        relationshipToPrisoner = request.contacts[0].relationshipType.code,
        prisonerNumber = request.contacts[0].prisonerNumber,
        emergencyContact = request.contacts[0].emergencyContact,
        nextOfKin = request.contacts[0].nextOfKin,
        approvedVisitor = request.contacts[0].approvedVisitor,
        currentTerm = request.contacts[0].currentTerm,
        active = request.contacts[0].active,
        comments = request.contacts[0].comment,
        createdBy = aUsername,
        createdTime = aDateTime,
      )

      val restrictionResponses = listOf(
        PrisonerContactRestrictionEntity(
          prisonerContactRestrictionId = 1L,
          prisonerContactId = 1L,
          restrictionType = request.contacts[0].restrictions[0].restrictionType.code,
          startDate = request.contacts[0].restrictions[0].startDate,
          expiryDate = request.contacts[0].restrictions[0].expiryDate,
          comments = request.contacts[0].restrictions[0].comment,
          createdBy = request.contacts[0].restrictions[0].createUsername!!,
          createdTime = request.contacts[0].restrictions[0].createDateTime!!,
        ),
        PrisonerContactRestrictionEntity(
          prisonerContactRestrictionId = 2L,
          prisonerContactId = 1L,
          restrictionType = request.contacts[0].restrictions[1].restrictionType.code,
          startDate = request.contacts[0].restrictions[1].startDate,
          expiryDate = request.contacts[0].restrictions[1].expiryDate,
          comments = request.contacts[0].restrictions[1].comment,
          createdBy = request.contacts[0].restrictions[1].createUsername!!,
          createdTime = request.contacts[0].restrictions[1].createDateTime!!,
        ),
      )

      whenever(prisonerContactRepository.save(any())).thenReturn(relationshipResponse)

      whenever(prisonerContactRestrictionRepository.save(any()))
        .thenReturn(restrictionResponses[0])
        .thenReturn(restrictionResponses[1])

      val prisonerContactRestrictionCaptor = argumentCaptor<PrisonerContactRestrictionEntity>()

      val resultContacts = migrationService.extractAndSavePrisonerContacts(request, 1L)
      val resultRestrictions = migrationService.extractAndSavePrisonerContactRestrictions(request, 1L, resultContacts)

      assertThat(resultRestrictions[0].second.size).isEqualTo(2)
      assertThat(resultRestrictions[0].second[0].first).isEqualTo(20L)
      assertThat(resultRestrictions[0].second[0].second.restrictionType).isEqualTo("NONCON")
      assertThat(resultRestrictions[0].second[1].first).isEqualTo(21L)
      assertThat(resultRestrictions[0].second[1].second.restrictionType).isEqualTo("CLOSED")

      verify(prisonerContactRestrictionRepository, times(2)).save(prisonerContactRestrictionCaptor.capture())

      for (x in 0..1) {
        with(prisonerContactRestrictionCaptor.allValues[x]) {
          assertThat(this)
            .extracting("prisonerContactId", "prisonerContactRestrictionId", "restrictionType", "comments")
            .contains(
              relationshipResponse.prisonerContactId,
              0L,
              restrictionResponses[x].restrictionType,
              restrictionResponses[x].comments,
            )
        }
      }
    }

    @Test
    fun `should build the contact restrictions response object`() {
      val contacts = listOf(
        Pair(
          first = 2L,
          second = PrisonerContactEntity(
            prisonerContactId = 1L,
            contactId = 1L,
            relationshipType = "S",
            relationshipToPrisoner = "BRO",
            prisonerNumber = "A1234AA",
            createdBy = "TEST",
            createdTime = LocalDateTime.now(),
            comments = "",
            emergencyContact = false,
            nextOfKin = true,
            approvedVisitor = false,
            currentTerm = true,
            active = true,
          ),
        ),
      )

      val restrictions = listOf(
        Pair(
          first = 2L,
          second = listOf(
            Pair(
              first = 20L,
              second = PrisonerContactRestrictionEntity(
                prisonerContactRestrictionId = 1L,
                prisonerContactId = 1L,
                restrictionType = "NONCON",
                startDate = LocalDate.now(),
                expiryDate = LocalDate.now().plusDays(10),
                comments = "No contact",
                createdBy = "TEST",
                createdTime = LocalDateTime.now(),
              ),
            ),
            Pair(
              first = 21L,
              second = PrisonerContactRestrictionEntity(
                prisonerContactRestrictionId = 2L,
                prisonerContactId = 1L,
                restrictionType = "CLOSED",
                startDate = LocalDate.now(),
                expiryDate = LocalDate.now().plusDays(10),
                comments = "Closed visit",
                createdBy = "TEST",
                createdTime = LocalDateTime.now(),
              ),
            ),
          ),
        ),
      )

      val result = migrationService.buildContactsAndRestrictionsResponse(contacts, restrictions)

      assertThat(result[0].restrictions[0])
        .extracting("elementType", "nomisId", "dpsId")
        .contains(ElementType.PRISONER_CONTACT_RESTRICTION, 20L, 1L)

      assertThat(result[0].restrictions[1])
        .extracting("elementType", "nomisId", "dpsId")
        .contains(ElementType.PRISONER_CONTACT_RESTRICTION, 21L, 2L)
    }
  }

  private fun migrateRequest(personId: Long): MigrateContactRequest = MigrateContactRequest(
    personId = personId,
    title = CodedValue("MR", "Mr"),
    lastName = "Smith",
    firstName = "John",
    gender = CodedValue("M", "Male"),
  ).also {
    it.createDateTime = aDateTime
    it.createUsername = aUsername
    it.modifyDateTime = aDateTime
    it.modifyUsername = aUsername
  }

  private fun aContactEntity() = ContactWithFixedIdEntity(
    contactId = 1L,
    title = "Mr",
    firstName = "John",
    middleNames = null,
    lastName = "Smith",
    dateOfBirth = null,
    deceasedDate = null,
    createdBy = aUsername,
    createdTime = aDateTime,
    updatedTime = aDateTime,
    updatedBy = aUsername,
  )

  private fun phoneNumbers() = listOf(
    MigratePhoneNumber(phoneId = 1L, number = "11111", extension = "1", type = CodedValue("HOME", "Home"))
      .also {
        it.createDateTime = aDateTime
        it.createUsername = aUsername
      },
    MigratePhoneNumber(phoneId = 2L, number = "22222", extension = "2", type = CodedValue("WORK", "Work"))
      .also {
        it.createDateTime = aDateTime
        it.createUsername = aUsername
      },
  )

  private fun addresses() = listOf(
    MigrateAddress(
      addressId = 1L,
      type = CodedValue("HOME", "Home"),
      premise = "10",
      street = "Dublin Road",
      postCode = "D1 1DN",
      primaryAddress = true,
    )
      .also {
        it.createDateTime = aDateTime
        it.createUsername = aUsername
      },
    MigrateAddress(
      addressId = 2L,
      type = CodedValue("WORK", "Work"),
      premise = "11",
      street = "Dublin Road",
      postCode = "D1 2DN",
    )
      .also {
        it.createDateTime = aDateTime
        it.createUsername = aUsername
      },
  )

  private fun emails() = listOf(
    MigrateEmailAddress(emailAddressId = 1L, email = "a@.com").also {
      it.createDateTime = aDateTime
      it.createUsername = aUsername
    },
    MigrateEmailAddress(emailAddressId = 2L, email = "b@b.com").also {
      it.createDateTime = aDateTime
      it.createUsername = aUsername
    },
  )

  private fun restrictions() = listOf(
    MigrateRestriction(
      id = 1L,
      type = CodedValue("PREINF", "Prior information"),
      comment = "Active",
      effectiveDate = LocalDate.now(),
      expiryDate = LocalDate.now().plusDays(30),
    ).also {
      it.createDateTime = aDateTime
      it.createUsername = aUsername
    },
    MigrateRestriction(
      id = 2L,
      type = CodedValue("CHILD", "Child restrictions"),
      comment = "Expired",
      effectiveDate = LocalDate.now().minusDays(30),
      expiryDate = LocalDate.now().minusDays(1),
    ).also {
      it.createDateTime = aDateTime
      it.createUsername = aUsername
    },
  )

  private fun employments() = listOf(
    MigrateEmployment(sequence = 1L, corporate = Corporate(id = 123L), active = true)
      .also {
        it.createDateTime = aDateTime
        it.createUsername = aUsername
      },
    MigrateEmployment(sequence = 2L, corporate = Corporate(id = 321L), active = false)
      .also {
        it.createDateTime = aDateTime
        it.createUsername = aUsername
      },
  )

  private fun relationships() = listOf(
    MigrateRelationship(
      id = 1L,
      contactType = CodedValue("S", "Social"),
      relationshipType = CodedValue("BRO", "Brother"),
      currentTerm = true,
      active = true,
      expiryDate = null,
      approvedVisitor = true,
      nextOfKin = true,
      emergencyContact = true,
      comment = "Visits regularly",
      prisonerNumber = "A1234AA",
      restrictions = emptyList(),
    ).also {
      it.createDateTime = aDateTime
      it.createUsername = aUsername
    },
    MigrateRelationship(
      id = 2L,
      contactType = CodedValue("O", "Official"),
      relationshipType = CodedValue("ILP", "In Loco Parentis"),
      currentTerm = true,
      active = false,
      expiryDate = LocalDate.now().minusDays(10),
      approvedVisitor = false,
      nextOfKin = false,
      emergencyContact = false,
      comment = "Used to visit but no more",
      prisonerNumber = "A1234AA",
      restrictions = emptyList(),
    ).also {
      it.createDateTime = aDateTime
      it.createUsername = aUsername
    },
  )

  private fun relationshipsWithRestrictions() = listOf(
    MigrateRelationship(
      id = 11L,
      contactType = CodedValue("S", "Social"),
      relationshipType = CodedValue("BRO", "Brother"),
      currentTerm = true,
      active = true,
      expiryDate = null,
      approvedVisitor = true,
      nextOfKin = true,
      emergencyContact = true,
      comment = "Visits regularly",
      prisonerNumber = "A1234AA",
      restrictions = listOf(
        MigratePrisonerContactRestriction(
          id = 20L,
          restrictionType = CodedValue("NONCON", "No contact"),
          comment = "This person is not allowed to visit",
          startDate = LocalDate.now().minusDays(30),
          expiryDate = LocalDate.now().plusDays(10),
        ).also {
          it.createDateTime = aDateTime
          it.createUsername = aUsername
        },
        MigratePrisonerContactRestriction(
          id = 21L,
          restrictionType = CodedValue("CLOSED", "Closed visit"),
          comment = "This person must be accompanied during visits",
          startDate = LocalDate.now().minusDays(30),
          expiryDate = LocalDate.now().plusDays(10),
        ).also {
          it.createDateTime = aDateTime
          it.createUsername = aUsername
        },
      ),
    )
      .also {
        it.createDateTime = aDateTime
        it.createUsername = aUsername
      },
  )

  private fun identifiers() = listOf(
    MigrateIdentifier(sequence = 1L, type = CodedValue("DL", "Driving Licence"), identifier = "DL1", issuedAuthority = "DVLA")
      .also {
        it.createDateTime = aDateTime
        it.createUsername = aUsername
      },
    MigrateIdentifier(sequence = 2L, type = CodedValue("PASS", "Passport"), identifier = "PASS1", issuedAuthority = "UKBORDER")
      .also {
        it.createDateTime = aDateTime
        it.createUsername = aUsername
      },
  )
}
