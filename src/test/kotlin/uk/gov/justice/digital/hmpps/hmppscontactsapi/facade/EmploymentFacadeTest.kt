package uk.gov.justice.digital.hmpps.hmppscontactsapi.facade

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppscontactsapi.helpers.aUser
import uk.gov.justice.digital.hmpps.hmppscontactsapi.helpers.createEmploymentDetails
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.internal.PatchEmploymentResult
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.employment.CreateEmploymentRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.employment.PatchEmploymentsRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.employment.UpdateEmploymentRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.EmploymentService
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.OutboundEventsService
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.Source

class EmploymentFacadeTest {

  private val employmentService: EmploymentService = mock()
  private val outboundEventsService: OutboundEventsService = mock()
  private val facade = EmploymentFacade(employmentService, outboundEventsService)
  private val user = aUser()

  @Test
  fun `patch contact should send updates for all ids created updated or deleted`() {
    val expectedEmploymentDetails = listOf(createEmploymentDetails())
    val contactId = 123L
    val request = PatchEmploymentsRequest(emptyList(), emptyList(), emptyList())

    whenever(employmentService.patchEmployments(contactId, request, user)).thenReturn(
      PatchEmploymentResult(
        createdIds = listOf(1, 2),
        updatedIds = listOf(3, 4),
        deletedIds = listOf(5, 6),
        employmentsAfterUpdate = expectedEmploymentDetails,
      ),
    )

    val result = facade.patchEmployments(contactId, request, user)

    assertThat(result).isEqualTo(expectedEmploymentDetails)
    verify(outboundEventsService).send(
      outboundEvent = OutboundEvent.EMPLOYMENT_CREATED,
      identifier = 1,
      contactId = contactId,
      noms = "",
      source = Source.DPS,
      user = user,
    )
    verify(outboundEventsService).send(
      outboundEvent = OutboundEvent.EMPLOYMENT_CREATED,
      identifier = 2,
      contactId = contactId,
      noms = "",
      source = Source.DPS,
      user = user,
    )

    verify(outboundEventsService).send(
      outboundEvent = OutboundEvent.EMPLOYMENT_UPDATED,
      identifier = 3,
      contactId = contactId,
      noms = "",
      source = Source.DPS,
      user = user,
    )
    verify(outboundEventsService).send(
      outboundEvent = OutboundEvent.EMPLOYMENT_UPDATED,
      identifier = 4,
      contactId = contactId,
      noms = "",
      source = Source.DPS,
      user = user,
    )

    verify(outboundEventsService).send(
      outboundEvent = OutboundEvent.EMPLOYMENT_DELETED,
      identifier = 5,
      contactId = contactId,
      noms = "",
      source = Source.DPS,
      user = user,
    )
    verify(outboundEventsService).send(
      outboundEvent = OutboundEvent.EMPLOYMENT_DELETED,
      identifier = 6,
      contactId = contactId,
      noms = "",
      source = Source.DPS,
      user = user,
    )
  }

  @Test
  fun `create employment should send event for created employment id`() {
    val expectedEmploymentDetails = createEmploymentDetails(id = 765)
    val contactId = 123L
    val request = CreateEmploymentRequest(999, true)

    whenever(employmentService.createEmployment(contactId, request.organisationId, request.isActive, user.username)).thenReturn(expectedEmploymentDetails)

    val result = facade.createEmployment(contactId, request, user)

    assertThat(result).isEqualTo(expectedEmploymentDetails)
    verify(outboundEventsService).send(
      outboundEvent = OutboundEvent.EMPLOYMENT_CREATED,
      identifier = 765,
      contactId = contactId,
      noms = "",
      source = Source.DPS,
      user = user,
    )
  }

  @Test
  fun `update employment should send event for updated employment id`() {
    val employmentId = 456L
    val expectedEmploymentDetails = createEmploymentDetails(id = employmentId)
    val contactId = 123L
    val request = UpdateEmploymentRequest(999, true)

    whenever(employmentService.updateEmployment(contactId, employmentId, request, user)).thenReturn(expectedEmploymentDetails)

    val result = facade.updateEmployment(contactId, employmentId, request, user)

    assertThat(result).isEqualTo(expectedEmploymentDetails)
    verify(outboundEventsService).send(
      outboundEvent = OutboundEvent.EMPLOYMENT_UPDATED,
      identifier = employmentId,
      contactId = contactId,
      noms = "",
      source = Source.DPS,
      user = user,
    )
  }

  @Test
  fun `delete employment should send event for deleted employment id`() {
    val employmentId = 456L
    val contactId = 123L

    doNothing().whenever(employmentService).deleteEmployment(contactId, employmentId)

    facade.deleteEmployment(contactId, employmentId, user)

    verify(outboundEventsService).send(
      outboundEvent = OutboundEvent.EMPLOYMENT_DELETED,
      identifier = employmentId,
      contactId = contactId,
      noms = "",
      source = Source.DPS,
      user = user,
    )
  }
}
