package uk.gov.justice.digital.hmpps.personalrelationships.resource

import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.personalrelationships.facade.PrisonerNumberOfChildrenFacade
import uk.gov.justice.digital.hmpps.personalrelationships.helpers.aUser
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.CreateOrUpdatePrisonerNumberOfChildrenRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.PrisonerNumberOfChildrenResponse
@ExtendWith(MockitoExtension::class)
class PrisonerNumberOfChildrenControllerTest {
  @Mock
  private lateinit var prisonerNumberOfChildrenFacade: PrisonerNumberOfChildrenFacade

  @InjectMocks
  private lateinit var controller: PrisonerNumberOfChildrenController

  @Nested
  inner class GetNumberOfChildren {

    @Test
    fun `should return number of children when prisoner number of children exists`() {
      // Given
      val prisonerNumber = "A1234BC"
      val expectedResponse = PrisonerNumberOfChildrenResponse(
        id = 1L,
        numberOfChildren = "1",
        active = true,
      )
      whenever(prisonerNumberOfChildrenFacade.getNumberOfChildren(prisonerNumber)).thenReturn(expectedResponse)

      // When
      val result = controller.getNumberOfChildren(prisonerNumber)

      // Then
      assertThat(result).isEqualTo(expectedResponse)
      verify(prisonerNumberOfChildrenFacade).getNumberOfChildren(prisonerNumber)
    }

    @Test
    fun `should throw exception when prisoner number of children not found`() {
      // Given
      val prisonerNumber = "A1234BC"
      whenever(prisonerNumberOfChildrenFacade.getNumberOfChildren(prisonerNumber)).thenThrow(EntityNotFoundException("Prisoner number of children not found"))

      // When/Then
      assertThrows<EntityNotFoundException> {
        controller.getNumberOfChildren(prisonerNumber)
      }
      verify(prisonerNumberOfChildrenFacade).getNumberOfChildren(prisonerNumber)
    }
  }

  @Nested
  inner class CreateOrUpdateNumberOfChildren {

    private val user = aUser("test-user")

    @Test
    fun `should create or update number of children successfully`() {
      // Given
      val prisonerNumber = "A1234BC"
      val request = CreateOrUpdatePrisonerNumberOfChildrenRequest(
        numberOfChildren = 1,
      )
      val expectedResponse = PrisonerNumberOfChildrenResponse(
        id = 1L,
        numberOfChildren = "1",
        active = true,
      )
      whenever(
        prisonerNumberOfChildrenFacade.createOrUpdateNumberOfChildren(prisonerNumber, request, user),
      ).thenReturn(expectedResponse)

      // When
      val result = controller.createOrUpdateNumberOfChildren(prisonerNumber, request, user)

      // Then
      assertThat(result).isEqualTo(expectedResponse)
      verify(prisonerNumberOfChildrenFacade).createOrUpdateNumberOfChildren(prisonerNumber, request, user)
    }

    @Test
    fun `should throw exception when prisoner number of children not found`() {
      // Given
      val prisonerNumber = "A1234BC"
      val request = CreateOrUpdatePrisonerNumberOfChildrenRequest(
        numberOfChildren = 1,
      )
      whenever(
        prisonerNumberOfChildrenFacade.createOrUpdateNumberOfChildren(prisonerNumber, request, user),
      ).thenThrow(EntityNotFoundException("Prisoner not found"))

      // When/Then
      assertThrows<EntityNotFoundException> {
        controller.createOrUpdateNumberOfChildren(prisonerNumber, request, user)
      }
      verify(prisonerNumberOfChildrenFacade).createOrUpdateNumberOfChildren(prisonerNumber, request, user)
    }
  }
}
