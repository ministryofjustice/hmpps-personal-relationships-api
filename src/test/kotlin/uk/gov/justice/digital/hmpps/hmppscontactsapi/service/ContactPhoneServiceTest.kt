package uk.gov.justice.digital.hmpps.hmppscontactsapi.service

import jakarta.persistence.EntityNotFoundException
import jakarta.validation.ValidationException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.ContactEntity
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.ContactPhoneDetailsEntity
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.ContactPhoneEntity
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.ReferenceCodeGroup
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.phone.CreateMultipleContactPhoneNumbersRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.phone.CreatePhoneRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.phone.PhoneNumber
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.phone.UpdatePhoneRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.ContactPhoneDetails
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.ReferenceCode
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.ContactAddressPhoneRepository
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.ContactPhoneDetailsRepository
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.ContactPhoneRepository
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.ContactRepository
import java.time.LocalDateTime.now
import java.util.*

class ContactPhoneServiceTest {

  private val contactRepository: ContactRepository = mock()
  private val contactPhoneRepository: ContactPhoneRepository = mock()
  private val contactAddressPhoneRepository: ContactAddressPhoneRepository = mock()
  private val contactPhoneDetailsRepository: ContactPhoneDetailsRepository = mock()
  private val referenceCodeService: ReferenceCodeService = mock()
  private val service =
    ContactPhoneService(
      contactRepository,
      contactPhoneRepository,
      contactPhoneDetailsRepository,
      contactAddressPhoneRepository,
      referenceCodeService,
    )

  private val contactId = 99L
  private val aContact = ContactEntity(
    contactId = contactId,
    title = "Mr",
    lastName = "last",
    middleNames = "middle",
    firstName = "first",
    dateOfBirth = null,
    deceasedDate = null,
    createdBy = "user",
    createdTime = now(),
  )

  companion object {
    @JvmStatic
    fun invalidPhoneNumbers(): List<Arguments> = listOf(
      "!",
      "\"",
      "£",
      "$",
      "%",
      "^",
      "&",
      "*",
      "_",
      "-",
      "=",
      // + not allowed unless at start
      "0+",
      ":",
      ";",
      "[",
      "]",
      "{",
      "}",
      "@",
      "#",
      "~",
      "/",
      "\\",
      "'",
    ).map { Arguments.of(it) }
  }

  @Nested
  inner class CreateMultiplePhones {
    private val request = CreateMultipleContactPhoneNumbersRequest(
      listOf(
        PhoneNumber(
          "MOB",
          "+447777777777",
          "0123",
        ),
        PhoneNumber(
          phoneType = "HOME",
          phoneNumber = "01234 567890",
          extNumber = null,
        ),
      ),
      "USER1",
    )

    @Test
    fun `should throw EntityNotFoundException creating phone numbers if contact doesn't exist`() {
      whenever(contactRepository.findById(contactId)).thenReturn(Optional.empty())

      val exception = assertThrows<EntityNotFoundException> {
        service.createMultiple(contactId, request)
      }
      assertThat(exception.message).isEqualTo("Contact (99) not found")
    }

    @Test
    fun `should throw ValidationException creating phone numbers if phone type is invalid`() {
      val expectedException = ValidationException("Invalid")
      whenever(contactRepository.findById(contactId)).thenReturn(Optional.of(aContact))
      whenever(
        referenceCodeService.validateReferenceCode(
          ReferenceCodeGroup.PHONE_TYPE,
          "FOO",
          allowInactive = false,
        ),
      ).thenThrow(expectedException)

      val exception = assertThrows<ValidationException> {
        service.createMultiple(contactId, request.copy(phoneNumbers = listOf(PhoneNumber(phoneType = "FOO", phoneNumber = "123"))))
      }
      assertThat(exception).isEqualTo(expectedException)
    }

    @ParameterizedTest
    @MethodSource("uk.gov.justice.digital.hmpps.hmppscontactsapi.service.ContactPhoneServiceTest#invalidPhoneNumbers")
    fun `should throw ValidationException creating phone if phone contains invalid chars`(phoneNumber: String) {
      whenever(contactRepository.findById(contactId)).thenReturn(Optional.of(aContact))
      whenever(referenceCodeService.getReferenceDataByGroupAndCode(ReferenceCodeGroup.PHONE_TYPE, "MOB")).thenReturn(
        ReferenceCode(
          0,
          ReferenceCodeGroup.PHONE_TYPE,
          "MOB",
          "Mobile",
          90,
          true,
        ),
      )

      val exception = assertThrows<ValidationException> {
        service.createMultiple(contactId, request.copy(phoneNumbers = listOf(PhoneNumber(phoneType = "MOB", phoneNumber = phoneNumber))))
      }
      assertThat(exception.message).isEqualTo("Phone number invalid, it can only contain numbers, () and whitespace with an optional + at the start")
    }

    @Test
    fun `should return all created phone details including the reference data after creating successfully`() {
      whenever(contactRepository.findById(contactId)).thenReturn(Optional.of(aContact))
      whenever(
        referenceCodeService.validateReferenceCode(
          ReferenceCodeGroup.PHONE_TYPE,
          "MOB",
          allowInactive = false,
        ),
      ).thenReturn(
        ReferenceCode(
          0,
          ReferenceCodeGroup.PHONE_TYPE,
          "MOB",
          "Mobile",
          90,
          true,
        ),
      )
      whenever(
        referenceCodeService.validateReferenceCode(
          ReferenceCodeGroup.PHONE_TYPE,
          "HOME",
          allowInactive = false,
        ),
      ).thenReturn(
        ReferenceCode(
          0,
          ReferenceCodeGroup.PHONE_TYPE,
          "HOME",
          "Home",
          91,
          true,
        ),
      )

      whenever(contactPhoneRepository.saveAndFlush(any())).thenAnswer { i ->
        (i.arguments[0] as ContactPhoneEntity).copy(
          contactPhoneId = 9999,
        )
      }.thenAnswer { i ->
        (i.arguments[0] as ContactPhoneEntity).copy(
          contactPhoneId = 8888,
        )
      }

      val allCreated = service.createMultiple(contactId, request)
      assertThat(allCreated).hasSize(2)
      val mobile = allCreated[0]
      assertThat(mobile.createdTime).isNotNull()
      assertThat(mobile).isEqualTo(
        ContactPhoneDetails(
          contactPhoneId = 9999,
          contactId = contactId,
          phoneType = "MOB",
          phoneTypeDescription = "Mobile",
          phoneNumber = "+447777777777",
          extNumber = "0123",
          createdBy = "USER1",
          createdTime = mobile.createdTime,
          updatedBy = null,
          updatedTime = null,
        ),
      )
      val home = allCreated[1]
      assertThat(home.createdTime).isNotNull()
      assertThat(home).isEqualTo(
        ContactPhoneDetails(
          contactPhoneId = 8888,
          contactId = contactId,
          phoneType = "HOME",
          phoneTypeDescription = "Home",
          phoneNumber = "01234 567890",
          extNumber = null,
          createdBy = "USER1",
          createdTime = home.createdTime,
          updatedBy = null,
          updatedTime = null,
        ),
      )
      verify(referenceCodeService).validateReferenceCode(ReferenceCodeGroup.PHONE_TYPE, "MOB", allowInactive = false)
      verify(referenceCodeService).validateReferenceCode(ReferenceCodeGroup.PHONE_TYPE, "HOME", allowInactive = false)
    }
  }

  @Nested
  inner class CreatePhone {
    private val request = CreatePhoneRequest(
      "MOB",
      "+447777777777",
      "0123",
      "USER1",
    )

    @Test
    fun `should throw EntityNotFoundException creating phone if contact doesn't exist`() {
      whenever(contactRepository.findById(contactId)).thenReturn(Optional.empty())

      val exception = assertThrows<EntityNotFoundException> {
        service.create(contactId, request)
      }
      assertThat(exception.message).isEqualTo("Contact (99) not found")
    }

    @Test
    fun `should throw ValidationException creating phone if phone type is invalid`() {
      val expectedException = ValidationException("Invalid")
      whenever(contactRepository.findById(contactId)).thenReturn(Optional.of(aContact))
      whenever(
        referenceCodeService.validateReferenceCode(
          ReferenceCodeGroup.PHONE_TYPE,
          "FOO",
          allowInactive = false,
        ),
      ).thenThrow(expectedException)

      val exception = assertThrows<ValidationException> {
        service.create(contactId, request.copy(phoneType = "FOO"))
      }
      assertThat(exception).isEqualTo(expectedException)
    }

    @ParameterizedTest
    @MethodSource("uk.gov.justice.digital.hmpps.hmppscontactsapi.service.ContactPhoneServiceTest#invalidPhoneNumbers")
    fun `should throw ValidationException creating phone if phone contains invalid chars`(phoneNumber: String) {
      whenever(contactRepository.findById(contactId)).thenReturn(Optional.of(aContact))
      whenever(referenceCodeService.getReferenceDataByGroupAndCode(ReferenceCodeGroup.PHONE_TYPE, "MOB")).thenReturn(
        ReferenceCode(
          0,
          ReferenceCodeGroup.PHONE_TYPE,
          "MOB",
          "Mobile",
          90,
          true,
        ),
      )

      val exception = assertThrows<ValidationException> {
        service.create(contactId, request.copy(phoneNumber = phoneNumber))
      }
      assertThat(exception.message).isEqualTo("Phone number invalid, it can only contain numbers, () and whitespace with an optional + at the start")
    }

    @Test
    fun `should return a phone details including the reference data after creating a contact successfully`() {
      whenever(contactRepository.findById(contactId)).thenReturn(Optional.of(aContact))
      whenever(
        referenceCodeService.validateReferenceCode(
          ReferenceCodeGroup.PHONE_TYPE,
          "MOB",
          allowInactive = false,
        ),
      ).thenReturn(
        ReferenceCode(
          0,
          ReferenceCodeGroup.PHONE_TYPE,
          "MOB",
          "Mobile",
          90,
          true,
        ),
      )
      whenever(contactPhoneRepository.saveAndFlush(any())).thenAnswer { i ->
        (i.arguments[0] as ContactPhoneEntity).copy(
          contactPhoneId = 9999,
        )
      }

      val created = service.create(contactId, request)
      assertThat(created.createdTime).isNotNull()
      assertThat(created).isEqualTo(
        ContactPhoneDetails(
          contactPhoneId = 9999,
          contactId = contactId,
          phoneType = "MOB",
          phoneTypeDescription = "Mobile",
          phoneNumber = "+447777777777",
          extNumber = "0123",
          createdBy = "USER1",
          createdTime = created.createdTime,
          updatedBy = null,
          updatedTime = null,
        ),
      )
      verify(referenceCodeService).validateReferenceCode(ReferenceCodeGroup.PHONE_TYPE, "MOB", allowInactive = false)
    }
  }

  @Nested
  inner class GetPhone {
    private val createdTime = now()
    private val phoneEntity = ContactPhoneDetailsEntity(
      contactPhoneId = 99,
      contactId = contactId,
      phoneType = "MOB",
      phoneTypeDescription = "Mobile",
      phoneNumber = "07777777777",
      extNumber = null,
      createdBy = "USER1",
      createdTime = createdTime,
      updatedBy = null,
      updatedTime = null,
    )

    @Test
    fun `get phone if found by ids`() {
      whenever(contactPhoneDetailsRepository.findByContactIdAndContactPhoneId(contactId, 99)).thenReturn(phoneEntity)

      val returnedPhone = service.get(contactId, 99)

      assertThat(returnedPhone).isEqualTo(
        ContactPhoneDetails(
          contactPhoneId = 99,
          contactId = contactId,
          phoneType = "MOB",
          phoneTypeDescription = "Mobile",
          phoneNumber = "07777777777",
          extNumber = null,
          createdBy = "USER1",
          createdTime = createdTime,
          updatedBy = null,
          updatedTime = null,
        ),
      )
    }

    @Test
    fun `return null if phone not found`() {
      whenever(contactPhoneDetailsRepository.findByContactIdAndContactPhoneId(contactId, 99)).thenReturn(null)

      assertThat(service.get(contactId, 99)).isNull()
    }
  }

  @Nested
  inner class UpdatePhone {
    private val request = UpdatePhoneRequest(
      "MOB",
      "+447777777777",
      "0123",
      "updated",
    )
    private val contactPhoneId = 1234L
    private val existingPhone = ContactPhoneEntity(
      contactPhoneId = contactPhoneId,
      contactId = contactId,
      phoneType = "HOME",
      phoneNumber = "999",
      extNumber = null,
      createdBy = "USER99",
      createdTime = now().minusDays(2),
      updatedBy = null,
      updatedTime = null,
    )

    @Test
    fun `should throw EntityNotFoundException updating phone if contact doesn't exist`() {
      whenever(contactRepository.findById(contactId)).thenReturn(Optional.empty())

      val exception = assertThrows<EntityNotFoundException> {
        service.update(contactId, contactPhoneId, request)
      }
      assertThat(exception.message).isEqualTo("Contact (99) not found")
    }

    @Test
    fun `should throw EntityNotFoundException updating phone if phone doesn't exist`() {
      whenever(contactRepository.findById(contactId)).thenReturn(Optional.of(aContact))
      whenever(contactPhoneRepository.findById(contactPhoneId)).thenReturn(Optional.empty())

      val exception = assertThrows<EntityNotFoundException> {
        service.update(contactId, contactPhoneId, request)
      }
      assertThat(exception.message).isEqualTo("Contact phone (1234) not found")
    }

    @Test
    fun `should throw ValidationException updating phone if phone type is invalid`() {
      whenever(contactRepository.findById(contactId)).thenReturn(Optional.of(aContact))
      whenever(contactPhoneRepository.findById(contactPhoneId)).thenReturn(Optional.of(existingPhone))
      val expectedException = ValidationException("Invalid")
      whenever(
        referenceCodeService.validateReferenceCode(
          ReferenceCodeGroup.PHONE_TYPE,
          "FOO",
          allowInactive = true,
        ),
      ).thenThrow(expectedException)

      val exception = assertThrows<ValidationException> {
        service.update(contactId, contactPhoneId, request.copy(phoneType = "FOO"))
      }
      assertThat(exception).isEqualTo(expectedException)
    }

    @ParameterizedTest
    @MethodSource("uk.gov.justice.digital.hmpps.hmppscontactsapi.service.ContactPhoneServiceTest#invalidPhoneNumbers")
    fun `should throw ValidationException updating phone if phone contains invalid chars`(phoneNumber: String) {
      whenever(contactRepository.findById(contactId)).thenReturn(Optional.of(aContact))
      whenever(contactPhoneRepository.findById(contactPhoneId)).thenReturn(Optional.of(existingPhone))
      whenever(referenceCodeService.getReferenceDataByGroupAndCode(ReferenceCodeGroup.PHONE_TYPE, "MOB")).thenReturn(
        ReferenceCode(
          0,
          ReferenceCodeGroup.PHONE_TYPE,
          "MOB",
          "Mobile",
          90,
          true,
        ),
      )

      val exception = assertThrows<ValidationException> {
        service.update(contactId, contactPhoneId, request.copy(phoneNumber = phoneNumber))
      }
      assertThat(exception.message).isEqualTo("Phone number invalid, it can only contain numbers, () and whitespace with an optional + at the start")
    }

    @Test
    fun `should return a phone details including the reference data after updating a phone successfully`() {
      whenever(contactRepository.findById(contactId)).thenReturn(Optional.of(aContact))
      whenever(contactPhoneRepository.findById(contactPhoneId)).thenReturn(Optional.of(existingPhone))
      whenever(
        referenceCodeService.validateReferenceCode(
          ReferenceCodeGroup.PHONE_TYPE,
          "MOB",
          allowInactive = true,
        ),
      ).thenReturn(
        ReferenceCode(
          0,
          ReferenceCodeGroup.PHONE_TYPE,
          "MOB",
          "Mobile",
          90,
          true,
        ),
      )
      whenever(contactPhoneRepository.saveAndFlush(any())).thenAnswer { i ->
        (i.arguments[0] as ContactPhoneEntity).copy(
          contactPhoneId = 9999,
        )
      }

      val updated = service.update(contactId, contactPhoneId, request)
      assertThat(updated.updatedTime).isNotNull()
      assertThat(updated).isEqualTo(
        ContactPhoneDetails(
          contactPhoneId = 9999,
          contactId = contactId,
          phoneType = "MOB",
          phoneTypeDescription = "Mobile",
          phoneNumber = "+447777777777",
          extNumber = "0123",
          createdBy = "USER99",
          createdTime = existingPhone.createdTime,
          updatedBy = "updated",
          updatedTime = updated.updatedTime,
        ),
      )
      verify(referenceCodeService).validateReferenceCode(ReferenceCodeGroup.PHONE_TYPE, "MOB", allowInactive = true)
    }
  }

  @Nested
  inner class DeletePhone {
    private val contactPhoneId = 1234L
    private val existingPhone = ContactPhoneEntity(
      contactPhoneId = contactPhoneId,
      contactId = contactId,
      phoneType = "HOME",
      phoneNumber = "999",
      extNumber = null,
      createdBy = "USER99",
      createdTime = now().minusDays(2),
      updatedBy = null,
      updatedTime = null,
    )

    @Test
    fun `should throw EntityNotFoundException deleting phone if contact doesn't exist`() {
      whenever(contactRepository.findById(contactId)).thenReturn(Optional.empty())

      val exception = assertThrows<EntityNotFoundException> {
        service.delete(contactId, contactPhoneId)
      }
      assertThat(exception.message).isEqualTo("Contact (99) not found")
    }

    @Test
    fun `should throw EntityNotFoundException deleting phone if phone doesn't exist`() {
      whenever(contactRepository.findById(contactId)).thenReturn(Optional.of(aContact))
      whenever(contactPhoneRepository.findById(contactPhoneId)).thenReturn(Optional.empty())

      val exception = assertThrows<EntityNotFoundException> {
        service.delete(contactId, contactPhoneId)
      }
      assertThat(exception.message).isEqualTo("Contact phone (1234) not found")
    }

    @Test
    fun `should just delete the phone and any address links if it exists`() {
      whenever(contactRepository.findById(contactId)).thenReturn(Optional.of(aContact))
      whenever(contactPhoneRepository.findById(contactPhoneId)).thenReturn(Optional.of(existingPhone))
      whenever(contactPhoneRepository.delete(any())).then {}
      whenever(contactAddressPhoneRepository.deleteByContactPhoneId(contactPhoneId)).then {}

      service.delete(contactId, contactPhoneId)

      verify(contactAddressPhoneRepository).deleteByContactPhoneId(contactPhoneId)
      verify(contactPhoneRepository).delete(existingPhone)
    }
  }
}
