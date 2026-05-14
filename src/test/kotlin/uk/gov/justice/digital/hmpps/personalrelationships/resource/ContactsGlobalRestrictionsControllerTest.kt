package uk.gov.justice.digital.hmpps.personalrelationships.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.personalrelationships.facade.ContactGlobalRestrictionsFacade
import uk.gov.justice.digital.hmpps.personalrelationships.helpers.createContactRestrictionDetails
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.ContactIdsRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.ContactRestrictions
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.ContactsRestrictionsResponse

class ContactsGlobalRestrictionsControllerTest {
  private val restrictionsFacade: ContactGlobalRestrictionsFacade = mock()
  private val controller = ContactsGlobalRestrictionsController(restrictionsFacade)

  @Nested
  inner class GetContactGlobalRestrictionsByContactIds {
    @Test
    fun `should get contact global restrictions successfully`() {
      val request = ContactIdsRequest(
        contactIds = listOf(1L, 2L, 3L),
      )

      val expected = ContactsRestrictionsResponse(
        contactRestrictions = listOf(
          ContactRestrictions(
            contactId = 1L,
            globalContactRestrictions = listOf(createContactRestrictionDetails(contactId = 1L)),
          ),
          ContactRestrictions(
            contactId = 2L,
            globalContactRestrictions = listOf(createContactRestrictionDetails(contactId = 2L)),
          ),
          ContactRestrictions(
            contactId = 3L,
            globalContactRestrictions = emptyList(),
          ),
        ),
      )

      whenever(
        restrictionsFacade.getGlobalRestrictionsForContacts(setOf(1L, 2L, 3L)),
      ).thenReturn(expected)

      val result = controller.getContactGlobalRestrictionsByContactIds(request)

      assertThat(result).isEqualTo(expected)
      verify(restrictionsFacade).getGlobalRestrictionsForContacts(setOf(1L, 2L, 3L))
    }

    @Test
    fun `should propagate exceptions getting contact global restrictions`() {
      val request = ContactIdsRequest(
        contactIds = listOf(1L, 2L, 3L),
      )

      val expected = RuntimeException("Bang!")

      whenever(
        restrictionsFacade.getGlobalRestrictionsForContacts(setOf(1L, 2L, 3L)),
      ).thenThrow(expected)

      val result = assertThrows<RuntimeException> {
        controller.getContactGlobalRestrictionsByContactIds(request)
      }

      assertThat(result).isEqualTo(expected)
      verify(restrictionsFacade).getGlobalRestrictionsForContacts(setOf(1L, 2L, 3L))
    }
  }
}
