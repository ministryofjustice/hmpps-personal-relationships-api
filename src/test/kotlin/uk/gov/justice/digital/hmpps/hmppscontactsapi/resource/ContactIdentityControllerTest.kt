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
import uk.gov.justice.digital.hmpps.hmppscontactsapi.facade.ContactIdentityFacade
import uk.gov.justice.digital.hmpps.hmppscontactsapi.helpers.createContactIdentityDetails
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.identity.CreateIdentityRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.identity.UpdateIdentityRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.ContactIdentityDetails
import java.time.LocalDateTime

class ContactIdentityControllerTest {

  private val facade: ContactIdentityFacade = mock()
  private val controller = ContactIdentityController(facade)

  @Nested
  inner class CreateContactIdentity {
    @Test
    fun `should return 201 with created identity if created successfully`() {
      val createdIdentity = createContactIdentityDetails(id = 99, contactId = 1)
      val request = CreateIdentityRequest(
        identityType = "DL",
        identityValue = "DL123456789",
        createdBy = "created",
      )
      whenever(facade.create(1, request)).thenReturn(createdIdentity)

      val response = controller.createIdentityNumber(1, request)

      assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
      assertThat(response.body).isEqualTo(createdIdentity)
      verify(facade).create(1, request)
    }

    @Test
    fun `should propagate exceptions if create fails`() {
      val request = CreateIdentityRequest(
        identityType = "DL",
        identityValue = "DL123456789",
        createdBy = "created",
      )
      val expected = EntityNotFoundException("Couldn't find contact")
      whenever(facade.create(1, request)).thenThrow(expected)

      val exception = assertThrows<EntityNotFoundException> {
        controller.createIdentityNumber(1, request)
      }

      assertThat(exception).isEqualTo(expected)
      verify(facade).create(1, request)
    }
  }

  @Nested
  inner class UpdateContactIdentity {
    @Test
    fun `should return 200 with updated identity if updated successfully`() {
      val updatedIdentity = createContactIdentityDetails(id = 2, contactId = 1)
      val request = UpdateIdentityRequest(
        "MOB",
        "+07777777777",
        null,
        "JAMES",
      )
      whenever(facade.update(1, 2, request)).thenReturn(updatedIdentity)

      val response = controller.updateIdentityNumber(1, 2, request)

      assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
      assertThat(response.body).isEqualTo(updatedIdentity)
      verify(facade).update(1, 2, request)
    }

    @Test
    fun `should propagate exceptions if update fails`() {
      val request = UpdateIdentityRequest(
        "MOB",
        "+07777777777",
        null,
        "JAMES",
      )
      val expected = EntityNotFoundException("Couldn't find contact")
      whenever(facade.update(1, 2, request)).thenThrow(expected)

      val exception = assertThrows<EntityNotFoundException> {
        controller.updateIdentityNumber(1, 2, request)
      }

      assertThat(exception).isEqualTo(expected)
      verify(facade).update(1, 2, request)
    }
  }

  @Nested
  inner class GetIdentity {
    private val identity = ContactIdentityDetails(
      contactIdentityId = 99,
      contactId = 11,
      identityType = "DL",
      identityTypeDescription = "Driving licence",
      identityTypeIsActive = true,
      identityValue = "DL123456789",
      issuingAuthority = null,
      createdBy = "USER1",
      createdTime = LocalDateTime.now(),
      updatedBy = null,
      updatedTime = null,
    )

    @Test
    fun `get identity if found by ids`() {
      whenever(facade.get(11, 99)).thenReturn(identity)

      val returnedIdentity = facade.get(11, 99)

      assertThat(returnedIdentity).isEqualTo(identity)
    }

    @Test
    fun `propagate exception getting identity`() {
      val expected = EntityNotFoundException("Bang!")
      whenever(facade.get(11, 99)).thenThrow(expected)
      val exception = assertThrows<EntityNotFoundException> {
        controller.getIdentityNumber(11, 99)
      }
      assertThat(exception).isEqualTo(expected)
    }
  }

  @Nested
  inner class DeleteContactIdentity {
    @Test
    fun `should return 204 if deleted successfully`() {
      whenever(facade.delete(1, 2)).then { }

      val response = controller.deleteIdentityNumber(1, 2)

      assertThat(response.statusCode).isEqualTo(HttpStatus.NO_CONTENT)
      verify(facade).delete(1, 2)
    }

    @Test
    fun `should propagate exceptions if delete fails`() {
      val expected = EntityNotFoundException("Couldn't find contact")
      whenever(facade.delete(1, 2)).thenThrow(expected)

      val exception = assertThrows<EntityNotFoundException> {
        controller.deleteIdentityNumber(1, 2)
      }

      assertThat(exception).isEqualTo(expected)
      verify(facade).delete(1, 2)
    }
  }
}
