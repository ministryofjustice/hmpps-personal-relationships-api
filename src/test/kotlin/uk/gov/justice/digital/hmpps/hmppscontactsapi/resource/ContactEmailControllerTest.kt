package uk.gov.justice.digital.hmpps.hmppscontactsapi.resource

import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.hmppscontactsapi.facade.ContactEmailFacade
import uk.gov.justice.digital.hmpps.hmppscontactsapi.helpers.createContactEmailDetails
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.email.CreateEmailRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.email.UpdateEmailRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.ContactEmailDetails
import java.time.LocalDateTime

class ContactEmailControllerTest {

  private val facade: ContactEmailFacade = mock()
  private val controller = ContactEmailController(facade)

  @Nested
  inner class CreateContactEmail {
    @Test
    fun `should return 201 with created email if created successfully`() {
      val createdEmail = createContactEmailDetails(id = 99, contactId = 1)
      val request = CreateEmailRequest(
        emailAddress = "test@example.com",
        createdBy = "created",
      )
      whenever(facade.create(1, request)).thenReturn(createdEmail)

      val response = controller.createEmailAddress(1, request)

      assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
      assertThat(response.body).isEqualTo(createdEmail)
      verify(facade).create(1, request)
    }

    @Test
    fun `should propagate exceptions if create fails`() {
      val request = CreateEmailRequest(
        emailAddress = "test@example.com",
        createdBy = "created",
      )
      val expected = EntityNotFoundException("Couldn't find contact")
      whenever(facade.create(1, request)).thenThrow(expected)

      val exception = assertThrows<EntityNotFoundException> {
        controller.createEmailAddress(1, request)
      }

      assertThat(exception).isEqualTo(expected)
      verify(facade).create(1, request)
    }
  }

  @Nested
  inner class UpdateContactEmail {
    @Test
    fun `should return 200 with updated email if updated successfully`() {
      val updatedEmail = createContactEmailDetails(id = 2, contactId = 1)
      val request = UpdateEmailRequest(
        emailAddress = "test@example.com",
        updatedBy = "JAMES",
      )
      whenever(facade.update(1, 2, request)).thenReturn(updatedEmail)

      val response = controller.updateEmailAddress(1, 2, request)

      assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
      assertThat(response.body).isEqualTo(updatedEmail)
      verify(facade).update(1, 2, request)
    }

    @Test
    fun `should propagate exceptions if update fails`() {
      val request = UpdateEmailRequest(
        emailAddress = "test@example.com",
        updatedBy = "JAMES",
      )
      val expected = EntityNotFoundException("Couldn't find contact")
      whenever(facade.update(1, 2, request)).thenThrow(expected)

      val exception = assertThrows<EntityNotFoundException> {
        controller.updateEmailAddress(1, 2, request)
      }

      assertThat(exception).isEqualTo(expected)
      verify(facade).update(1, 2, request)
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
      whenever(facade.delete(1, 2)).then { }

      val response = controller.deleteEmailAddress(1, 2)

      assertThat(response.statusCode).isEqualTo(HttpStatus.NO_CONTENT)
      verify(facade).delete(1, 2)
    }

    @Test
    fun `should propagate exceptions if delete fails`() {
      val expected = EntityNotFoundException("Couldn't find contact")
      whenever(facade.delete(1, 2)).thenThrow(expected)

      val exception = assertThrows<EntityNotFoundException> {
        controller.deleteEmailAddress(1, 2)
      }

      assertThat(exception).isEqualTo(expected)
      verify(facade).delete(1, 2)
    }
  }
}
