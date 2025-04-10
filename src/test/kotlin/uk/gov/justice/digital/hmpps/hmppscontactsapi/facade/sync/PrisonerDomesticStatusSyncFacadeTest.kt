package uk.gov.justice.digital.hmpps.hmppscontactsapi.facade.sync

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync.SyncUpdatePrisonerDomesticStatusRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.sync.Status
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.sync.SyncPrisonerDomesticStatusResponse
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.OutboundEventsService
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.Source
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.sync.SyncPrisonerDomesticStatusService
import java.time.LocalDateTime

class PrisonerDomesticStatusSyncFacadeTest {

  private val syncDomesticStatusService: SyncPrisonerDomesticStatusService = mock()
  private val outboundEventsService: OutboundEventsService = mock()
  private val facade = PrisonerDomesticStatusSyncFacade(syncDomesticStatusService, outboundEventsService)

  @Nested
  inner class GetDomesticStatusByPrisonerNumber {
    @Test
    fun `should return domestic status when prisoner number exists`() {
      val prisonerNumber = "A1234BC"
      val expectedStatus = SyncPrisonerDomesticStatusResponse(
        id = 1L,
        domesticStatusCode = "S",
        createdBy = "User",
        createdTime = LocalDateTime.now(),
        active = true,
      )

      whenever(syncDomesticStatusService.getDomesticStatusByPrisonerNumber(prisonerNumber))
        .thenReturn(expectedStatus)

      val result = facade.getDomesticStatusByPrisonerNumber(prisonerNumber)

      verify(syncDomesticStatusService).getDomesticStatusByPrisonerNumber(prisonerNumber)
      assertThat(result).isEqualTo(expectedStatus)
    }
  }

  @Nested
  inner class CreateOrUpdateDomesticStatus {

    @Test
    fun `should create new record and send created event when no existing record`() {
      // Given
      val prisonerNumber = "A1234BC"
      val request = SyncUpdatePrisonerDomesticStatusRequest(
        domesticStatusCode = "S",
        createdBy = "User",
        createdTime = LocalDateTime.now(),
      )
      val response = SyncPrisonerDomesticStatusResponse(
        id = 1L,
        domesticStatusCode = "S",
        createdBy = "User",
        createdTime = LocalDateTime.now(),
        active = true,
        status = Status.CREATED,
      )

      whenever(syncDomesticStatusService.createOrUpdateDomesticStatus(prisonerNumber, request))
        .thenReturn(response)

      // When
      val result = facade.createOrUpdateDomesticStatus(prisonerNumber, request)

      // Then
      verify(outboundEventsService, times(1)).send(
        outboundEvent = OutboundEvent.PRISONER_DOMESTIC_STATUS_CREATED,
        identifier = response.id,
        noms = prisonerNumber,
        source = Source.NOMIS,
      )
      assertThat(result).isEqualTo(response)
    }

    @Test
    fun `should update existing record and send both updated and created events`() {
      // Given
      val prisonerNumber = "A1234BC"
      val createdTime = LocalDateTime.now()
      val request = SyncUpdatePrisonerDomesticStatusRequest(
        domesticStatusCode = "S",
        createdBy = "User",
        createdTime = createdTime,
      )
      val response = SyncPrisonerDomesticStatusResponse(
        id = 1L,
        domesticStatusCode = "S",
        createdBy = "User",
        createdTime = LocalDateTime.now(),
        active = true,
        status = Status.UPDATED,
        updatedId = 2L,
      )

      whenever(syncDomesticStatusService.createOrUpdateDomesticStatus(prisonerNumber, request))
        .thenReturn(response)

      // When
      val result = facade.createOrUpdateDomesticStatus(prisonerNumber, request)

      // Then
      assertThat(result).isEqualTo(response)
      verify(outboundEventsService).send(
        outboundEvent = OutboundEvent.PRISONER_DOMESTIC_STATUS_CREATED,
        identifier = response.id,
        noms = prisonerNumber,
        source = Source.NOMIS,
      )
      verify(outboundEventsService).send(
        outboundEvent = OutboundEvent.PRISONER_DOMESTIC_STATUS_UPDATED,
        identifier = response.updatedId!!,
        noms = prisonerNumber,
        source = Source.NOMIS,
      )
    }

    @Test
    fun `should not send both updated and created event when status is unchanged `() {
      // Given
      val prisonerNumber = "A1234BC"
      val createdTime = LocalDateTime.now()
      val request = SyncUpdatePrisonerDomesticStatusRequest(
        domesticStatusCode = "S",
        createdBy = "User",
        createdTime = createdTime,
      )
      val response = SyncPrisonerDomesticStatusResponse(
        id = 1L,
        domesticStatusCode = "S",
        createdBy = "User",
        createdTime = LocalDateTime.now(),
        active = true,
        status = Status.UNCHANGED,
      )

      whenever(syncDomesticStatusService.createOrUpdateDomesticStatus(prisonerNumber, request))
        .thenReturn(response)

      // When
      facade.createOrUpdateDomesticStatus(prisonerNumber, request)

      // Then
      verify(outboundEventsService, never()).send(
        outboundEvent = OutboundEvent.PRISONER_DOMESTIC_STATUS_CREATED,
        identifier = response.id,
        noms = prisonerNumber,
        source = Source.NOMIS,
      )
    }
  }
}
