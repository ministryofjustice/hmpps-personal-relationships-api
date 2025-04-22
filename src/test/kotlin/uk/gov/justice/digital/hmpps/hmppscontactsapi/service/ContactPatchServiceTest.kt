package uk.gov.justice.digital.hmpps.hmppscontactsapi.service

import jakarta.persistence.EntityNotFoundException
import jakarta.validation.ValidationException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.whenever
import org.openapitools.jackson.nullable.JsonNullable
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.ContactEntity
import uk.gov.justice.digital.hmpps.hmppscontactsapi.helpers.aUser
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.ReferenceCodeGroup
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.PatchContactRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.PatchContactResponse
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.ReferenceCode
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.ContactRepository
import java.lang.Boolean.TRUE
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class ContactPatchServiceTest {

  private val contactId = 1L
  private val domesticStatusCode = "P"
  private var originalContact = createDummyContactEntity()
  private val languageCode = "FRE-FRA"

  private val contactRepository: ContactRepository = mock()
  private val referenceCodeService: ReferenceCodeService = mock()
  private val user = aUser("updated")

  private val service = ContactPatchService(contactRepository, referenceCodeService)

  @Test
  fun `should throw EntityNotFoundException when contact does not exist`() {
    val patchRequest = PatchContactRequest(
      languageCode = JsonNullable.of("ENG"),
    )

    whenever(contactRepository.findById(contactId)).thenReturn(Optional.empty())

    assertThrows<EntityNotFoundException> {
      service.patch(contactId, patchRequest, user)
    }
  }

  @Test
  fun `should patch when only the updated by field is provided`() {
    val patchRequest = PatchContactRequest()

    whenContactExists()
    whenUpdateIsSuccessful()

    val updatedContact = service.patch(contactId, patchRequest, user)

    val contactCaptor = argumentCaptor<ContactEntity>()

    verify(contactRepository).saveAndFlush(contactCaptor.capture())
    verify(referenceCodeService, never()).validateReferenceCode(any(), any(), any())

    assertUnchangedFields(updatedContact)

    assertThat(updatedContact.updatedBy).isEqualTo("updated")
  }

  @Nested
  inner class LanguageCode {

    @Test
    fun `should patch when language code is null`() {
      val patchRequest = PatchContactRequest(
        languageCode = JsonNullable.of(null),
      )

      whenContactExists()
      whenUpdateIsSuccessful()

      val updatedContact = service.patch(contactId, patchRequest, user)

      assertThat(updatedContact.languageCode).isEqualTo(null)
      assertThat(updatedContact.updatedBy).isEqualTo("updated")
    }

    @Test
    fun `should patch without validating a null language code `() {
      val patchRequest = PatchContactRequest(
        languageCode = JsonNullable.of(null),
      )

      whenContactExists()
      whenUpdateIsSuccessful()

      service.patch(contactId, patchRequest, user)

      verify(referenceCodeService, never()).validateReferenceCode(any(), any(), any())
      verify(contactRepository, times(1)).saveAndFlush(any())
    }

    @Test
    fun `should patch without validating undefined language code`() {
      val patchRequest = PatchContactRequest()

      whenContactExists()
      whenUpdateIsSuccessful()

      service.patch(contactId, patchRequest, user)

      verify(referenceCodeService, never()).validateReferenceCode(any(), any(), any())
      verify(contactRepository, times(1)).saveAndFlush(any())
    }

    @Test
    fun `should patch language code when existing value is null`() {
      originalContact = createDummyContactEntity(languageCode = null)

      val patchRequest = PatchContactRequest(
        languageCode = JsonNullable.of(languageCode),
      )

      whenContactExists()
      whenUpdateIsSuccessful()
      whenever(referenceCodeService.validateReferenceCode(ReferenceCodeGroup.LANGUAGE, languageCode, allowInactive = true)).thenReturn(
        ReferenceCode(1, ReferenceCodeGroup.LANGUAGE, languageCode, "French", 1, true),
      )

      val updatedContact = service.patch(contactId, patchRequest, user)

      val contactCaptor = argumentCaptor<ContactEntity>()

      verify(contactRepository).saveAndFlush(contactCaptor.capture())
      verify(referenceCodeService, times(1)).validateReferenceCode(ReferenceCodeGroup.LANGUAGE, languageCode, allowInactive = true)

      assertThat(updatedContact.languageCode).isEqualTo(languageCode)
      assertThat(updatedContact.updatedBy).isEqualTo("updated")
    }

    @Test
    fun `should patch when language code is valid`() {
      val patchRequest = PatchContactRequest(
        languageCode = JsonNullable.of("FR"),
      )

      whenContactExists()
      whenUpdateIsSuccessful()

      val response = service.patch(contactId, patchRequest, user)

      val contactCaptor = argumentCaptor<ContactEntity>()

      verify(contactRepository).saveAndFlush(contactCaptor.capture())
      verify(referenceCodeService).validateReferenceCode(ReferenceCodeGroup.LANGUAGE, "FR", allowInactive = true)

      val updatingEntity = contactCaptor.firstValue

      assertThat(updatingEntity.languageCode).isEqualTo(patchRequest.languageCode.get())
      assertThat(updatingEntity.updatedBy).isEqualTo("updated")

      assertThat(response.languageCode).isEqualTo(patchRequest.languageCode.get())
      assertThat(response.updatedBy).isEqualTo("updated")
    }
  }

  @Nested
  inner class InterpreterRequired {

    @Test
    fun `should patch when interpreter required is valid`() {
      val patchRequest = PatchContactRequest(
        interpreterRequired = JsonNullable.of(TRUE),
      )

      whenContactExists()
      whenUpdateIsSuccessful()

      val response = service.patch(contactId, patchRequest, user)

      val contactCaptor = argumentCaptor<ContactEntity>()

      verify(contactRepository).saveAndFlush(contactCaptor.capture())

      val updatingEntity = contactCaptor.firstValue

      assertThat(updatingEntity.interpreterRequired).isEqualTo(patchRequest.interpreterRequired.get())
      assertThat(updatingEntity.updatedBy).isEqualTo("updated")

      assertThat(response.interpreterRequired).isEqualTo(patchRequest.interpreterRequired.get())
      assertThat(response.updatedBy).isEqualTo("updated")
    }

    @Test
    fun `should throw validation error when interpreter required is null`() {
      val patchRequest = PatchContactRequest(
        interpreterRequired = JsonNullable.of(null),
      )

      whenContactExists()

      val exception = assertThrows<ValidationException> {
        service.patch(contactId, patchRequest, user)
      }
      assertThat(exception.message).isEqualTo("Unsupported interpreter required type null.")
    }
  }

  @Nested
  inner class DomesticStatusCode {

    @Test
    fun `should patch when domestic status code is null`() {
      val patchRequest = PatchContactRequest(
        domesticStatusCode = JsonNullable.of(null),
      )

      whenContactExists()
      whenUpdateIsSuccessful()

      val updatedContact = service.patch(contactId, patchRequest, user)

      assertThat(updatedContact.domesticStatusCode).isEqualTo(null)
      assertThat(updatedContact.updatedBy).isEqualTo("updated")
    }

    @Test
    fun `should patch without validating a null domestic status code `() {
      val patchRequest = PatchContactRequest(
        domesticStatusCode = JsonNullable.of(null),
      )

      whenContactExists()
      whenUpdateIsSuccessful()

      service.patch(contactId, patchRequest, user)

      verify(referenceCodeService, never()).validateReferenceCode(any(), any(), any())
      verify(contactRepository, times(1)).saveAndFlush(any())
    }

    @Test
    fun `should patch without validating undefined domestic status code`() {
      val patchRequest = PatchContactRequest()

      whenContactExists()
      whenUpdateIsSuccessful()

      service.patch(contactId, patchRequest, user)

      verify(referenceCodeService, never()).validateReferenceCode(any(), any(), any())
      verify(contactRepository, times(1)).saveAndFlush(any())
    }

    @Test
    fun `should patch domestic status code when existing value is null`() {
      originalContact = createDummyContactEntity(domesticStatus = null)

      val patchRequest = PatchContactRequest(
        domesticStatusCode = JsonNullable.of(domesticStatusCode),
      )

      whenContactExists()
      whenUpdateIsSuccessful()
      whenever(referenceCodeService.validateReferenceCode(ReferenceCodeGroup.DOMESTIC_STS, domesticStatusCode, allowInactive = true)).thenReturn(
        ReferenceCode(
          0,
          ReferenceCodeGroup.PHONE_TYPE,
          "MOB",
          "Mobile",
          90,
          true,
        ),
      )

      val updatedContact = service.patch(contactId, patchRequest, user)

      val contactCaptor = argumentCaptor<ContactEntity>()

      verify(contactRepository).saveAndFlush(contactCaptor.capture())
      verify(referenceCodeService, times(1)).validateReferenceCode(ReferenceCodeGroup.DOMESTIC_STS, domesticStatusCode, allowInactive = true)

      assertThat(updatedContact.domesticStatusCode).isEqualTo(domesticStatusCode)
      assertThat(updatedContact.updatedBy).isEqualTo("updated")
    }

    @Test
    fun `should patch when domestic status code is valid`() {
      val patchRequest = PatchContactRequest(
        domesticStatusCode = JsonNullable.of(domesticStatusCode),
      )

      whenContactExists()
      whenUpdateIsSuccessful()
      whenever(referenceCodeService.validateReferenceCode(ReferenceCodeGroup.DOMESTIC_STS, domesticStatusCode, allowInactive = true)).thenReturn(
        ReferenceCode(1, ReferenceCodeGroup.DOMESTIC_STS, "P", "Single", 1, true),
      )

      val response = service.patch(contactId, patchRequest, user)

      val contactCaptor = argumentCaptor<ContactEntity>()

      verify(contactRepository).saveAndFlush(contactCaptor.capture())
      verify(referenceCodeService).validateReferenceCode(ReferenceCodeGroup.DOMESTIC_STS, domesticStatusCode, allowInactive = true)

      val updatingEntity = contactCaptor.firstValue

      assertThat(updatingEntity.domesticStatus).isEqualTo(patchRequest.domesticStatusCode.get())
      assertThat(updatingEntity.updatedBy).isEqualTo("updated")

      assertThat(response.domesticStatusCode).isEqualTo(patchRequest.domesticStatusCode.get())
      assertThat(response.updatedBy).isEqualTo("updated")
    }
  }

  @Nested
  inner class Staff {

    @Test
    fun `should patch when is staff flag is valid`() {
      val patchRequest = PatchContactRequest(
        isStaff = JsonNullable.of(true),
      )

      whenContactExists()
      whenUpdateIsSuccessful()

      val response = service.patch(contactId, patchRequest, user)

      val contactCaptor = argumentCaptor<ContactEntity>()

      verify(contactRepository).saveAndFlush(contactCaptor.capture())

      val updatingEntity = contactCaptor.firstValue

      assertThat(updatingEntity.staffFlag).isEqualTo(patchRequest.isStaff.get())
      assertThat(updatingEntity.updatedBy).isEqualTo("updated")

      assertThat(response.isStaff).isEqualTo(patchRequest.isStaff.get())
      assertThat(response.updatedBy).isEqualTo("updated")
    }

    @Test
    fun `should throw validation error when is staff flag is null`() {
      val patchRequest = PatchContactRequest(
        isStaff = JsonNullable.of(null),
      )

      whenContactExists()

      val exception = assertThrows<ValidationException> {
        service.patch(contactId, patchRequest, user)
      }
      assertThat(exception.message).isEqualTo("Unsupported staff flag value null.")
    }

    @Test
    fun `should patch staff flag is undefined`() {
      val patchRequest = PatchContactRequest(
        isStaff = JsonNullable.undefined(),
      )

      whenContactExists()
      whenUpdateIsSuccessful()

      service.patch(contactId, patchRequest, user)

      verify(contactRepository, times(1)).saveAndFlush(any())
    }
  }

  @Nested
  inner class Title {

    @Test
    fun `should patch when title is valid`() {
      whenever(referenceCodeService.validateReferenceCode(ReferenceCodeGroup.TITLE, "MRS", allowInactive = true)).thenReturn(
        ReferenceCode(
          0,
          ReferenceCodeGroup.TITLE,
          "MRS",
          "Mrs",
          0,
          true,
        ),
      )

      val patchRequest = PatchContactRequest(
        titleCode = JsonNullable.of("MRS"),
      )

      whenContactExists()
      whenUpdateIsSuccessful()

      val response = service.patch(contactId, patchRequest, user)

      val contactCaptor = argumentCaptor<ContactEntity>()

      verify(contactRepository).saveAndFlush(contactCaptor.capture())

      val updatingEntity = contactCaptor.firstValue

      assertThat(updatingEntity.title).isEqualTo("MRS")
      assertThat(updatingEntity.updatedBy).isEqualTo("updated")

      assertThat(response.titleCode).isEqualTo("MRS")
      assertThat(response.updatedBy).isEqualTo("updated")
    }

    @Test
    fun `should patch when title is null`() {
      val patchRequest = PatchContactRequest(
        titleCode = JsonNullable.of(null),
      )

      whenContactExists()
      whenUpdateIsSuccessful()

      val response = service.patch(contactId, patchRequest, user)

      val contactCaptor = argumentCaptor<ContactEntity>()

      verify(contactRepository).saveAndFlush(contactCaptor.capture())

      val updatingEntity = contactCaptor.firstValue

      assertThat(updatingEntity.title).isNull()
      assertThat(updatingEntity.updatedBy).isEqualTo("updated")

      assertThat(response.titleCode).isNull()
      assertThat(response.updatedBy).isEqualTo("updated")
      verify(referenceCodeService, never()).validateReferenceCode(any(), any(), any())
    }

    @Test
    fun `should not patch title if it is invalid code`() {
      val expectedException = ValidationException("Invalid")
      whenever(referenceCodeService.validateReferenceCode(ReferenceCodeGroup.TITLE, "FOO", allowInactive = true)).thenThrow(expectedException)
      val patchRequest = PatchContactRequest(
        titleCode = JsonNullable.of("FOO"),
      )

      whenContactExists()
      whenUpdateIsSuccessful()

      val exception = assertThrows<ValidationException> {
        service.patch(contactId, patchRequest, user)
      }
      assertThat(exception).isEqualTo(expectedException)
      verify(contactRepository, never()).saveAndFlush(any())
    }
  }

  @Nested
  inner class MiddleNames {

    @Test
    fun `should patch when middle names is valid`() {
      val patchRequest = PatchContactRequest(
        middleNames = JsonNullable.of("Some Middle Names Updated"),
      )

      whenContactExists()
      whenUpdateIsSuccessful()

      val response = service.patch(contactId, patchRequest, user)

      val contactCaptor = argumentCaptor<ContactEntity>()

      verify(contactRepository).saveAndFlush(contactCaptor.capture())

      val updatingEntity = contactCaptor.firstValue

      assertThat(updatingEntity.middleNames).isEqualTo("Some Middle Names Updated")
      assertThat(updatingEntity.updatedBy).isEqualTo("updated")

      assertThat(response.middleNames).isEqualTo("Some Middle Names Updated")
      assertThat(response.updatedBy).isEqualTo("updated")
    }

    @Test
    fun `should patch when middle names is null`() {
      val patchRequest = PatchContactRequest(
        middleNames = JsonNullable.of(null),
      )

      whenContactExists()
      whenUpdateIsSuccessful()

      val response = service.patch(contactId, patchRequest, user)

      val contactCaptor = argumentCaptor<ContactEntity>()

      verify(contactRepository).saveAndFlush(contactCaptor.capture())

      val updatingEntity = contactCaptor.firstValue

      assertThat(updatingEntity.middleNames).isNull()
      assertThat(updatingEntity.updatedBy).isEqualTo("updated")

      assertThat(response.middleNames).isNull()
      assertThat(response.updatedBy).isEqualTo("updated")
      verify(referenceCodeService, never()).validateReferenceCode(any(), any(), any())
    }
  }

  @Nested
  inner class DateOfBirth {

    @Test
    fun `should patch when date of birth is null`() {
      originalContact = createDummyContactEntity().copy(dateOfBirth = LocalDate.of(1982, 6, 15))
      val patchRequest = PatchContactRequest(
        dateOfBirth = JsonNullable.of(null),
      )

      whenContactExists()
      whenUpdateIsSuccessful()

      val updatedContact = service.patch(contactId, patchRequest, user)

      assertThat(updatedContact.dateOfBirth).isNull()
      assertThat(updatedContact.updatedBy).isEqualTo("updated")
    }

    @Test
    fun `should patch when date of birth provided`() {
      originalContact = createDummyContactEntity().copy(dateOfBirth = LocalDate.of(1982, 6, 15))

      val patchRequest = PatchContactRequest(
        dateOfBirth = JsonNullable.of(LocalDate.of(2000, 12, 25)),
      )

      whenContactExists()
      whenUpdateIsSuccessful()

      val updatedContact = service.patch(contactId, patchRequest, user)

      val contactCaptor = argumentCaptor<ContactEntity>()

      verify(contactRepository).saveAndFlush(contactCaptor.capture())

      assertThat(updatedContact.dateOfBirth).isEqualTo(LocalDate.of(2000, 12, 25))
      assertThat(updatedContact.updatedBy).isEqualTo("updated")
    }
  }

  private fun whenUpdateIsSuccessful() {
    whenever(contactRepository.saveAndFlush(any())).thenAnswer { i -> i.arguments[0] }
  }

  private fun whenContactExists() {
    whenever(contactRepository.findById(contactId)).thenReturn(Optional.of(originalContact))
  }

  private fun createDummyContactEntity(languageCode: String? = this.languageCode, domesticStatus: String? = "M") = ContactEntity(
    contactId = 1L,
    title = "Mr.",
    firstName = "John",
    lastName = "Doe",
    middleNames = "A",
    dateOfBirth = LocalDate.of(1990, 1, 1),
    deceasedDate = null,
    createdBy = "Admin",
    createdTime = LocalDateTime.of(2024, 1, 22, 0, 0, 0),
    staffFlag = false,
    gender = "M",
    domesticStatus = domesticStatus,
    languageCode = languageCode,
    interpreterRequired = false,
    updatedBy = "admin",
    updatedTime = LocalDateTime.of(2024, 1, 22, 0, 0, 0),
  )

  private fun assertUnchangedFields(updatedContact: PatchContactResponse) {
    assertThat(updatedContact.titleCode).isEqualTo(originalContact.title)
    assertThat(updatedContact.firstName).isEqualTo(originalContact.firstName)
    assertThat(updatedContact.lastName).isEqualTo(originalContact.lastName)
    assertThat(updatedContact.middleNames).isEqualTo(originalContact.middleNames)
    assertThat(updatedContact.dateOfBirth).isEqualTo(originalContact.dateOfBirth)
    assertThat(updatedContact.isStaff).isEqualTo(originalContact.staffFlag)
    assertThat(updatedContact.genderCode).isEqualTo(originalContact.gender)
    assertThat(updatedContact.interpreterRequired).isEqualTo(originalContact.interpreterRequired)
    assertThat(updatedContact.domesticStatusCode).isEqualTo(originalContact.domesticStatus)
    assertThat(updatedContact.updatedTime).isAfter(originalContact.updatedTime)
    assertThat(updatedContact.languageCode).isEqualTo(originalContact.languageCode)
  }

  @Nested
  inner class Gender {

    @Test
    fun `should patch when gender is null`() {
      originalContact = createDummyContactEntity().copy(gender = "NS")

      val patchRequest = PatchContactRequest(
        genderCode = JsonNullable.of(null),
      )

      whenContactExists()
      whenUpdateIsSuccessful()

      val updatedContact = service.patch(contactId, patchRequest, user)

      assertThat(updatedContact.genderCode).isEqualTo(null)
      assertThat(updatedContact.updatedBy).isEqualTo("updated")
    }

    @Test
    fun `should patch without validating undefined gender`() {
      originalContact = createDummyContactEntity().copy(gender = "NS")

      val patchRequest = PatchContactRequest()

      whenContactExists()
      whenUpdateIsSuccessful()

      val patched = service.patch(contactId, patchRequest, user)

      assertThat(patched.genderCode).isEqualTo("NS")
      verify(referenceCodeService, never()).validateReferenceCode(any(), any(), any())
      verify(contactRepository, times(1)).saveAndFlush(any())
    }

    @Test
    fun `should patch gender with a valid code`() {
      originalContact = createDummyContactEntity().copy(gender = null)

      val patchRequest = PatchContactRequest(
        genderCode = JsonNullable.of("NS"),
      )

      whenContactExists()
      whenUpdateIsSuccessful()
      whenever(referenceCodeService.validateReferenceCode(ReferenceCodeGroup.GENDER, "NS", allowInactive = true)).thenReturn(
        ReferenceCode(1, ReferenceCodeGroup.GENDER, "NS", "Not specified", 99, true),
      )

      val updatedContact = service.patch(contactId, patchRequest, user)

      val contactCaptor = argumentCaptor<ContactEntity>()

      verify(contactRepository).saveAndFlush(contactCaptor.capture())
      verify(referenceCodeService, times(1)).validateReferenceCode(ReferenceCodeGroup.GENDER, "NS", allowInactive = true)

      assertThat(updatedContact.genderCode).isEqualTo("NS")
      assertThat(updatedContact.updatedBy).isEqualTo("updated")
    }

    @Test
    fun `should reject gender when code is invalid`() {
      originalContact = createDummyContactEntity().copy(gender = null)

      val patchRequest = PatchContactRequest(
        genderCode = JsonNullable.of("NS"),
      )
      val expectedException = ValidationException("Invalid")
      whenContactExists()
      whenUpdateIsSuccessful()
      whenever(referenceCodeService.validateReferenceCode(ReferenceCodeGroup.GENDER, "NS", allowInactive = true)).thenThrow(expectedException)

      val exception = assertThrows<ValidationException> {
        service.patch(contactId, patchRequest, user)
      }
      assertThat(exception).isEqualTo(expectedException)
    }
  }
}
