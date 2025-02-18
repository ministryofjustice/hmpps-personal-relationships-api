package uk.gov.justice.digital.hmpps.hmppscontactsapi.facade

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertNotNull
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
import uk.gov.justice.digital.hmpps.hmppscontactsapi.helpers.isEqualTo
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.UpdatePrisonerContactRestrictionRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.UpdatePrisonerDomesticStatusRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.PrisonerDomesticStatusResponse
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.PrisonerDomesticStatusService
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.OutboundEventsService
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.Source
import java.time.LocalDate

@ExtendWith(MockitoExtension::class)
class PrisonerDomesticStatusFacadeTest {

    @Mock
    private lateinit var prisonerDomesticStatusService: PrisonerDomesticStatusService

    @Mock
    private lateinit var outboundEventsService: OutboundEventsService

    @InjectMocks
    private lateinit var prisonerDomesticStatusFacade: PrisonerDomesticStatusFacade

    @Test
    fun `getDomesticStatus should return domestic status`() {
        val prisonerNumber = "A1234BC"
        val response = PrisonerDomesticStatusResponse(
            id = 1L,
            prisonerNumber = prisonerNumber,
            domesticStatusValue = "SINGLE",
            active = true,
        )

        whenever(prisonerDomesticStatusService.getDomesticStatus(prisonerNumber)).thenReturn(response)

        val result = prisonerDomesticStatusFacade.getDomesticStatus(prisonerNumber)

        assertNotNull(result)
        assertThat(result).isEqualTo(response)
    }

    @Test
    fun `getDomesticStatus should return not found`() {
        val prisonerNumber = "A1234BC"

        whenever(prisonerDomesticStatusService.getDomesticStatus(prisonerNumber)).thenThrow(RuntimeException("Prisoner's domestic status not found!"))

        assertThrows<RuntimeException> {
            prisonerDomesticStatusFacade.getDomesticStatus(prisonerNumber)
        }.message isEqualTo "Prisoner's domestic status not found!"

        verify(prisonerDomesticStatusService).getDomesticStatus(prisonerNumber)
    }

    @Test
    fun `createOrUpdateDomesticStatus should create send event`() {
        val prisonerNumber = "A1234BC"
        val request = UpdatePrisonerDomesticStatusRequest(
            domesticStatusCode = "MARRIED",
            prisonerNumber = prisonerNumber,
            updatedBy = "test-user",
        )
        val updatedResponse = PrisonerDomesticStatusResponse(
            id = 2L,
            prisonerNumber = prisonerNumber,
            domesticStatusValue = "MARRIED",
            active = true,
        )

        whenever(prisonerDomesticStatusService.createOrUpdateDomesticStatus(prisonerNumber, request)).thenReturn(
            updatedResponse,
        )
        val result = prisonerDomesticStatusFacade.createOrUpdateDomesticStatus(prisonerNumber, request)

        assertThat(result)
            .isNotNull
            .isEqualTo(updatedResponse)

        verify(outboundEventsService).send(
            outboundEvent = OutboundEvent.PRISONER_DOMESTIC_STATUS_CREATED,
            identifier = updatedResponse.id,
            noms = updatedResponse.prisonerNumber,
            source = Source.DPS,
        )
    }

    @Test
    fun `should not send crate prisoner domestic status event on failure`() {
        val prisonerNumber = "A1234BC"
        val request = UpdatePrisonerDomesticStatusRequest(

            domesticStatusCode = "MARRIED",
            prisonerNumber = prisonerNumber,
            updatedBy = "test-user",
        )

        whenever(prisonerDomesticStatusService.createOrUpdateDomesticStatus(prisonerNumber, request)).thenThrow(RuntimeException("Prisoner's domestic status could not updated!"))

        assertThrows<RuntimeException> {
            prisonerDomesticStatusFacade.createOrUpdateDomesticStatus(prisonerNumber, request)
        }.message isEqualTo "Prisoner's domestic status could not updated!"

        verify(prisonerDomesticStatusService).createOrUpdateDomesticStatus(prisonerNumber, request)
        verify(outboundEventsService, never()).send(any(), any(), any(), any(), any(), any())
    }
}
