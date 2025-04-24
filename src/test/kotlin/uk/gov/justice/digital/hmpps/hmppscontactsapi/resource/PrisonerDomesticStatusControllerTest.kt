package uk.gov.justice.digital.hmpps.hmppscontactsapi.resource

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
import uk.gov.justice.digital.hmpps.hmppscontactsapi.facade.PrisonerDomesticStatusFacade
import uk.gov.justice.digital.hmpps.hmppscontactsapi.helpers.aUser
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.CreateOrUpdatePrisonerDomesticStatusRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.PrisonerDomesticStatusResponse
@ExtendWith(MockitoExtension::class)
class PrisonerDomesticStatusControllerTest {

  @Mock
  private lateinit var prisonerDomesticStatusFacade: PrisonerDomesticStatusFacade

  @InjectMocks
  private lateinit var controller: PrisonerDomesticStatusController

  private val user = aUser("test-user")

  @Nested
  inner class GetDomesticStatus {

    @Test
    fun `should return domestic status when prisoner domestic status exists`() {
      // Given
      val prisonerNumber = "A1234BC"
      val expectedResponse = PrisonerDomesticStatusResponse(
        id = 1L,
        domesticStatusCode = "SINGLE",
        active = true,
      )
      whenever(prisonerDomesticStatusFacade.getDomesticStatus(prisonerNumber)).thenReturn(expectedResponse)

      // When
      val result = controller.getDomesticStatus(prisonerNumber)

      // Then
      assertThat(result).isEqualTo(expectedResponse)
      verify(prisonerDomesticStatusFacade).getDomesticStatus(prisonerNumber)
    }

    @Test
    fun `should throw exception when prisoner domestic status not found`() {
      // Given
      val prisonerNumber = "A1234BC"
      whenever(prisonerDomesticStatusFacade.getDomesticStatus(prisonerNumber)).thenThrow(EntityNotFoundException("Prisoner domestic status not found"))

      // When/Then
      assertThrows<EntityNotFoundException> {
        controller.getDomesticStatus(prisonerNumber)
      }
      verify(prisonerDomesticStatusFacade).getDomesticStatus(prisonerNumber)
    }
  }

  @Nested
  inner class CreateOrUpdateDomesticStatus {
    @Test
    fun `should create or update domestic status successfully`() {
      // Given
      val prisonerNumber = "A1234BC"
      val request = CreateOrUpdatePrisonerDomesticStatusRequest(
        domesticStatusCode = "MARRIED",
      )
      val expectedResponse = PrisonerDomesticStatusResponse(
        id = 1L,
        domesticStatusCode = "SINGLE",
        active = true,
      )
      whenever(
        prisonerDomesticStatusFacade.createOrUpdateDomesticStatus(prisonerNumber, request, user),
      ).thenReturn(expectedResponse)

      // When
      val result = controller.createOrUpdateDomesticStatus(prisonerNumber, request, user)

      // Then
      assertThat(result).isEqualTo(expectedResponse)
      verify(prisonerDomesticStatusFacade).createOrUpdateDomesticStatus(prisonerNumber, request, user)
    }

    @Test
    fun `should throw exception when prisoner domestic status not found`() {
      // Given
      val prisonerNumber = "A1234BC"
      val request = CreateOrUpdatePrisonerDomesticStatusRequest(
        domesticStatusCode = "MARRIED",
      )
      whenever(
        prisonerDomesticStatusFacade.createOrUpdateDomesticStatus(prisonerNumber, request, user),
      ).thenThrow(EntityNotFoundException("Prisoner not found"))

      // When/Then
      assertThrows<EntityNotFoundException> {
        controller.createOrUpdateDomesticStatus(prisonerNumber, request, user)
      }
      verify(prisonerDomesticStatusFacade).createOrUpdateDomesticStatus(prisonerNumber, request, user)
    }
  }
}
