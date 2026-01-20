package uk.gov.justice.digital.hmpps.personalrelationships.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.personalrelationships.facade.ContactGlobalRestrictionsFacade
import uk.gov.justice.digital.hmpps.personalrelationships.helpers.aUser
import uk.gov.justice.digital.hmpps.personalrelationships.helpers.createContactRestrictionDetails
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.restrictions.CreateContactRestrictionRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.restrictions.UpdateContactRestrictionRequest
import java.time.LocalDate

class ContactGlobalRestrictionControllerTest {
  private val facade: ContactGlobalRestrictionsFacade = mock()
  private val controller = ContactGlobalRestrictionController(facade)
  private val user = aUser()

  @Nested
  inner class GetContactRestrictions {
    @Test
    fun `should get restrictions`() {
      val expected = listOf(createContactRestrictionDetails())
      whenever(facade.getGlobalRestrictionsForContact(9)).thenReturn(expected)

      val response = controller.getContactGlobalRestrictions(9)

      assertThat(response).isEqualTo(expected)
    }

    @Test
    fun `should propagate exceptions getting a restriction`() {
      val expected = RuntimeException("Bang!")
      whenever(facade.getGlobalRestrictionsForContact(9)).thenThrow(expected)

      val result = assertThrows<RuntimeException> {
        controller.getContactGlobalRestrictions(9)
      }
      assertThat(result).isEqualTo(expected)
    }
  }

  @Nested
  inner class CreateContactRestrictions {
    private val request = CreateContactRestrictionRequest(
      restrictionType = "BAN",
      startDate = LocalDate.of(2020, 1, 1),
      expiryDate = LocalDate.of(2022, 2, 2),
      comments = "Some comments",
    )

    @Test
    fun `should create restrictions`() {
      val expected = createContactRestrictionDetails()
      whenever(facade.createContactGlobalRestriction(9, request, user)).thenReturn(expected)

      val response = controller.createContactGlobalRestriction(9, request, user)

      assertThat(response.body).isEqualTo(expected)
      verify(facade).createContactGlobalRestriction(9, request, user)
    }

    @Test
    fun `should propagate exceptions creating a restriction`() {
      val expected = RuntimeException("Bang!")
      whenever(facade.createContactGlobalRestriction(9, request, user)).thenThrow(expected)

      val result = assertThrows<RuntimeException> {
        controller.createContactGlobalRestriction(9, request, user)
      }
      assertThat(result).isEqualTo(expected)
    }
  }

  @Nested
  inner class UpdateContactRestrictions {
    private val contactRestrictionId = 564L
    private val request = UpdateContactRestrictionRequest(
      restrictionType = "BAN",
      startDate = LocalDate.of(2020, 1, 1),
      expiryDate = LocalDate.of(2022, 2, 2),
      comments = "Some comments",
    )

    @Test
    fun `should update restrictions`() {
      val expected = createContactRestrictionDetails()
      whenever(facade.updateContactGlobalRestriction(9, contactRestrictionId, request, user)).thenReturn(expected)

      val response = controller.updateContactGlobalRestriction(9, contactRestrictionId, request, user)

      assertThat(response).isEqualTo(expected)
      verify(facade).updateContactGlobalRestriction(9, contactRestrictionId, request, user)
    }

    @Test
    fun `should propagate exceptions updating a restriction`() {
      val expected = RuntimeException("Bang!")
      whenever(facade.updateContactGlobalRestriction(9, contactRestrictionId, request, user)).thenThrow(expected)

      val result = assertThrows<RuntimeException> {
        controller.updateContactGlobalRestriction(9, contactRestrictionId, request, user)
      }
      assertThat(result).isEqualTo(expected)
    }
  }
}
