package uk.gov.justice.digital.hmpps.hmppscontactsapi.service

import jakarta.persistence.EntityNotFoundException
import jakarta.validation.ValidationException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.ContactAddressEntity
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.ContactAddressPhoneEntity
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.ContactEntity
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.ContactPhoneEntity
import uk.gov.justice.digital.hmpps.hmppscontactsapi.helpers.aUser
import uk.gov.justice.digital.hmpps.hmppscontactsapi.helpers.createContactAddressPhoneRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.ReferenceCodeGroup
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.phone.CreateMultiplePhoneNumbersRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.phone.PhoneNumber
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.phone.UpdateContactAddressPhoneRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.ReferenceCode
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.ContactAddressPhoneRepository
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.ContactAddressRepository
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.ContactPhoneRepository
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.ContactRepository
import java.time.LocalDateTime.now
import java.util.Optional

class ContactAddressPhoneServiceTest {
  private val contactRepository: ContactRepository = mock()
  private val contactPhoneRepository: ContactPhoneRepository = mock()
  private val contactAddressRepository: ContactAddressRepository = mock()
  private val contactAddressPhoneRepository: ContactAddressPhoneRepository = mock()
  private val referenceCodeService: ReferenceCodeService = mock()

  private val service = ContactAddressPhoneService(
    contactRepository,
    contactPhoneRepository,
    contactAddressRepository,
    contactAddressPhoneRepository,
    referenceCodeService,
  )

  private val contactId = 1L
  private val contactAddressId = 2L
  private val contactPhoneId = 3L
  private val contactAddressPhoneId = 4L
  private val phoneNumber = "07888 777888"

  @Nested
  inner class CreateAddressSpecificPhone {
    private val request = createContactAddressPhoneRequest(contactAddressId)
    private val user = aUser("created")

    @Test
    fun `should throw EntityNotFoundException if the contact does not exist`() {
      whenever(contactRepository.findById(contactId)).thenReturn(Optional.empty())

      val exception = assertThrows<EntityNotFoundException> {
        service.create(contactId, contactAddressId, request, user)
      }
      assertThat(exception.message).isEqualTo("Contact ($contactId) not found")
    }

    @Test
    fun `should throw EntityNotFoundException if the address does not exist`() {
      whenever(contactRepository.findById(contactId)).thenReturn(Optional.of(contact))
      whenever(contactAddressRepository.findById(contactAddressId)).thenReturn(Optional.empty())

      val exception = assertThrows<EntityNotFoundException> {
        service.create(contactId, contactAddressId, request, user)
      }
      assertThat(exception.message).isEqualTo("Contact address ($contactAddressId) not found")
    }

    @Test
    fun `should throw ValidationException creating an address-specific phone if the phone type is invalid`() {
      val expectedException = ValidationException("Unsupported phone type (FOO)")
      whenever(contactRepository.findById(contactId)).thenReturn(Optional.of(contact))
      whenever(contactAddressRepository.findById(contactAddressId)).thenReturn(Optional.of(contactAddressEntity))
      whenever(referenceCodeService.validateReferenceCode(ReferenceCodeGroup.PHONE_TYPE, "FOO", allowInactive = false)).thenThrow(expectedException)

      val exception = assertThrows<ValidationException> {
        service.create(contactId, contactAddressId, request.copy(phoneType = "FOO"), user)
      }

      assertThat(exception).isEqualTo(expectedException)
      verify(referenceCodeService).validateReferenceCode(ReferenceCodeGroup.PHONE_TYPE, "FOO", allowInactive = false)
    }

    @ParameterizedTest
    @MethodSource("uk.gov.justice.digital.hmpps.hmppscontactsapi.util.PhoneNumberTestUtils#invalidPhoneNumbers")
    fun `should throw ValidationException creating address-specific phone if phone number contains invalid chars`(phoneNumber: String) {
      whenever(contactRepository.findById(contactId)).thenReturn(Optional.of(contact))
      whenever(contactAddressRepository.findById(contactAddressId)).thenReturn(Optional.of(contactAddressEntity))
      whenever(referenceCodeService.validateReferenceCode(ReferenceCodeGroup.PHONE_TYPE, "HOME", allowInactive = false))
        .thenReturn(ReferenceCode(0, ReferenceCodeGroup.PHONE_TYPE, "HOME", "Home", 90, true))

      val exception = assertThrows<ValidationException> {
        service.create(contactId, contactAddressId, request.copy(phoneNumber = phoneNumber), user)
      }

      assertThat(exception.message).isEqualTo("Phone number invalid, it can only contain numbers, () and whitespace with an optional + at the start")
    }

    @Test
    fun `should return a the address-specific phone details after successful creation`() {
      whenever(contactRepository.findById(contactId)).thenReturn(Optional.of(contact))
      whenever(contactAddressRepository.findById(contactAddressId)).thenReturn(Optional.of(contactAddressEntity))
      whenever(referenceCodeService.validateReferenceCode(ReferenceCodeGroup.PHONE_TYPE, "HOME", allowInactive = false))
        .thenReturn(ReferenceCode(0, ReferenceCodeGroup.PHONE_TYPE, "HOME", "Home", 90, true))

      whenever(contactPhoneRepository.saveAndFlush(any())).thenAnswer { i ->
        (i.arguments[0] as ContactPhoneEntity).copy(
          contactPhoneId = contactPhoneId,
          contactId = contactId,
        )
      }

      whenever(contactAddressPhoneRepository.saveAndFlush(any())).thenAnswer { i ->
        (i.arguments[0] as ContactAddressPhoneEntity).copy(
          contactAddressPhoneId = contactAddressPhoneId,
          contactAddressId = contactAddressId,
          contactId = contactId,
        )
      }

      val created = service.create(contactId, contactAddressId, request, user)

      assertThat(created.createdTime).isNotNull()

      with(created) {
        assertThat(contactAddressPhoneId).isEqualTo(contactAddressPhoneId)
        assertThat(contactAddressId).isEqualTo(contactAddressId)
        assertThat(contactPhoneId).isEqualTo(contactPhoneId)
        assertThat(contactId).isEqualTo(contactId)
        assertThat(phoneType).isEqualTo(request.phoneType)
        assertThat(phoneNumber).isEqualTo(request.phoneNumber)
      }
    }
  }

  @Nested
  inner class CreateMultipleAddressSpecificPhone {
    private val request = CreateMultiplePhoneNumbersRequest(
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
    )
    private val user = aUser("created")

    @Test
    fun `should throw EntityNotFoundException if the contact does not exist`() {
      whenever(contactRepository.findById(contactId)).thenReturn(Optional.empty())

      val exception = assertThrows<EntityNotFoundException> {
        service.createMultiple(contactId, contactAddressId, request, user)
      }
      assertThat(exception.message).isEqualTo("Contact ($contactId) not found")
    }

    @Test
    fun `should throw EntityNotFoundException if the address does not exist`() {
      whenever(contactRepository.findById(contactId)).thenReturn(Optional.of(contact))
      whenever(contactAddressRepository.findById(contactAddressId)).thenReturn(Optional.empty())

      val exception = assertThrows<EntityNotFoundException> {
        service.createMultiple(contactId, contactAddressId, request, user)
      }
      assertThat(exception.message).isEqualTo("Contact address ($contactAddressId) not found")
    }

    @Test
    fun `should throw ValidationException creating an address-specific phone if the phone type is invalid`() {
      val expectedException = ValidationException("Unsupported phone type (FOO)")
      whenever(contactRepository.findById(contactId)).thenReturn(Optional.of(contact))
      whenever(contactAddressRepository.findById(contactAddressId)).thenReturn(Optional.of(contactAddressEntity))
      whenever(referenceCodeService.validateReferenceCode(ReferenceCodeGroup.PHONE_TYPE, "FOO", allowInactive = false)).thenThrow(expectedException)

      val exception = assertThrows<ValidationException> {
        service.createMultiple(
          contactId,
          contactAddressId,
          request.copy(
            phoneNumbers = listOf(
              PhoneNumber(
                phoneType = "FOO",
                phoneNumber = "+447777777777",
              ),
            ),
          ),
          user,
        )
      }

      assertThat(exception).isEqualTo(expectedException)
      verify(referenceCodeService).validateReferenceCode(ReferenceCodeGroup.PHONE_TYPE, "FOO", allowInactive = false)
    }

    @ParameterizedTest
    @MethodSource("uk.gov.justice.digital.hmpps.hmppscontactsapi.util.PhoneNumberTestUtils#invalidPhoneNumbers")
    fun `should throw ValidationException creating address-specific phone if phone number contains invalid chars`(phoneNumber: String) {
      whenever(contactRepository.findById(contactId)).thenReturn(Optional.of(contact))
      whenever(contactAddressRepository.findById(contactAddressId)).thenReturn(Optional.of(contactAddressEntity))
      whenever(referenceCodeService.validateReferenceCode(ReferenceCodeGroup.PHONE_TYPE, "HOME", allowInactive = false))
        .thenReturn(ReferenceCode(0, ReferenceCodeGroup.PHONE_TYPE, "HOME", "Home", 90, true))

      val exception = assertThrows<ValidationException> {
        service.createMultiple(
          contactId,
          contactAddressId,
          request.copy(
            phoneNumbers = listOf(
              PhoneNumber(
                phoneType = "MOB",
                phoneNumber = phoneNumber,
              ),
            ),
          ),
          user,
        )
      }

      assertThat(exception.message).isEqualTo("Phone number invalid, it can only contain numbers, () and whitespace with an optional + at the start")
    }

    @Test
    fun `should return a the address-specific phone details after successful creation`() {
      whenever(contactRepository.findById(contactId)).thenReturn(Optional.of(contact))
      whenever(contactAddressRepository.findById(contactAddressId)).thenReturn(Optional.of(contactAddressEntity))
      whenever(referenceCodeService.validateReferenceCode(ReferenceCodeGroup.PHONE_TYPE, "HOME", allowInactive = false))
        .thenReturn(ReferenceCode(0, ReferenceCodeGroup.PHONE_TYPE, "HOME", "Home", 90, true))

      whenever(contactPhoneRepository.saveAndFlush(any())).thenAnswer { i ->
        (i.arguments[0] as ContactPhoneEntity).copy(
          contactPhoneId = 8888,
          contactId = contactId,
        )
      }.thenAnswer { i ->
        (i.arguments[0] as ContactPhoneEntity).copy(
          contactPhoneId = 9999,
          contactId = contactId,
        )
      }

      whenever(contactAddressPhoneRepository.saveAndFlush(any())).thenAnswer { i ->
        (i.arguments[0] as ContactAddressPhoneEntity).copy(
          contactAddressPhoneId = 6666,
          contactAddressId = contactAddressId,
          contactId = contactId,
        )
      }.thenAnswer { i ->
        (i.arguments[0] as ContactAddressPhoneEntity).copy(
          contactAddressPhoneId = 5555,
          contactAddressId = contactAddressId,
          contactId = contactId,
        )
      }

      val created = service.createMultiple(contactId, contactAddressId, request, user)
      assertThat(created).hasSize(2)

      val mobile = created.find { it.phoneType == "MOB" }!!
      with(mobile) {
        assertThat(contactAddressPhoneId).isEqualTo(6666)
        assertThat(contactAddressId).isEqualTo(contactAddressId)
        assertThat(contactPhoneId).isEqualTo(8888)
        assertThat(contactId).isEqualTo(contactId)
        assertThat(phoneType).isEqualTo("MOB")
        assertThat(phoneNumber).isEqualTo("+447777777777")
        assertThat(extNumber).isEqualTo("0123")
      }

      val home = created.find { it.phoneType == "HOME" }!!
      with(home) {
        assertThat(contactAddressPhoneId).isEqualTo(5555)
        assertThat(contactAddressId).isEqualTo(contactAddressId)
        assertThat(contactPhoneId).isEqualTo(9999)
        assertThat(contactId).isEqualTo(contactId)
        assertThat(phoneType).isEqualTo("HOME")
        assertThat(phoneNumber).isEqualTo("01234 567890")
        assertThat(extNumber).isNull()
      }
    }
  }

  @Nested
  inner class CreateMultipleAddressSpecificPhonesReturnIds {
    val contactId = 1L
    val contactAddressId = 2L
    val createdBy = "TEST_USER"
    private val phoneNumbers = listOf(
      PhoneNumber(phoneType = "MOBILE", phoneNumber = "07777123456", extNumber = null),
      PhoneNumber(phoneType = "HOME", phoneNumber = "02071234567", extNumber = "123"),
    )

    @Test
    fun `should throw EntityNotFoundException if the contact does not exist`() {
      whenever(contactRepository.findById(contactId)).thenReturn(Optional.empty())

      val exception = assertThrows<EntityNotFoundException> {
        service.createMultipleAddressSpecificPhones(contactId, contactAddressId, createdBy, phoneNumbers)
      }
      assertThat(exception.message).isEqualTo("Contact ($contactId) not found")
    }

    @Test
    fun `should throw EntityNotFoundException if the address does not exist`() {
      whenever(contactRepository.findById(contactId)).thenReturn(Optional.of(contact))
      whenever(contactAddressRepository.findById(contactAddressId)).thenReturn(Optional.empty())

      val exception = assertThrows<EntityNotFoundException> {
        service.createMultipleAddressSpecificPhones(contactId, contactAddressId, createdBy, phoneNumbers)
      }
      assertThat(exception.message).isEqualTo("Contact address ($contactAddressId) not found")
    }

    @Test
    fun `should throw ValidationException creating an address-specific phone if the phone type is invalid`() {
      val expectedException = ValidationException("Unsupported phone type (FOO)")
      whenever(contactRepository.findById(contactId)).thenReturn(Optional.of(contact))
      whenever(contactAddressRepository.findById(contactAddressId)).thenReturn(Optional.of(contactAddressEntity))
      whenever(referenceCodeService.validateReferenceCode(ReferenceCodeGroup.PHONE_TYPE, "FOO", allowInactive = false)).thenThrow(expectedException)

      val exception = assertThrows<ValidationException> {
        service.createMultipleAddressSpecificPhones(
          contactId,
          contactAddressId,
          createdBy,
          phoneNumbers = listOf(
            PhoneNumber(
              phoneType = "FOO",
              phoneNumber = "+447777777777",
            ),
          ),
        )
      }

      assertThat(exception).isEqualTo(expectedException)
      verify(referenceCodeService).validateReferenceCode(ReferenceCodeGroup.PHONE_TYPE, "FOO", allowInactive = false)
    }

    @ParameterizedTest
    @MethodSource("uk.gov.justice.digital.hmpps.hmppscontactsapi.util.PhoneNumberTestUtils#invalidPhoneNumbers")
    fun `should throw ValidationException creating address-specific phone if phone number contains invalid chars`(phoneNumber: String) {
      whenever(contactRepository.findById(contactId)).thenReturn(Optional.of(contact))
      whenever(contactAddressRepository.findById(contactAddressId)).thenReturn(Optional.of(contactAddressEntity))
      whenever(referenceCodeService.validateReferenceCode(ReferenceCodeGroup.PHONE_TYPE, "HOME", allowInactive = false))
        .thenReturn(ReferenceCode(0, ReferenceCodeGroup.PHONE_TYPE, "HOME", "Home", 90, true))

      val exception = assertThrows<ValidationException> {
        service.createMultipleAddressSpecificPhones(
          contactId,
          contactAddressId,
          createdBy,
          phoneNumbers = listOf(
            PhoneNumber(
              phoneType = "MOB",
              phoneNumber = phoneNumber,
            ),
          ),
        )
      }

      assertThat(exception.message).isEqualTo("Phone number invalid, it can only contain numbers, () and whitespace with an optional + at the start")
    }

    @Test
    fun `should return a the address-specific phone details after successful creation`() {
      whenever(contactRepository.findById(contactId)).thenReturn(Optional.of(contact))
      whenever(contactAddressRepository.findById(contactAddressId)).thenReturn(Optional.of(contactAddressEntity))
      whenever(referenceCodeService.validateReferenceCode(ReferenceCodeGroup.PHONE_TYPE, "HOME", allowInactive = false))
        .thenReturn(ReferenceCode(0, ReferenceCodeGroup.PHONE_TYPE, "HOME", "Home", 90, true))

      whenever(contactPhoneRepository.saveAndFlush(any())).thenAnswer { i ->
        (i.arguments[0] as ContactPhoneEntity).copy(
          contactPhoneId = 8888,
          contactId = contactId,
        )
      }.thenAnswer { i ->
        (i.arguments[0] as ContactPhoneEntity).copy(
          contactPhoneId = 9999,
          contactId = contactId,
        )
      }

      whenever(contactAddressPhoneRepository.saveAndFlush(any())).thenAnswer { i ->
        (i.arguments[0] as ContactAddressPhoneEntity).copy(
          contactAddressPhoneId = 6666,
          contactAddressId = contactAddressId,
          contactId = contactId,
        )
      }.thenAnswer { i ->
        (i.arguments[0] as ContactAddressPhoneEntity).copy(
          contactAddressPhoneId = 5555,
          contactAddressId = contactAddressId,
          contactId = contactId,
        )
      }

      val created = service.createMultipleAddressSpecificPhones(contactId, contactAddressId, createdBy, phoneNumbers)
      assertThat(created).hasSize(2).contains(6666L, 5555L)
    }
  }

  @Nested
  inner class GetAddressSpecificPhone {
    @Test
    fun `get address-specific phone number by ID`() {
      whenever(contactAddressPhoneRepository.findById(contactAddressPhoneId)).thenReturn(Optional.of(addressPhoneEntity))
      whenever(contactPhoneRepository.findById(contactPhoneId)).thenReturn(Optional.of(phoneEntity))
      whenever(referenceCodeService.getReferenceDataByGroupAndCode(ReferenceCodeGroup.PHONE_TYPE, "HOME"))
        .thenReturn(ReferenceCode(0, ReferenceCodeGroup.PHONE_TYPE, "HOME", "Home", 90, true))

      val response = service.get(contactId, contactAddressPhoneId)

      with(response) {
        assertThat(this.contactAddressPhoneId).isEqualTo(contactAddressPhoneId)
        assertThat(this.contactPhoneId).isEqualTo(contactPhoneId)
        assertThat(this.phoneType).isEqualTo("HOME")
        assertThat(this.phoneNumber).isEqualTo(phoneNumber)
        assertThat(this.createdBy).isEqualTo("USER1")
      }

      verify(contactAddressPhoneRepository).findById(contactAddressPhoneId)
      verify(contactPhoneRepository).findById(contactPhoneId)
      verify(referenceCodeService).getReferenceDataByGroupAndCode(ReferenceCodeGroup.PHONE_TYPE, "HOME")
    }

    @Test
    fun `throws an exception if the address specific phone number is not found`() {
      whenever(contactAddressPhoneRepository.findById(contactAddressPhoneId)).thenReturn(Optional.empty())
      whenever(contactPhoneRepository.findById(contactPhoneId)).thenReturn(Optional.of(phoneEntity))
      whenever(referenceCodeService.getReferenceDataByGroupAndCode(ReferenceCodeGroup.PHONE_TYPE, "HOME"))
        .thenReturn(ReferenceCode(0, ReferenceCodeGroup.PHONE_TYPE, "HOME", "Home", 90, true))

      val exception = assertThrows<EntityNotFoundException> {
        service.get(contactId, contactAddressPhoneId)
      }

      assertThat(exception.message).isEqualTo("Contact address phone ($contactAddressPhoneId) not found")

      verify(contactAddressPhoneRepository).findById(contactAddressPhoneId)
      verify(contactPhoneRepository, never()).findById(contactPhoneId)
      verify(referenceCodeService, never()).getReferenceDataByGroupAndCode(any(), any())
    }

    @Test
    fun `throw an exception if the phone number is not found`() {
      whenever(contactAddressPhoneRepository.findById(contactAddressPhoneId)).thenReturn(Optional.of(addressPhoneEntity))
      whenever(contactPhoneRepository.findById(contactPhoneId)).thenReturn(Optional.empty())
      whenever(referenceCodeService.getReferenceDataByGroupAndCode(ReferenceCodeGroup.PHONE_TYPE, "HOME"))
        .thenReturn(ReferenceCode(0, ReferenceCodeGroup.PHONE_TYPE, "HOME", "Home", 90, true))

      val exception = assertThrows<EntityNotFoundException> {
        service.get(contactId, contactAddressPhoneId)
      }

      assertThat(exception.message).isEqualTo("Contact phone ($contactPhoneId) not found")

      verify(contactAddressPhoneRepository).findById(contactAddressPhoneId)
      verify(contactPhoneRepository).findById(contactPhoneId)
      verify(referenceCodeService, never()).getReferenceDataByGroupAndCode(any(), any())
    }
  }

  @Nested
  inner class UpdateAddressSpecificPhone {
    private val request = UpdateContactAddressPhoneRequest(
      "HOME",
      "+44  555 878787",
      "0123",
    )
    private val user = aUser("AMEND_USER")

    @Test
    fun `should throw EntityNotFoundException updating address-specific phone if contact doesn't exist`() {
      whenever(contactRepository.findById(contactId)).thenReturn(Optional.empty())
      val exception = assertThrows<EntityNotFoundException> {
        service.update(contactId, contactAddressPhoneId, request, user)
      }
      assertThat(exception.message).isEqualTo("Contact ($contactId) not found")
    }

    @Test
    fun `should throw EntityNotFoundException updating address-specific phone if phone doesn't exist`() {
      whenever(contactRepository.findById(contactId)).thenReturn(Optional.of(contact))
      whenever(contactAddressPhoneRepository.findById(contactAddressPhoneId)).thenReturn(Optional.of(addressPhoneEntity))
      whenever(contactPhoneRepository.findById(contactPhoneId)).thenReturn(Optional.empty())

      val exception = assertThrows<EntityNotFoundException> {
        service.update(contactId, contactAddressPhoneId, request, user)
      }

      assertThat(exception.message).isEqualTo("Contact phone ($contactPhoneId) not found")
    }

    @Test
    fun `should throw ValidationException updating phone if phone type is invalid`() {
      whenever(contactRepository.findById(contactId)).thenReturn(Optional.of(contact))
      whenever(contactAddressPhoneRepository.findById(contactAddressPhoneId)).thenReturn(Optional.of(addressPhoneEntity))
      whenever(contactPhoneRepository.findById(contactPhoneId)).thenReturn(Optional.of(phoneEntity))
      val expectedException = ValidationException("Unsupported phone type (FOO)")
      whenever(referenceCodeService.validateReferenceCode(ReferenceCodeGroup.PHONE_TYPE, "FOO", allowInactive = true)).thenThrow(expectedException)

      val exception = assertThrows<ValidationException> {
        service.update(contactId, contactAddressPhoneId, request.copy(phoneType = "FOO"), user)
      }

      assertThat(exception).isEqualTo(expectedException)
      verify(referenceCodeService).validateReferenceCode(ReferenceCodeGroup.PHONE_TYPE, "FOO", allowInactive = true)
    }

    @ParameterizedTest
    @MethodSource("uk.gov.justice.digital.hmpps.hmppscontactsapi.util.PhoneNumberTestUtils#invalidPhoneNumbers")
    fun `should throw ValidationException updating address-specific phone if number contains invalid chars`(phone: String) {
      whenever(contactRepository.findById(contactId)).thenReturn(Optional.of(contact))
      whenever(contactAddressPhoneRepository.findById(contactAddressPhoneId)).thenReturn(Optional.of(addressPhoneEntity))
      whenever(contactPhoneRepository.findById(contactPhoneId)).thenReturn(Optional.of(phoneEntity))
      whenever(referenceCodeService.getReferenceDataByGroupAndCode(ReferenceCodeGroup.PHONE_TYPE, "HOME"))
        .thenReturn(ReferenceCode(0, ReferenceCodeGroup.PHONE_TYPE, "HOME", "Home", 90, true))

      val exception = assertThrows<ValidationException> {
        service.update(contactId, contactAddressPhoneId, request.copy(phoneNumber = phone), user)
      }
      assertThat(exception.message).isEqualTo("Phone number invalid, it can only contain numbers, () and whitespace with an optional + at the start")
    }

    @Test
    fun `should return a success response after updating an address-specific phone number`() {
      whenever(contactRepository.findById(contactId)).thenReturn(Optional.of(contact))
      whenever(contactAddressPhoneRepository.findById(contactAddressPhoneId)).thenReturn(Optional.of(addressPhoneEntity))
      whenever(contactPhoneRepository.findById(contactPhoneId)).thenReturn(Optional.of(phoneEntity))
      whenever(referenceCodeService.validateReferenceCode(ReferenceCodeGroup.PHONE_TYPE, "HOME", allowInactive = true))
        .thenReturn(ReferenceCode(0, ReferenceCodeGroup.PHONE_TYPE, "HOME", "Home", 90, true))

      whenever(contactPhoneRepository.saveAndFlush(any())).thenAnswer { i ->
        (i.arguments[0] as ContactPhoneEntity).copy(
          contactPhoneId = contactPhoneId,
          contactId = contactId,
          phoneNumber = request.phoneNumber,
          extNumber = request.extNumber,
          phoneType = request.phoneType,
        )
      }

      whenever(contactAddressPhoneRepository.saveAndFlush(any())).thenAnswer { i ->
        (i.arguments[0] as ContactAddressPhoneEntity).copy(
          contactAddressPhoneId = contactAddressPhoneId,
          contactAddressId = contactAddressId,
          contactPhoneId = contactPhoneId,
          contactId = contactId,
        )
      }

      val updated = service.update(contactId, contactAddressPhoneId, request, user)

      assertThat(updated.updatedTime).isNotNull()

      with(updated) {
        assertThat(this.phoneNumber).isEqualTo(request.phoneNumber)
        assertThat(this.phoneType).isEqualTo(request.phoneType)
        assertThat(this.extNumber).isEqualTo(request.extNumber)
      }

      verify(contactRepository).findById(contactId)
      verify(contactAddressPhoneRepository).findById(contactAddressPhoneId)
      verify(contactPhoneRepository).findById(contactPhoneId)
      verify(referenceCodeService).validateReferenceCode(ReferenceCodeGroup.PHONE_TYPE, "HOME", true)
    }
  }

  @Nested
  inner class DeleteAddressSpecificPhone {

    @Test
    fun `should throw EntityNotFoundException deleting if contact doesn't exist`() {
      whenever(contactRepository.findById(contactId)).thenReturn(Optional.empty())

      val exception = assertThrows<EntityNotFoundException> {
        service.delete(contactId, contactAddressPhoneId)
      }

      assertThat(exception.message).isEqualTo("Contact ($contactId) not found")
    }

    @Test
    fun `should throw EntityNotFoundException deleting if address-specific phone doesn't exist`() {
      whenever(contactRepository.findById(contactId)).thenReturn(Optional.of(contact))
      whenever(contactAddressPhoneRepository.findById(contactAddressPhoneId)).thenReturn(Optional.empty())

      val exception = assertThrows<EntityNotFoundException> {
        service.delete(contactId, contactAddressPhoneId)
      }

      assertThat(exception.message).isEqualTo("Contact address phone ($contactAddressPhoneId) not found")
    }

    @Test
    fun `should throw EntityNotFoundException deleting if the phone doesn't exist`() {
      whenever(contactRepository.findById(contactId)).thenReturn(Optional.of(contact))
      whenever(contactAddressPhoneRepository.findById(contactAddressPhoneId)).thenReturn(Optional.of(addressPhoneEntity))
      whenever(contactPhoneRepository.findById(contactPhoneId)).thenReturn(Optional.empty())

      val exception = assertThrows<EntityNotFoundException> {
        service.delete(contactId, contactAddressPhoneId)
      }

      assertThat(exception.message).isEqualTo("Contact phone ($contactPhoneId) not found")
    }

    @Test
    fun `should delete the address-specific phone and the phone details`() {
      whenever(contactRepository.findById(contactId)).thenReturn(Optional.of(contact))
      whenever(contactAddressPhoneRepository.findById(contactAddressPhoneId)).thenReturn(Optional.of(addressPhoneEntity))
      whenever(contactPhoneRepository.findById(contactPhoneId)).thenReturn(Optional.of(phoneEntity))
      whenever(referenceCodeService.getReferenceDataByGroupAndCode(ReferenceCodeGroup.PHONE_TYPE, "HOME"))
        .thenReturn(ReferenceCode(0, ReferenceCodeGroup.PHONE_TYPE, "HOME", "Home", 90, true))

      service.delete(contactId, contactAddressPhoneId)

      verify(contactAddressPhoneRepository).deleteById(contactAddressPhoneId)
      verify(contactPhoneRepository).deleteById(contactPhoneId)
    }
  }

  private val contact = ContactEntity(
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

  private val contactAddressEntity =
    ContactAddressEntity(
      contactAddressId = contactAddressId,
      contactId = contactId,
      addressType = "HOME",
      primaryAddress = true,
      flat = "1B",
      property = "Mason House",
      street = "Main Street",
      area = "Howarth",
      cityCode = "LEEDS",
      countyCode = "YORKS",
      postCode = "LS13 4KD",
      countryCode = "UK",
      createdBy = "TEST",
      createdTime = now(),
    )

  private val addressPhoneEntity = ContactAddressPhoneEntity(
    contactAddressPhoneId = contactAddressPhoneId,
    contactAddressId = contactAddressId,
    contactPhoneId = contactPhoneId,
    contactId = contactId,
    createdBy = "USER1",
    createdTime = now(),
    updatedBy = null,
    updatedTime = null,
  )

  private val phoneEntity = ContactPhoneEntity(
    contactPhoneId = contactPhoneId,
    contactId = contactId,
    phoneType = "HOME",
    phoneNumber = phoneNumber,
    createdBy = "USER1",
    createdTime = now(),
  )
}
