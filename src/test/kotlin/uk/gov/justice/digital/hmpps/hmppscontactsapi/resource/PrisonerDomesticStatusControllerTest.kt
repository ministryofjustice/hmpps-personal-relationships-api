package uk.gov.justice.digital.hmpps.hmppscontactsapi.resource

import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppscontactsapi.facade.PrisonerDomesticStatusFacade
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.UpdatePrisonerDomesticStatusRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.PrisonerDomesticStatusResponse

class PrisonerDomesticStatusControllerTest {

  private val prisonerDomesticStatusFacade: PrisonerDomesticStatusFacade = mock()
  private lateinit var controller: PrisonerDomesticStatusController

  @BeforeEach
  fun setUp() {
    controller = PrisonerDomesticStatusController(prisonerDomesticStatusFacade)
  }

  @Nested
  inner class GetDomesticStatus {


    @Test
    fun `should return domestic status when prisoner domestic status exists`() {
      // Given
      val prisonerNumber = "A1234BC"
      val expectedResponse = PrisonerDomesticStatusResponse(
        id = 1L,
        prisonerNumber = prisonerNumber,
        domesticStatusValue = "SINGLE",
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
      val request = UpdatePrisonerDomesticStatusRequest(
        domesticStatusCode = "MARRIED",
        prisonerNumber = prisonerNumber,
        updatedBy = "test-user",
      )
      val expectedResponse = PrisonerDomesticStatusResponse(
        id = 1L,
        prisonerNumber = prisonerNumber,
        domesticStatusValue = "SINGLE",
        active = true,
      )
      whenever(
        prisonerDomesticStatusFacade.createOrUpdateDomesticStatus(prisonerNumber, request),
      ).thenReturn(expectedResponse)

      // When
      val result = controller.createOrUpdateDomesticStatus(prisonerNumber, request)

      // Then
      assertThat(result).isEqualTo(expectedResponse)
      verify(prisonerDomesticStatusFacade).createOrUpdateDomesticStatus(prisonerNumber, request)
    }

    @Test
    fun `should throw exception when prisoner domestic status not found`() {
      // Given
      val prisonerNumber = "A1234BC"
      val request = UpdatePrisonerDomesticStatusRequest(
        domesticStatusCode = "MARRIED",
        prisonerNumber = prisonerNumber,
        updatedBy = "test-user",
      )
      whenever(
        prisonerDomesticStatusFacade.createOrUpdateDomesticStatus(prisonerNumber, request),
      ).thenThrow(EntityNotFoundException("Prisoner not found"))

      // When/Then
      assertThrows<EntityNotFoundException> {
        controller.createOrUpdateDomesticStatus(prisonerNumber, request)
      }
      verify(prisonerDomesticStatusFacade).createOrUpdateDomesticStatus(prisonerNumber, request)
    }
  }
}
