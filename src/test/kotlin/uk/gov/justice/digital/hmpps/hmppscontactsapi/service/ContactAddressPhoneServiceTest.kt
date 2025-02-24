package uk.gov.justice.digital.hmpps.hmppscontactsapi.service

import jakarta.persistence.EntityNotFoundException
import jakarta.validation.ValidationException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.ContactAddressEntity
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.ContactAddressPhoneEntity
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.ContactEntity
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.ContactPhoneEntity
import uk.gov.justice.digital.hmpps.hmppscontactsapi.helpers.createContactAddressPhoneRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.ReferenceCodeGroup
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.UpdateContactAddressPhoneRequest
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

    @Test
    fun `should throw EntityNotFoundException if the contact does not exist`() {
      whenever(contactRepository.findById(contactId)).thenReturn(Optional.empty())

      val exception = assertThrows<EntityNotFoundException> {
        service.create(contactId, contactAddressId, request)
      }
      assertThat(exception.message).isEqualTo("Contact ($contactId) not found")
    }

    @Test
    fun `should throw EntityNotFoundException if the address does not exist`() {
      whenever(contactRepository.findById(contactId)).thenReturn(Optional.of(contact))
      whenever(contactAddressRepository.findById(contactAddressId)).thenReturn(Optional.empty())

      val exception = assertThrows<EntityNotFoundException> {
        service.create(contactId, contactAddressId, request)
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
        service.create(contactId, contactAddressId, request.copy(phoneType = "FOO"))
      }

      assertThat(exception).isEqualTo(expectedException)
      verify(referenceCodeService).validateReferenceCode(ReferenceCodeGroup.PHONE_TYPE, "FOO", allowInactive = false)
    }

    @ParameterizedTest
    @CsvSource(
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
      quoteCharacter = 'X',
    )
    fun `should throw ValidationException creating address-specific phone if phone number contains invalid chars`(phoneNumber: String) {
      whenever(contactRepository.findById(contactId)).thenReturn(Optional.of(contact))
      whenever(contactAddressRepository.findById(contactAddressId)).thenReturn(Optional.of(contactAddressEntity))
      whenever(referenceCodeService.validateReferenceCode(ReferenceCodeGroup.PHONE_TYPE, "HOME", allowInactive = false))
        .thenReturn(ReferenceCode(0, ReferenceCodeGroup.PHONE_TYPE, "HOME", "Home", 90, true))

      val exception = assertThrows<ValidationException> {
        service.create(contactId, contactAddressId, request.copy(phoneNumber = phoneNumber))
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

      val created = service.create(contactId, contactAddressId, request)

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
      "AMEND_USER",
    )

    @Test
    fun `should throw EntityNotFoundException updating address-specific phone if contact doesn't exist`() {
      whenever(contactRepository.findById(contactId)).thenReturn(Optional.empty())
      val exception = assertThrows<EntityNotFoundException> {
        service.update(contactId, contactAddressPhoneId, request)
      }
      assertThat(exception.message).isEqualTo("Contact ($contactId) not found")
    }

    @Test
    fun `should throw EntityNotFoundException updating address-specific phone if phone doesn't exist`() {
      whenever(contactRepository.findById(contactId)).thenReturn(Optional.of(contact))
      whenever(contactAddressPhoneRepository.findById(contactAddressPhoneId)).thenReturn(Optional.of(addressPhoneEntity))
      whenever(contactPhoneRepository.findById(contactPhoneId)).thenReturn(Optional.empty())

      val exception = assertThrows<EntityNotFoundException> {
        service.update(contactId, contactAddressPhoneId, request)
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
        service.update(contactId, contactAddressPhoneId, request.copy(phoneType = "FOO"))
      }

      assertThat(exception).isEqualTo(expectedException)
      verify(referenceCodeService).validateReferenceCode(ReferenceCodeGroup.PHONE_TYPE, "FOO", allowInactive = true)
    }

    @ParameterizedTest
    @CsvSource(
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
      quoteCharacter = 'X',
    )
    fun `should throw ValidationException updating address-specific phone if number contains invalid chars`(phone: String) {
      whenever(contactRepository.findById(contactId)).thenReturn(Optional.of(contact))
      whenever(contactAddressPhoneRepository.findById(contactAddressPhoneId)).thenReturn(Optional.of(addressPhoneEntity))
      whenever(contactPhoneRepository.findById(contactPhoneId)).thenReturn(Optional.of(phoneEntity))
      whenever(referenceCodeService.getReferenceDataByGroupAndCode(ReferenceCodeGroup.PHONE_TYPE, "HOME"))
        .thenReturn(ReferenceCode(0, ReferenceCodeGroup.PHONE_TYPE, "HOME", "Home", 90, true))

      val exception = assertThrows<ValidationException> {
        service.update(contactId, contactAddressPhoneId, request.copy(phoneNumber = phone))
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

      val updated = service.update(contactId, contactAddressPhoneId, request)

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
