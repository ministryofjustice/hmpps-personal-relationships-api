package uk.gov.justice.digital.hmpps.personalrelationships.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageImpl
import org.springframework.data.web.PagedModel
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.LinkedPrisonerDetails
import uk.gov.justice.digital.hmpps.personalrelationships.service.LinkedPrisonersService

class ContactLinkedPrisonersControllerTest {

  private val service: LinkedPrisonersService = mock()
  private val controller = ContactLinkedPrisonersController(service)

  @Test
  fun `should return linked prisoners`() {
    val expected = listOf(
      LinkedPrisonerDetails(
        prisonerNumber = "A1234BC",
        firstName = "Joe",
        middleNames = "Middle",
        lastName = "Bloggs",
        prisonerContactId = 99,
        relationshipTypeCode = "S",
        relationshipTypeDescription = "Social",
        relationshipToPrisonerCode = "FA",
        relationshipToPrisonerDescription = "Father",
        isRelationshipActive = true,
      ),
    )
    whenever(service.getLinkedPrisoners(123, 0, 10)).thenReturn(PagedModel(PageImpl(expected)))
    val response = controller.getContactLinkedPrisoners(123, 0, 10)
    assertThat(response.content).isEqualTo(expected)
  }
}
