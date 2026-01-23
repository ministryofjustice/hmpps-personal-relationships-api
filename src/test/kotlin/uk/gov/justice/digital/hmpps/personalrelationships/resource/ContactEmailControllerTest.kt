package uk.gov.justice.digital.hmpps.personalrelationships.resource

import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.personalrelationships.facade.ContactEmailFacade
import uk.gov.justice.digital.hmpps.personalrelationships.helpers.aUser
import uk.gov.justice.digital.hmpps.personalrelationships.helpers.createContactEmailDetails
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.email.CreateEmailRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.email.UpdateEmailRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.ContactEmailDetails
import java.time.LocalDateTime

class ContactEmailControllerTest {

  private val facade: ContactEmailFacade = mock()
  private val controller = ContactEmailController(facade)
  private val user = aUser()

  @Nested
  inner class CreateContactEmail {
    @Test
    fun `should return 201 with created email if created successfully`() {
      val createdEmail = createContactEmailDetails(id = 99, contactId = 1)
      val request = CreateEmailRequest(
        emailAddress = "test@example.com",
      )
      whenever(facade.create(1, request, user)).thenReturn(createdEmail)

      val response = controller.createEmailAddress(1, request, user)

      assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
      assertThat(response.body).isEqualTo(createdEmail)
      verify(facade).create(1, request, user)
    }

    @Test
    fun `should propagate exceptions if create fails`() {
      val request = CreateEmailRequest(
        emailAddress = "test@example.com",
      )
      val expected = EntityNotFoundException("Couldn't find contact")
      whenever(facade.create(1, request, user)).thenThrow(expected)

      val exception = assertThrows<EntityNotFoundException> {
        controller.createEmailAddress(1, request, user)
      }

      assertThat(exception).isEqualTo(expected)
      verify(facade).create(1, request, user)
    }
  }

  @Nested
  inner class UpdateContactEmail {
    @Test
    fun `should return 200 with updated email if updated successfully`() {
      val updatedEmail = createContactEmailDetails(id = 2, contactId = 1)
      val request = UpdateEmailRequest(
        emailAddress = "test@example.com",
      )
      whenever(facade.update(1, 2, request, user)).thenReturn(updatedEmail)

      val response = controller.updateEmailAddress(1, 2, request, user)

      assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
      assertThat(response.body).isEqualTo(updatedEmail)
      verify(facade).update(1, 2, request, user)
    }

    @Test
    fun `should propagate exceptions if update fails`() {
      val request = UpdateEmailRequest(
        emailAddress = "test@example.com",
      )
      val expected = EntityNotFoundException("Couldn't find contact")
      whenever(facade.update(1, 2, request, user)).thenThrow(expected)

      val exception = assertThrows<EntityNotFoundException> {
        controller.updateEmailAddress(1, 2, request, user)
      }

      assertThat(exception).isEqualTo(expected)
      verify(facade).update(1, 2, request, user)
    }
  }

  @Nested
  inner class GetEmail {
    private val email = ContactEmailDetails(
      contactEmailId = 99,
      contactId = 11,
      emailAddress = "test@example.com",
      createdBy = "USER1",
      createdTime = LocalDateTime.now(),
      updatedBy = null,
      updatedTime = null,
    )

    @Test
    fun `get email if found by ids`() {
      whenever(facade.get(11, 99)).thenReturn(email)

      val returnedEmail = facade.get(11, 99)

      assertThat(returnedEmail).isEqualTo(email)
    }

    @Test
    fun `propagate exception getting email`() {
      val expected = EntityNotFoundException("Bang!")
      whenever(facade.get(11, 99)).thenThrow(expected)
      val exception = assertThrows<EntityNotFoundException> {
        controller.getEmailAddress(11, 99)
      }
      assertThat(exception).isEqualTo(expected)
    }
  }

  @Nested
  inner class DeleteContactEmail {
    @Test
    fun `should return 204 if deleted successfully`() {
      whenever(facade.delete(1, 2, user)).then { }

      val response = controller.deleteEmailAddress(1, 2, user)

      assertThat(response.statusCode).isEqualTo(HttpStatus.NO_CONTENT)
      verify(facade).delete(1, 2, user)
    }

    @Test
    fun `should propagate exceptions if delete fails`() {
      val expected = EntityNotFoundException("Couldn't find contact")
      whenever(facade.delete(1, 2, user)).thenThrow(expected)

      val exception = assertThrows<EntityNotFoundException> {
        controller.deleteEmailAddress(1, 2, user)
      }

      assertThat(exception).isEqualTo(expected)
      verify(facade).delete(1, 2, user)
    }
  }
}
