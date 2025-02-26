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
import org.mockito.kotlin.never
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.ContactEmailEntity
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.ContactEntity
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.email.CreateEmailRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.email.UpdateEmailRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.ContactEmailDetails
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.ContactEmailRepository
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.ContactRepository
import java.time.LocalDateTime.now
import java.util.*

class ContactEmailServiceTest {

  private val contactRepository: ContactRepository = mock()
  private val contactEmailRepository: ContactEmailRepository = mock()
  private val service = ContactEmailService(contactRepository, contactEmailRepository)

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

  @Nested
  inner class CreateEmail {
    private val request = CreateEmailRequest(
      emailAddress = "test@example.com",
      createdBy = "created",
    )

    @Test
    fun `should throw EntityNotFoundException creating email if contact doesn't exist`() {
      whenever(contactRepository.findById(contactId)).thenReturn(Optional.empty())

      val exception = assertThrows<EntityNotFoundException> {
        service.create(contactId, request)
      }
      assertThat(exception.message).isEqualTo("Contact (99) not found")
    }

    @ParameterizedTest
    @MethodSource("uk.gov.justice.digital.hmpps.hmppscontactsapi.service.ContactEmailServiceTest#allInvalidEmailAddresses")
    fun `should not update email address if it's invalid`(case: String, invalidEmail: String) {
      whenever(contactRepository.findById(contactId)).thenReturn(Optional.of(aContact))
      val exception = assertThrows<ValidationException> {
        service.create(contactId, request.copy(emailAddress = invalidEmail))
      }
      assertThat(exception.message).isEqualTo("Email address is invalid")
      verify(contactEmailRepository, never()).saveAndFlush(any())
    }

    @Test
    fun `should return email details after creating successfully`() {
      whenever(contactRepository.findById(contactId)).thenReturn(Optional.of(aContact))
      whenever(contactEmailRepository.saveAndFlush(any())).thenAnswer { i ->
        (i.arguments[0] as ContactEmailEntity).copy(
          contactEmailId = 9999,
        )
      }

      val created = service.create(contactId, request)
      assertThat(created.createdTime).isNotNull()
      assertThat(created).isEqualTo(
        ContactEmailDetails(
          contactEmailId = 9999,
          contactId = contactId,
          emailAddress = "test@example.com",
          createdBy = "created",
          createdTime = created.createdTime,
          updatedBy = null,
          updatedTime = null,
        ),
      )
    }
  }

  @Nested
  inner class UpdateEmail {
    private val request = UpdateEmailRequest(
      emailAddress = "updated@example.com",
      updatedBy = "updated",
    )
    private val contactEmailId = 1234L
    private val existingEmail = ContactEmailEntity(
      contactEmailId = contactEmailId,
      contactId = contactId,
      emailAddress = "test@example.com",
      createdBy = "USER99",
      createdTime = now().minusDays(2),
      updatedBy = null,
      updatedTime = null,
    )

    @Test
    fun `should throw EntityNotFoundException updating email if contact doesn't exist`() {
      whenever(contactRepository.findById(contactId)).thenReturn(Optional.empty())

      val exception = assertThrows<EntityNotFoundException> {
        service.update(contactId, contactEmailId, request)
      }
      assertThat(exception.message).isEqualTo("Contact (99) not found")
    }

    @Test
    fun `should throw EntityNotFoundException updating email if email doesn't exist`() {
      whenever(contactRepository.findById(contactId)).thenReturn(Optional.of(aContact))
      whenever(contactEmailRepository.findById(contactEmailId)).thenReturn(Optional.empty())

      val exception = assertThrows<EntityNotFoundException> {
        service.update(contactId, contactEmailId, request)
      }
      assertThat(exception.message).isEqualTo("Contact email (1234) not found")
    }

    @ParameterizedTest
    @MethodSource("uk.gov.justice.digital.hmpps.hmppscontactsapi.service.ContactEmailServiceTest#allInvalidEmailAddresses")
    fun `should not update email address if it's invalid`(case: String, invalidEmail: String) {
      whenever(contactRepository.findById(contactId)).thenReturn(Optional.of(aContact))
      whenever(contactEmailRepository.findById(contactEmailId)).thenReturn(Optional.of(existingEmail))

      val exception = assertThrows<ValidationException> {
        service.update(contactId, contactEmailId, request.copy(emailAddress = invalidEmail))
      }
      assertThat(exception.message).isEqualTo("Email address is invalid")
      verify(contactEmailRepository, never()).saveAndFlush(any())
    }

    @Test
    fun `should return a email details after updating a email successfully`() {
      whenever(contactRepository.findById(contactId)).thenReturn(Optional.of(aContact))
      whenever(contactEmailRepository.findById(contactEmailId)).thenReturn(Optional.of(existingEmail))
      whenever(contactEmailRepository.saveAndFlush(any())).thenAnswer { i ->
        (i.arguments[0] as ContactEmailEntity).copy(
          contactEmailId = 9999,
        )
      }

      val updated = service.update(contactId, contactEmailId, request)
      assertThat(updated.updatedTime).isNotNull()
      assertThat(updated).isEqualTo(
        ContactEmailDetails(
          contactEmailId = 9999,
          contactId = contactId,
          emailAddress = "updated@example.com",
          createdBy = "USER99",
          createdTime = existingEmail.createdTime,
          updatedBy = "updated",
          updatedTime = updated.updatedTime,
        ),
      )
    }
  }

  @Nested
  inner class GetEmail {
    private val createdTime = now()
    private val entity = ContactEmailEntity(
      contactEmailId = 99,
      contactId = contactId,
      emailAddress = "test@example.com",
      createdBy = "USER1",
      createdTime = createdTime,
      updatedBy = null,
      updatedTime = null,
    )

    @Test
    fun `get email if found by ids`() {
      whenever(contactEmailRepository.findByContactIdAndContactEmailId(contactId, 99)).thenReturn(entity)

      val returned = service.get(contactId, 99)

      assertThat(returned).isEqualTo(
        ContactEmailDetails(
          contactEmailId = 99,
          contactId = contactId,
          emailAddress = "test@example.com",
          createdBy = "USER1",
          createdTime = createdTime,
          updatedBy = null,
          updatedTime = null,
        ),
      )
    }

    @Test
    fun `return null if not found`() {
      whenever(contactEmailRepository.findByContactIdAndContactEmailId(contactId, 99)).thenReturn(null)

      assertThat(service.get(contactId, 99)).isNull()
    }
  }

  @Nested
  inner class DeleteEmail {
    private val contactEmailId = 1234L
    private val existingEmail = ContactEmailEntity(
      contactEmailId = contactEmailId,
      contactId = contactId,
      emailAddress = "test@example.com",
      createdBy = "USER99",
      createdTime = now().minusDays(2),
      updatedBy = null,
      updatedTime = null,
    )

    @Test
    fun `should throw EntityNotFoundException deleting email if contact doesn't exist`() {
      whenever(contactRepository.findById(contactId)).thenReturn(Optional.empty())

      val exception = assertThrows<EntityNotFoundException> {
        service.delete(contactId, contactEmailId)
      }
      assertThat(exception.message).isEqualTo("Contact (99) not found")
    }

    @Test
    fun `should throw EntityNotFoundException deleting email if email doesn't exist`() {
      whenever(contactRepository.findById(contactId)).thenReturn(Optional.of(aContact))
      whenever(contactEmailRepository.findById(contactEmailId)).thenReturn(Optional.empty())

      val exception = assertThrows<EntityNotFoundException> {
        service.delete(contactId, contactEmailId)
      }
      assertThat(exception.message).isEqualTo("Contact email (1234) not found")
    }

    @Test
    fun `should just delete the email and any address links if it exists`() {
      whenever(contactRepository.findById(contactId)).thenReturn(Optional.of(aContact))
      whenever(contactEmailRepository.findById(contactEmailId)).thenReturn(Optional.of(existingEmail))
      whenever(contactEmailRepository.delete(any())).then {}

      service.delete(contactId, contactEmailId)

      verify(contactEmailRepository).delete(existingEmail)
    }
  }

  companion object {
    @JvmStatic
    fun allInvalidEmailAddresses(): List<Arguments> = listOf(
      Arguments.of("Must have something before the @", "@example.com"),
      Arguments.of("Must have something after the @", "test@"),
      Arguments.of("Must have something between the @ and dot", "test@."),
      Arguments.of("Must have something after the .", "test@a."),
      Arguments.of("Must not have an @ after @ before .", "test@a@a.a"),
      Arguments.of("Must not have an @ after .", "test@a.@a"),
      Arguments.of("Must match whole string to check for more @", "test@a.a@"),
    )
  }
}
