package uk.gov.justice.digital.hmpps.personalrelationships.service

import jakarta.persistence.EntityNotFoundException
import jakarta.validation.ValidationException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.personalrelationships.entity.ContactEntity
import uk.gov.justice.digital.hmpps.personalrelationships.entity.ContactIdentityDetailsEntity
import uk.gov.justice.digital.hmpps.personalrelationships.entity.ContactIdentityEntity
import uk.gov.justice.digital.hmpps.personalrelationships.helpers.aUser
import uk.gov.justice.digital.hmpps.personalrelationships.model.ReferenceCodeGroup
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.identity.CreateIdentityRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.identity.CreateMultipleIdentitiesRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.identity.IdentityDocument
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.identity.UpdateIdentityRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.ContactIdentityDetails
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.ReferenceCode
import uk.gov.justice.digital.hmpps.personalrelationships.repository.ContactIdentityDetailsRepository
import uk.gov.justice.digital.hmpps.personalrelationships.repository.ContactIdentityRepository
import uk.gov.justice.digital.hmpps.personalrelationships.repository.ContactRepository
import java.time.LocalDateTime.now
import java.util.*

class ContactIdentityServiceTest {

  private val contactRepository: ContactRepository = mock()
  private val contactIdentityRepository: ContactIdentityRepository = mock()
  private val contactIdentityDetailsRepository: ContactIdentityDetailsRepository = mock()
  private val referenceCodeService: ReferenceCodeService = mock()
  private val service =
    ContactIdentityService(contactRepository, contactIdentityRepository, contactIdentityDetailsRepository, referenceCodeService)

  private val contactId = 99L
  private val aContact = ContactEntity(
    contactId = contactId,
    title = "MR",
    lastName = "last",
    middleNames = "middle",
    firstName = "first",
    dateOfBirth = null,
    deceasedDate = null,
    createdBy = "user",
    createdTime = now(),
  )

  private val user = aUser("identity_user")

  @Nested
  inner class CreateIdentity {
    private val request = CreateIdentityRequest(
      identityType = "DL",
      identityValue = "DL123456789",
      issuingAuthority = "DVLA",
    )

    @Test
    fun `should throw EntityNotFoundException creating identity if contact doesn't exist`() {
      whenever(contactRepository.findById(contactId)).thenReturn(Optional.empty())

      val exception = assertThrows<EntityNotFoundException> {
        service.create(contactId, request, user)
      }
      assertThat(exception.message).isEqualTo("Contact (99) not found")
    }

    @Test
    fun `should throw ValidationException creating identity if identity type is invalid`() {
      whenever(contactRepository.findById(contactId)).thenReturn(Optional.of(aContact))
      val expectedException = ValidationException("Unsupported identity type (FOO)")
      whenever(referenceCodeService.validateReferenceCode(ReferenceCodeGroup.ID_TYPE, "FOO", allowInactive = false)).thenThrow(
        expectedException,
      )

      val exception = assertThrows<ValidationException> {
        service.create(contactId, request.copy(identityType = "FOO"), user)
      }
      assertThat(exception).isEqualTo(expectedException)
      verify(referenceCodeService).validateReferenceCode(ReferenceCodeGroup.ID_TYPE, "FOO", allowInactive = false)
    }

    @Test
    fun `should throw ValidationException if identity type is PNC but identity is not a valid PNC`() {
      whenever(contactRepository.findById(contactId)).thenReturn(Optional.of(aContact))
      whenever(referenceCodeService.validateReferenceCode(ReferenceCodeGroup.ID_TYPE, "PNC", allowInactive = false)).thenReturn(
        ReferenceCode(
          0,
          ReferenceCodeGroup.ID_TYPE,
          "PNC",
          "PNC Number",
          0,
          true,
        ),
      )

      val exception = assertThrows<ValidationException> {
        service.create(contactId, request.copy(identityValue = "1923/1Z34567A", identityType = "PNC"), user)
      }
      assertThat(exception.message).isEqualTo("Identity value (1923/1Z34567A) is not a valid PNC Number")
    }

    @Test
    fun `should return identity details including the reference data after creating successfully`() {
      whenever(contactRepository.findById(contactId)).thenReturn(Optional.of(aContact))
      whenever(referenceCodeService.validateReferenceCode(ReferenceCodeGroup.ID_TYPE, "DL", allowInactive = false)).thenReturn(
        ReferenceCode(
          0,
          ReferenceCodeGroup.ID_TYPE,
          "DL",
          "Driving licence",
          90,
          true,
        ),
      )
      whenever(contactIdentityRepository.saveAndFlush(any())).thenAnswer { i ->
        (i.arguments[0] as ContactIdentityEntity).copy(
          contactIdentityId = 9999,
        )
      }

      val created = service.create(contactId, request, user)
      assertThat(created.createdTime).isNotNull()
      assertThat(created).isEqualTo(
        ContactIdentityDetails(
          contactIdentityId = 9999,
          contactId = contactId,
          identityType = "DL",
          identityTypeDescription = "Driving licence",
          identityTypeIsActive = true,
          identityValue = "DL123456789",
          issuingAuthority = "DVLA",
          createdBy = "identity_user",
          createdTime = created.createdTime,
          updatedBy = null,
          updatedTime = null,
        ),
      )
    }
  }

  @Nested
  inner class CreateMultipleIdentities {
    private val request = CreateMultipleIdentitiesRequest(
      identities = listOf(
        IdentityDocument(
          identityType = "DL",
          identityValue = "DL123456789",
          issuingAuthority = "DVLA",
        ),
        IdentityDocument(
          identityType = "PASS",
          identityValue = "P897654312",
          issuingAuthority = null,
        ),
      ),
    )

    @Test
    fun `should throw EntityNotFoundException creating identities if contact doesn't exist`() {
      whenever(contactRepository.findById(contactId)).thenReturn(Optional.empty())

      val exception = assertThrows<EntityNotFoundException> {
        service.createMultiple(contactId, request, user)
      }
      assertThat(exception.message).isEqualTo("Contact (99) not found")
    }

    @Test
    fun `should throw ValidationException creating identities if identity type is invalid`() {
      whenever(contactRepository.findById(contactId)).thenReturn(Optional.of(aContact))
      val expectedException = ValidationException("Unsupported identity type (FOO)")
      whenever(referenceCodeService.validateReferenceCode(ReferenceCodeGroup.ID_TYPE, "FOO", allowInactive = false)).thenThrow(
        expectedException,
      )

      val exception = assertThrows<ValidationException> {
        service.createMultiple(contactId, request.copy(identities = listOf(IdentityDocument(identityType = "FOO", identityValue = "111111"))), user)
      }
      assertThat(exception).isEqualTo(expectedException)
      verify(referenceCodeService).validateReferenceCode(ReferenceCodeGroup.ID_TYPE, "FOO", allowInactive = false)
    }

    @Test
    fun `should throw ValidationException if identity type is PNC but identity is not a valid PNC`() {
      whenever(contactRepository.findById(contactId)).thenReturn(Optional.of(aContact))
      whenever(referenceCodeService.validateReferenceCode(ReferenceCodeGroup.ID_TYPE, "PNC", allowInactive = false)).thenReturn(
        ReferenceCode(
          0,
          ReferenceCodeGroup.ID_TYPE,
          "PNC",
          "PNC Number",
          0,
          true,
        ),
      )

      val exception = assertThrows<ValidationException> {
        service.createMultiple(contactId, request.copy(identities = listOf(IdentityDocument(identityValue = "1923/1Z34567A", identityType = "PNC"))), user)
      }
      assertThat(exception.message).isEqualTo("Identity value (1923/1Z34567A) is not a valid PNC Number")
    }

    @Test
    fun `should return identity details including the reference data after creating successfully`() {
      whenever(contactRepository.findById(contactId)).thenReturn(Optional.of(aContact))
      whenever(referenceCodeService.validateReferenceCode(ReferenceCodeGroup.ID_TYPE, "DL", allowInactive = false)).thenReturn(
        ReferenceCode(
          0,
          ReferenceCodeGroup.ID_TYPE,
          "DL",
          "Driving licence",
          90,
          true,
        ),
      )
      whenever(referenceCodeService.validateReferenceCode(ReferenceCodeGroup.ID_TYPE, "PASS", allowInactive = false)).thenReturn(
        ReferenceCode(
          0,
          ReferenceCodeGroup.ID_TYPE,
          "PASS",
          "Passport Number",
          90,
          true,
        ),
      )
      whenever(contactIdentityRepository.saveAndFlush(any())).thenAnswer { i ->
        (i.arguments[0] as ContactIdentityEntity).copy(
          contactIdentityId = 9999,
        )
      }.thenAnswer { i ->
        (i.arguments[0] as ContactIdentityEntity).copy(
          contactIdentityId = 8888,
        )
      }

      val allCreated = service.createMultiple(contactId, request, user)

      val drivingLicence = allCreated[0]
      assertThat(drivingLicence.createdTime).isNotNull()
      assertThat(drivingLicence).isEqualTo(
        ContactIdentityDetails(
          contactIdentityId = 9999,
          contactId = contactId,
          identityType = "DL",
          identityTypeDescription = "Driving licence",
          identityTypeIsActive = true,
          identityValue = "DL123456789",
          issuingAuthority = "DVLA",
          createdBy = "identity_user",
          createdTime = drivingLicence.createdTime,
          updatedBy = null,
          updatedTime = null,
        ),
      )

      val passport = allCreated[1]
      assertThat(passport.createdTime).isNotNull()
      assertThat(passport).isEqualTo(
        ContactIdentityDetails(
          contactIdentityId = 8888,
          contactId = contactId,
          identityType = "PASS",
          identityTypeDescription = "Passport Number",
          identityTypeIsActive = true,
          identityValue = "P897654312",
          issuingAuthority = null,
          createdBy = "identity_user",
          createdTime = passport.createdTime,
          updatedBy = null,
          updatedTime = null,
        ),
      )
    }
  }

  @Nested
  inner class UpdateIdentity {
    private val request = UpdateIdentityRequest(
      "PASS",
      "P987654321",
      "Passport office",
    )
    private val contactIdentityId = 1234L
    private val existingIdentity = ContactIdentityEntity(
      contactIdentityId = contactIdentityId,
      contactId = contactId,
      identityType = "DL",
      identityValue = "DL123456789",
      issuingAuthority = null,
      createdBy = "USER99",
      createdTime = now().minusDays(2),
      updatedBy = null,
      updatedTime = null,
    )

    @Test
    fun `should throw EntityNotFoundException updating identity if contact doesn't exist`() {
      whenever(contactRepository.findById(contactId)).thenReturn(Optional.empty())

      val exception = assertThrows<EntityNotFoundException> {
        service.update(contactId, contactIdentityId, request, user)
      }
      assertThat(exception.message).isEqualTo("Contact (99) not found")
    }

    @Test
    fun `should throw EntityNotFoundException updating identity if identity doesn't exist`() {
      whenever(contactRepository.findById(contactId)).thenReturn(Optional.of(aContact))
      whenever(contactIdentityRepository.findById(contactIdentityId)).thenReturn(Optional.empty())

      val exception = assertThrows<EntityNotFoundException> {
        service.update(contactId, contactIdentityId, request, user)
      }
      assertThat(exception.message).isEqualTo("Contact identity (1234) not found")
    }

    @Test
    fun `should throw ValidationException updating identity if identity type is invalid`() {
      whenever(contactRepository.findById(contactId)).thenReturn(Optional.of(aContact))
      whenever(contactIdentityRepository.findById(contactIdentityId)).thenReturn(Optional.of(existingIdentity))
      val expectedException = ValidationException("Unsupported identity type (FOO)")
      whenever(referenceCodeService.validateReferenceCode(ReferenceCodeGroup.ID_TYPE, "FOO", allowInactive = true)).thenThrow(expectedException)

      val exception = assertThrows<ValidationException> {
        service.update(contactId, contactIdentityId, request.copy(identityType = "FOO"), user)
      }
      assertThat(exception).isEqualTo(expectedException)
      verify(referenceCodeService).validateReferenceCode(ReferenceCodeGroup.ID_TYPE, "FOO", allowInactive = true)
    }

    @Test
    fun `should throw ValidationException if identity type is PNC but identity is not a valid PNC`() {
      whenever(contactRepository.findById(contactId)).thenReturn(Optional.of(aContact))
      whenever(contactIdentityRepository.findById(contactIdentityId)).thenReturn(Optional.of(existingIdentity))
      whenever(referenceCodeService.validateReferenceCode(ReferenceCodeGroup.ID_TYPE, "PNC", allowInactive = true)).thenReturn(
        ReferenceCode(
          0,
          ReferenceCodeGroup.ID_TYPE,
          "PNC",
          "PNC Number",
          0,
          true,
        ),
      )

      val exception = assertThrows<ValidationException> {
        service.update(contactId, contactIdentityId, request.copy(identityValue = "1923/1Z34567A", identityType = "PNC"), user)
      }
      assertThat(exception.message).isEqualTo("Identity value (1923/1Z34567A) is not a valid PNC Number")
    }

    @Test
    fun `should return a identity details including the reference data after updating a identity successfully`() {
      whenever(contactRepository.findById(contactId)).thenReturn(Optional.of(aContact))
      whenever(contactIdentityRepository.findById(contactIdentityId)).thenReturn(Optional.of(existingIdentity))
      whenever(referenceCodeService.validateReferenceCode(ReferenceCodeGroup.ID_TYPE, "PASS", allowInactive = true)).thenReturn(
        ReferenceCode(
          0,
          ReferenceCodeGroup.ID_TYPE,
          "PASS",
          "Passport",
          90,
          true,
        ),
      )
      whenever(contactIdentityRepository.saveAndFlush(any())).thenAnswer { i ->
        (i.arguments[0] as ContactIdentityEntity).copy(
          contactIdentityId = 9999,
        )
      }

      val updated = service.update(contactId, contactIdentityId, request, user)
      assertThat(updated.updatedTime).isNotNull()
      assertThat(updated).isEqualTo(
        ContactIdentityDetails(
          contactIdentityId = 9999,
          contactId = contactId,
          identityType = "PASS",
          identityTypeDescription = "Passport",
          identityTypeIsActive = true,
          identityValue = "P987654321",
          issuingAuthority = "Passport office",
          createdBy = "USER99",
          createdTime = existingIdentity.createdTime,
          updatedBy = "identity_user",
          updatedTime = updated.updatedTime,
        ),
      )
    }
  }

  @Nested
  inner class GetIdentity {
    private val createdTime = now()
    private val entity = ContactIdentityDetailsEntity(
      contactIdentityId = 99,
      contactId = contactId,
      identityType = "DL",
      identityTypeDescription = "Driving licence",
      identityTypeIsActive = true,
      identityValue = "DL123456789",
      issuingAuthority = "DVLA",
      createdBy = "USER1",
      createdTime = createdTime,
      updatedBy = null,
      updatedTime = null,
    )

    @Test
    fun `get identity if found by ids`() {
      whenever(contactIdentityDetailsRepository.findByContactIdAndContactIdentityId(contactId, 99)).thenReturn(entity)

      val returned = service.get(contactId, 99)

      assertThat(returned).isEqualTo(
        ContactIdentityDetails(
          contactIdentityId = 99,
          contactId = contactId,
          identityType = "DL",
          identityTypeDescription = "Driving licence",
          identityTypeIsActive = true,
          identityValue = "DL123456789",
          issuingAuthority = "DVLA",
          createdBy = "USER1",
          createdTime = createdTime,
          updatedBy = null,
          updatedTime = null,
        ),
      )
    }

    @Test
    fun `return null if not found`() {
      whenever(contactIdentityDetailsRepository.findByContactIdAndContactIdentityId(contactId, 99)).thenReturn(null)

      assertThat(service.get(contactId, 99)).isNull()
    }
  }

  @Nested
  inner class DeleteIdentity {
    private val contactIdentityId = 1234L
    private val existingIdentity = ContactIdentityEntity(
      contactIdentityId = contactIdentityId,
      contactId = contactId,
      identityType = "DL",
      identityValue = "DL123456789",
      createdBy = "USER99",
      createdTime = now().minusDays(2),
      updatedBy = null,
      updatedTime = null,
    )

    @Test
    fun `should throw EntityNotFoundException deleting identity if contact doesn't exist`() {
      whenever(contactRepository.findById(contactId)).thenReturn(Optional.empty())

      val exception = assertThrows<EntityNotFoundException> {
        service.delete(contactId, contactIdentityId)
      }
      assertThat(exception.message).isEqualTo("Contact (99) not found")
    }

    @Test
    fun `should throw EntityNotFoundException deleting identity if identity doesn't exist`() {
      whenever(contactRepository.findById(contactId)).thenReturn(Optional.of(aContact))
      whenever(contactIdentityRepository.findById(contactIdentityId)).thenReturn(Optional.empty())

      val exception = assertThrows<EntityNotFoundException> {
        service.delete(contactId, contactIdentityId)
      }
      assertThat(exception.message).isEqualTo("Contact identity (1234) not found")
    }

    @Test
    fun `should just delete the identity and any address links if it exists`() {
      whenever(contactRepository.findById(contactId)).thenReturn(Optional.of(aContact))
      whenever(contactIdentityRepository.findById(contactIdentityId)).thenReturn(Optional.of(existingIdentity))
      whenever(contactIdentityRepository.delete(any())).then {}

      service.delete(contactId, contactIdentityId)

      verify(contactIdentityRepository).delete(existingIdentity)
    }
  }
}
