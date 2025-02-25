package uk.gov.justice.digital.hmpps.hmppscontactsapi.facade.sync

import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.PrisonerDomesticStatus
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync.SyncUpdatePrisonerDomesticStatusRequest
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
      )

      whenever(syncDomesticStatusService.getPrisonerDomesticStatusActive(prisonerNumber))
        .thenReturn(null)
      whenever(syncDomesticStatusService.createOrUpdateDomesticStatus(prisonerNumber, request))
        .thenReturn(response)

      // When
      val result = facade.createOrUpdateDomesticStatus(prisonerNumber, request)

      // Then
      verify(outboundEventsService, never()).send(
        outboundEvent = OutboundEvent.PRISONER_DOMESTIC_STATUS_UPDATED,
        identifier = result.id,
        noms = prisonerNumber,
        source = Source.NOMIS,
      )
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
      )
      val existingRecord = PrisonerDomesticStatus(
        prisonerDomesticStatusId = 2L,
        prisonerNumber = prisonerNumber,
        domesticStatusCode = "D",
        createdBy = "USER1",
        createdTime = createdTime,
        active = true,
      )

      whenever(syncDomesticStatusService.getPrisonerDomesticStatusActive(prisonerNumber))
        .thenReturn(existingRecord)
      whenever(syncDomesticStatusService.createOrUpdateDomesticStatus(prisonerNumber, request))
        .thenReturn(response)

      // When
      val result = facade.createOrUpdateDomesticStatus(prisonerNumber, request)

      // Then
      verify(outboundEventsService).send(
        outboundEvent = OutboundEvent.PRISONER_DOMESTIC_STATUS_UPDATED,
        identifier = existingRecord.prisonerDomesticStatusId,
        noms = prisonerNumber,
        source = Source.NOMIS,
      )
      verify(outboundEventsService).send(
        outboundEvent = OutboundEvent.PRISONER_DOMESTIC_STATUS_CREATED,
        identifier = response.id,
        noms = prisonerNumber,
        source = Source.NOMIS,
      )
      assertThat(result).isEqualTo(response)
    }

    @Test
    fun `should not send both updated and created event on create or update failure `() {
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
      )
      val existingRecord = PrisonerDomesticStatus(
        prisonerDomesticStatusId = 2L,
        prisonerNumber = prisonerNumber,
        domesticStatusCode = "D",
        createdBy = "USER1",
        createdTime = createdTime,
        active = true,
      )

      whenever(syncDomesticStatusService.getPrisonerDomesticStatusActive(prisonerNumber))
        .thenThrow(EntityNotFoundException("Not found"))
      whenever(syncDomesticStatusService.createOrUpdateDomesticStatus(prisonerNumber, request))
        .thenReturn(response)

      // When
      val error = assertThrows<EntityNotFoundException> {
        facade.createOrUpdateDomesticStatus(prisonerNumber, request)
      }
      assertThat(error.message).isEqualTo("Not found")

      // Then
      verify(outboundEventsService, never()).send(
        outboundEvent = OutboundEvent.PRISONER_DOMESTIC_STATUS_UPDATED,
        identifier = existingRecord.prisonerDomesticStatusId,
        noms = prisonerNumber,
        source = Source.NOMIS,
      )
      verify(outboundEventsService, never()).send(
        outboundEvent = OutboundEvent.PRISONER_DOMESTIC_STATUS_CREATED,
        identifier = response.id,
        noms = prisonerNumber,
        source = Source.NOMIS,
      )
    }
  }
}
