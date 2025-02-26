package uk.gov.justice.digital.hmpps.hmppscontactsapi.facade.sync

import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.PrisonerNumberOfChildren
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync.SyncUpdatePrisonerNumberOfChildrenRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.sync.SyncPrisonerNumberOfChildrenResponse
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.OutboundEventsService
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.Source
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.sync.SyncPrisonerNumberOfChildrenService
import java.time.LocalDateTime

class PrisonerNumberOfChildrenSyncFacadeTest {

  private val syncNumberOfChildrenService = mock<SyncPrisonerNumberOfChildrenService>()
  private val outboundEventsService = mock<OutboundEventsService>()
  private val facade = PrisonerNumberOfChildrenSyncFacade(syncNumberOfChildrenService, outboundEventsService)

  @Nested
  inner class GetNumberOfChildrenByPrisonerNumber {
    @Test
    fun `should return number of children when prisoner number exists`() {
      val prisonerNumber = "A1234BC"
      val expected = SyncPrisonerNumberOfChildrenResponse(
        id = 1L,
        numberOfChildren = "1",
        createdBy = "USER1",
        createdTime = LocalDateTime.now(),
        active = true,
      )

      whenever(syncNumberOfChildrenService.getNumberOfChildrenByPrisonerNumber(prisonerNumber))
        .thenReturn(expected)

      val result = facade.getNumberOfChildrenByPrisonerNumber(prisonerNumber)

      verify(syncNumberOfChildrenService).getNumberOfChildrenByPrisonerNumber(prisonerNumber)
      assertThat(result).isEqualTo(expected)
    }
  }

  @Nested
  inner class UpdateNumberOfChildren {
    @Test
    fun `should create new record and send created event when no existing record`() {
      // Given
      val prisonerNumber = "A1234BC"
      val request = SyncUpdatePrisonerNumberOfChildrenRequest(
        numberOfChildren = "1",
        createdBy = "User",
        createdTime = LocalDateTime.now(),
      )
      val updatedNumberOfChildrenCount = SyncPrisonerNumberOfChildrenResponse(
        id = 1L,
        numberOfChildren = "1",
        createdBy = "USER1",
        createdTime = LocalDateTime.now(),
        active = true,
      )

      whenever(syncNumberOfChildrenService.getPrisonerNumberOfChildrenActive(prisonerNumber))
        .thenReturn(null)
      whenever(syncNumberOfChildrenService.createOrUpdateNumberOfChildren(prisonerNumber, request))
        .thenReturn(updatedNumberOfChildrenCount)

      val response = facade.createOrUpdateNumberOfChildren(prisonerNumber, request)

      verify(syncNumberOfChildrenService).createOrUpdateNumberOfChildren(prisonerNumber, request)
      // Then
      verify(outboundEventsService, never()).send(
        outboundEvent = OutboundEvent.PRISONER_NUMBER_OF_CHILDREN_UPDATED,
        identifier = response.id,
        noms = prisonerNumber,
        source = Source.NOMIS,
      )
      verify(outboundEventsService, times(1)).send(
        outboundEvent = OutboundEvent.PRISONER_NUMBER_OF_CHILDREN_CREATED,
        identifier = response.id,
        noms = prisonerNumber,
        source = Source.NOMIS,
      )
      assertThat(response).isEqualTo(response)
    }

    @Test
    fun `should update existing record and send both updated and created events`() {
      // Given
      val prisonerNumber = "A1234BC"
      val createdTime = LocalDateTime.now()
      val request = SyncUpdatePrisonerNumberOfChildrenRequest(
        numberOfChildren = "1",
        createdBy = "User",
        createdTime = createdTime,
      )
      val response = SyncPrisonerNumberOfChildrenResponse(
        id = 1L,
        numberOfChildren = "1",
        createdBy = "USER1",
        createdTime = LocalDateTime.now(),
        active = true,
      )
      val existingRecord = PrisonerNumberOfChildren(
        prisonerNumberOfChildrenId = 2L,
        prisonerNumber = prisonerNumber,
        numberOfChildren = "1",
        createdBy = "USER1",
        createdTime = createdTime,
        active = true,
      )

      whenever(syncNumberOfChildrenService.getPrisonerNumberOfChildrenActive(prisonerNumber))
        .thenReturn(existingRecord)
      whenever(syncNumberOfChildrenService.createOrUpdateNumberOfChildren(prisonerNumber, request))
        .thenReturn(response)

      // When
      val result = facade.createOrUpdateNumberOfChildren(prisonerNumber, request)

      // Then
      verify(outboundEventsService).send(
        outboundEvent = OutboundEvent.PRISONER_NUMBER_OF_CHILDREN_UPDATED,
        identifier = existingRecord.prisonerNumberOfChildrenId,
        noms = prisonerNumber,
        source = Source.NOMIS,
      )
      verify(outboundEventsService).send(
        outboundEvent = OutboundEvent.PRISONER_NUMBER_OF_CHILDREN_CREATED,
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
      val request = SyncUpdatePrisonerNumberOfChildrenRequest(
        numberOfChildren = "1",
        createdBy = "User",
        createdTime = createdTime,
      )
      val response = SyncPrisonerNumberOfChildrenResponse(
        id = 1L,
        numberOfChildren = "1",
        createdBy = "USER1",
        createdTime = LocalDateTime.now(),
        active = true,
      )
      val existingRecord = PrisonerNumberOfChildren(
        prisonerNumberOfChildrenId = 2L,
        prisonerNumber = prisonerNumber,
        numberOfChildren = "1",
        createdBy = "USER1",
        createdTime = createdTime,
        active = true,
      )

      whenever(syncNumberOfChildrenService.getPrisonerNumberOfChildrenActive(prisonerNumber))
        .thenThrow(EntityNotFoundException("Not found"))
      whenever(syncNumberOfChildrenService.createOrUpdateNumberOfChildren(prisonerNumber, request))
        .thenReturn(response)

      // When
      val error = assertThrows<EntityNotFoundException> {
        facade.createOrUpdateNumberOfChildren(prisonerNumber, request)
      }
      assertThat(error.message).isEqualTo("Not found")

      // Then
      verify(outboundEventsService, never()).send(
        outboundEvent = OutboundEvent.PRISONER_NUMBER_OF_CHILDREN_UPDATED,
        identifier = existingRecord.prisonerNumberOfChildrenId,
        noms = prisonerNumber,
        source = Source.NOMIS,
      )
      verify(outboundEventsService, never()).send(
        outboundEvent = OutboundEvent.PRISONER_NUMBER_OF_CHILDREN_CREATED,
        identifier = response.id,
        noms = prisonerNumber,
        source = Source.NOMIS,
      )
    }
  }
}
