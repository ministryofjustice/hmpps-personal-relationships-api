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
import uk.gov.justice.digital.hmpps.personalrelationships.facade.ContactPhoneFacade
import uk.gov.justice.digital.hmpps.personalrelationships.helpers.aUser
import uk.gov.justice.digital.hmpps.personalrelationships.helpers.createContactPhoneNumberDetails
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.phone.CreatePhoneRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.phone.UpdatePhoneRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.ContactPhoneDetails
import java.time.LocalDateTime

class ContactPhoneControllerTest {

  private val facade: ContactPhoneFacade = mock()
  private val controller = ContactPhoneController(facade)
  private val user = aUser()

  @Nested
  inner class CreateContactPhone {
    @Test
    fun `should return 201 with created phone number if created successfully`() {
      val createdPhone = createContactPhoneNumberDetails(id = 99, contactId = 1)
      val request = CreatePhoneRequest(
        "MOB",
        "+07777777777",
        null,
      )
      whenever(facade.create(1, request, user)).thenReturn(createdPhone)

      val response = controller.createPhone(1, request, user)

      assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
      assertThat(response.body).isEqualTo(createdPhone)
      verify(facade).create(1, request, user)
    }

    @Test
    fun `should propagate exceptions if create fails`() {
      val request = CreatePhoneRequest(
        "MOB",
        "+07777777777",
        null,
      )
      val expected = EntityNotFoundException("Couldn't find contact")
      whenever(facade.create(1, request, user)).thenThrow(expected)

      val exception = assertThrows<EntityNotFoundException> {
        controller.createPhone(1, request, user)
      }

      assertThat(exception).isEqualTo(expected)
      verify(facade).create(1, request, user)
    }
  }

  @Nested
  inner class GetPhone {
    private val phone = ContactPhoneDetails(
      contactPhoneId = 99,
      contactId = 11,
      phoneType = "MOB",
      phoneTypeDescription = "Mobile",
      phoneNumber = "07777777777",
      extNumber = null,
      createdBy = "USER1",
      createdTime = LocalDateTime.now(),
      updatedBy = null,
      updatedTime = null,
    )

    @Test
    fun `get phone if found by ids`() {
      whenever(facade.get(11, 99)).thenReturn(phone)

      val returnedPhone = facade.get(11, 99)

      assertThat(returnedPhone).isEqualTo(phone)
    }

    @Test
    fun `throw EntityNotFoundException when contact or phone cannot be found`() {
      whenever(facade.get(11, 99)).thenReturn(null)
      val exception = assertThrows<EntityNotFoundException> {
        controller.getPhone(11, 99)
      }
      assertThat(exception.message).isEqualTo("Contact phone with id (99) not found for contact (11)")
    }
  }

  @Nested
  inner class UpdateContactPhone {
    @Test
    fun `should return 200 with updated phone number if updated successfully`() {
      val updatedPhone = createContactPhoneNumberDetails(id = 2, contactId = 1)
      val request = UpdatePhoneRequest(
        "MOB",
        "+07777777777",
        null,
      )
      whenever(facade.update(1, 2, request, user)).thenReturn(updatedPhone)

      val response = controller.updatePhone(1, 2, request, user)

      assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
      assertThat(response.body).isEqualTo(updatedPhone)
      verify(facade).update(1, 2, request, user)
    }

    @Test
    fun `should propagate exceptions if update fails`() {
      val request = UpdatePhoneRequest(
        "MOB",
        "+07777777777",
        null,
      )
      val expected = EntityNotFoundException("Couldn't find contact")
      whenever(facade.update(1, 2, request, user)).thenThrow(expected)

      val exception = assertThrows<EntityNotFoundException> {
        controller.updatePhone(1, 2, request, user)
      }

      assertThat(exception).isEqualTo(expected)
      verify(facade).update(1, 2, request, user)
    }
  }

  @Nested
  inner class DeleteContactPhone {
    @Test
    fun `should return 204 if deleted successfully`() {
      whenever(facade.delete(1, 2, user)).then { }

      val response = controller.deletePhone(1, 2, user)

      assertThat(response.statusCode).isEqualTo(HttpStatus.NO_CONTENT)
      verify(facade).delete(1, 2, user)
    }

    @Test
    fun `should propagate exceptions if delete fails`() {
      val expected = EntityNotFoundException("Couldn't find contact")
      whenever(facade.delete(1, 2, user)).thenThrow(expected)

      val exception = assertThrows<EntityNotFoundException> {
        controller.deletePhone(1, 2, user)
      }

      assertThat(exception).isEqualTo(expected)
      verify(facade).delete(1, 2, user)
    }
  }
}
