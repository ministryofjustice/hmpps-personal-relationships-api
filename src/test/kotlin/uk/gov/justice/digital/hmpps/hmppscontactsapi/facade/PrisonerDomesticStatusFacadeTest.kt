package uk.gov.justice.digital.hmpps.hmppscontactsapi.facade

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppscontactsapi.helpers.aUser
import uk.gov.justice.digital.hmpps.hmppscontactsapi.helpers.isEqualTo
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.CreateOrUpdatePrisonerDomesticStatusRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.PrisonerDomesticStatusResponse
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.PrisonerDomesticStatusService
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.OutboundEventsService
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.Source

@ExtendWith(MockitoExtension::class)
class PrisonerDomesticStatusFacadeTest {

  @Mock
  private lateinit var prisonerDomesticStatusService: PrisonerDomesticStatusService

  @Mock
  private lateinit var outboundEventsService: OutboundEventsService

  @InjectMocks
  private lateinit var prisonerDomesticStatusFacade: PrisonerDomesticStatusFacade

  @Nested
  inner class GetDomesticStatus {

    @Test
    fun `should return domestic status`() {
      val prisonerNumber = "A1234BC"
      val response = PrisonerDomesticStatusResponse(
        id = 1L,
        domesticStatusCode = "SINGLE",
        active = true,
      )

      whenever(prisonerDomesticStatusService.getDomesticStatus(prisonerNumber)).thenReturn(response)

      val result = prisonerDomesticStatusFacade.getDomesticStatus(prisonerNumber)

      assertThat(result).isNotNull
      assertThat(result).isEqualTo(response)
    }

    @Test
    fun `should return not found`() {
      val prisonerNumber = "A1234BC"

      whenever(prisonerDomesticStatusService.getDomesticStatus(prisonerNumber)).thenThrow(RuntimeException("Prisoner's domestic status not found!"))

      assertThrows<RuntimeException> {
        prisonerDomesticStatusFacade.getDomesticStatus(prisonerNumber)
      }.message isEqualTo "Prisoner's domestic status not found!"

      verify(prisonerDomesticStatusService).getDomesticStatus(prisonerNumber)
    }
  }

  @Nested
  inner class CreateOrUpdateDomesticStatus {

    private val user = aUser("test-user")

    @Test
    fun `should create new record and send created event when no existing record`() {
      val prisonerNumber = "A1234BC"
      val request = CreateOrUpdatePrisonerDomesticStatusRequest(
        domesticStatusCode = "MARRIED",
      )
      val updatedResponse = PrisonerDomesticStatusResponse(
        id = 2L,
        domesticStatusCode = "MARRIED",
        active = true,
      )
      whenever(prisonerDomesticStatusService.createOrUpdateDomesticStatus(prisonerNumber, request, user)).thenReturn(
        updatedResponse,
      )
      val result = prisonerDomesticStatusFacade.createOrUpdateDomesticStatus(prisonerNumber, request, user)

      assertThat(result)
        .isNotNull
        .isEqualTo(updatedResponse)

      verify(outboundEventsService).send(
        outboundEvent = OutboundEvent.PRISONER_DOMESTIC_STATUS_CREATED,
        identifier = updatedResponse.id,
        noms = prisonerNumber,
        source = Source.DPS,
        user = user,
      )
    }

    @Test
    fun `should update existing record and send created events`() {
      val prisonerNumber = "A1234BC"
      val request = CreateOrUpdatePrisonerDomesticStatusRequest(
        domesticStatusCode = "MARRIED",
      )
      val updatedResponse = PrisonerDomesticStatusResponse(
        id = 2L,
        domesticStatusCode = "MARRIED",
        active = true,
      )
      whenever(prisonerDomesticStatusService.createOrUpdateDomesticStatus(prisonerNumber, request, user)).thenReturn(
        updatedResponse,
      )
      val result = prisonerDomesticStatusFacade.createOrUpdateDomesticStatus(prisonerNumber, request, user)

      assertThat(result)
        .isNotNull
        .isEqualTo(updatedResponse)

      verify(outboundEventsService).send(
        outboundEvent = OutboundEvent.PRISONER_DOMESTIC_STATUS_CREATED,
        identifier = updatedResponse.id,
        noms = prisonerNumber,
        source = Source.DPS,
        user = user,
      )
    }

    @Test
    fun `should not send created event on create or update failure `() {
      val prisonerNumber = "A1234BC"
      val request = CreateOrUpdatePrisonerDomesticStatusRequest(

        domesticStatusCode = "MARRIED",
      )
      whenever(prisonerDomesticStatusService.createOrUpdateDomesticStatus(prisonerNumber, request, user)).thenThrow(
        RuntimeException("Prisoner's domestic status could not updated!"),
      )

      assertThrows<RuntimeException> {
        prisonerDomesticStatusFacade.createOrUpdateDomesticStatus(prisonerNumber, request, user)
      }.message isEqualTo "Prisoner's domestic status could not updated!"

      verify(prisonerDomesticStatusService).createOrUpdateDomesticStatus(prisonerNumber, request, user)
      verify(outboundEventsService, never()).send(any(), any(), any(), any(), any(), any(), any())
    }
  }
}
