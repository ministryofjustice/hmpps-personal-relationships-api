package uk.gov.justice.digital.hmpps.hmppscontactsapi.facade

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertNotNull
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
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.PrisonerNumberOfChildren
import uk.gov.justice.digital.hmpps.hmppscontactsapi.helpers.isEqualTo
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.CreateOrUpdatePrisonerNumberOfChildrenRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.PrisonerNumberOfChildrenResponse
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.PrisonerNumberOfChildrenService
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.OutboundEventsService
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.Source
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
class PrisonerNumberOfChildrenFacadeTest {

  @Mock
  private lateinit var prisonerNumberOfChildrenService: PrisonerNumberOfChildrenService

  @Mock
  private lateinit var outboundEventsService: OutboundEventsService

  @InjectMocks
  private lateinit var prisonerNumberOfChildrenFacade: PrisonerNumberOfChildrenFacade

  @Nested
  inner class GetNumberOfChildren {

    @Test
    fun `should return number of children`() {
      val prisonerNumber = "A1234BC"
      val response = PrisonerNumberOfChildrenResponse(
        id = 1L,
        numberOfChildren = "1",
        active = true,
      )

      whenever(prisonerNumberOfChildrenService.getNumberOfChildren(prisonerNumber)).thenReturn(response)

      val result = prisonerNumberOfChildrenFacade.getNumberOfChildren(prisonerNumber)

      assertNotNull(result)
      assertThat(result).isEqualTo(response)
    }

    @Test
    fun `should return not found`() {
      val prisonerNumber = "A1234BC"

      whenever(prisonerNumberOfChildrenService.getNumberOfChildren(prisonerNumber)).thenThrow(RuntimeException("Prisoner's number of children not found!"))

      assertThrows<RuntimeException> {
        prisonerNumberOfChildrenFacade.getNumberOfChildren(prisonerNumber)
      }.message isEqualTo "Prisoner's number of children not found!"

      verify(prisonerNumberOfChildrenService).getNumberOfChildren(prisonerNumber)
    }
  }

  @Nested
  inner class CreateOrUpdateNumberOfChildren {

    @Test
    fun `should create new record and send created event when no existing record`() {
      val prisonerNumber = "A1234BC"
      val request = CreateOrUpdatePrisonerNumberOfChildrenRequest(
        numberOfChildren = 1,
        requestedBy = "test-user",
      )
      val updatedResponse = PrisonerNumberOfChildrenResponse(
        id = 2L,
        numberOfChildren = "1",
        active = true,
      )

      whenever(prisonerNumberOfChildrenService.createOrUpdateNumberOfChildren(prisonerNumber, request)).thenReturn(
        updatedResponse,
      )
      val result = prisonerNumberOfChildrenFacade.createOrUpdateNumberOfChildren(prisonerNumber, request)

      assertThat(result)
        .isNotNull
        .isEqualTo(updatedResponse)

      verify(outboundEventsService).send(
        outboundEvent = OutboundEvent.PRISONER_NUMBER_OF_CHILDREN_CREATED,
        identifier = updatedResponse.id,
        noms = prisonerNumber,
        source = Source.DPS,
      )
    }

    @Test
    fun `should update existing record and send created events`() {
      val prisonerNumber = "A1234BC"
      val createdTime = LocalDateTime.now()
      val request = CreateOrUpdatePrisonerNumberOfChildrenRequest(
        numberOfChildren = 2,
        requestedBy = "test-user",
      )
      val updatedResponse = PrisonerNumberOfChildrenResponse(
        id = 2L,
        numberOfChildren = "2",
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
      whenever(prisonerNumberOfChildrenService.createOrUpdateNumberOfChildren(prisonerNumber, request)).thenReturn(
        updatedResponse,
      )
      val result = prisonerNumberOfChildrenFacade.createOrUpdateNumberOfChildren(prisonerNumber, request)

      assertThat(result)
        .isNotNull
        .isEqualTo(updatedResponse)

      verify(outboundEventsService).send(
        outboundEvent = OutboundEvent.PRISONER_NUMBER_OF_CHILDREN_CREATED,
        identifier = updatedResponse.id,
        noms = prisonerNumber,
        source = Source.DPS,
      )
    }

    @Test
    fun `should not send created event on create or update failure`() {
      val prisonerNumber = "A1234BC"
      val request = CreateOrUpdatePrisonerNumberOfChildrenRequest(

        numberOfChildren = 1,
        requestedBy = "test-user",
      )

      whenever(prisonerNumberOfChildrenService.createOrUpdateNumberOfChildren(prisonerNumber, request))
        .thenThrow(RuntimeException("Prisoner's number of children could not updated!"))

      assertThrows<RuntimeException> {
        prisonerNumberOfChildrenFacade.createOrUpdateNumberOfChildren(prisonerNumber, request)
      }.message isEqualTo "Prisoner's number of children could not updated!"

      verify(prisonerNumberOfChildrenService).createOrUpdateNumberOfChildren(prisonerNumber, request)
      verify(outboundEventsService, never()).send(any(), any(), any(), any(), any(), any(), any())
    }
  }
}
