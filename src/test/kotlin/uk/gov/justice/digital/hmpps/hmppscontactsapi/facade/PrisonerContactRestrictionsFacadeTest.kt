package uk.gov.justice.digital.hmpps.hmppscontactsapi.facade

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppscontactsapi.helpers.aUser
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.restrictions.CreatePrisonerContactRestrictionRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.restrictions.UpdatePrisonerContactRestrictionRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.PrisonerContactRestrictionDetails
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.RestrictionsService
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.OutboundEventsService
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.Source
import java.time.LocalDate
import java.time.LocalDateTime

class PrisonerContactRestrictionsFacadeTest {

  private val restrictionService: RestrictionsService = mock()
  private val outboundEventsService: OutboundEventsService = mock()
  private val facade = PrisonerContactRestrictionsFacade(restrictionService, outboundEventsService)

  private val prisonerNumber = "A1234BC"
  private val contactId: Long = 77
  private val prisonerContactId: Long = 99
  private val prisonerContactRestrictionId: Long = 66
  private val user = aUser("restrictions")

  @Test
  fun `should send created prisoner contact restriction event on success`() {
    val request = CreatePrisonerContactRestrictionRequest(
      restrictionType = "BAN",
      startDate = LocalDate.of(2020, 1, 1),
      expiryDate = LocalDate.of(2022, 2, 2),
      comments = "Some comments",
    )

    val expected = PrisonerContactRestrictionDetails(
      prisonerContactRestrictionId = prisonerContactRestrictionId,
      prisonerContactId = prisonerContactId,
      contactId = contactId,
      prisonerNumber = prisonerNumber,
      restrictionType = "BAN",
      restrictionTypeDescription = "Banned",
      startDate = LocalDate.of(2020, 1, 1),
      expiryDate = LocalDate.of(2022, 2, 2),
      comments = "Some comments",
      enteredByUsername = "created",
      enteredByDisplayName = "Created User",
      createdBy = "created",
      createdTime = LocalDateTime.now(),
      updatedBy = null,
      updatedTime = null,
    )
    whenever(restrictionService.createPrisonerContactRestriction(prisonerContactId, request, user)).thenReturn(expected)

    val result = facade.createPrisonerContactRestriction(prisonerContactId, request, user)

    assertThat(result).isEqualTo(expected)
    verify(restrictionService).createPrisonerContactRestriction(prisonerContactId, request, user)
    verify(outboundEventsService).send(
      outboundEvent = OutboundEvent.PRISONER_CONTACT_RESTRICTION_CREATED,
      identifier = prisonerContactRestrictionId,
      contactId = contactId,
      noms = prisonerNumber,
      source = Source.DPS,
      user = user,
    )
  }

  @Test
  fun `should not send created prisoner contact restriction event on failure`() {
    val request = CreatePrisonerContactRestrictionRequest(
      restrictionType = "BAN",
      startDate = LocalDate.of(2020, 1, 1),
      expiryDate = LocalDate.of(2022, 2, 2),
      comments = "Some comments",
    )

    val expected = RuntimeException("Bang!")
    whenever(restrictionService.createPrisonerContactRestriction(prisonerContactId, request, user)).thenThrow(expected)

    val result = assertThrows<RuntimeException> {
      facade.createPrisonerContactRestriction(prisonerContactId, request, user)
    }

    assertThat(result).isEqualTo(expected)
    verify(restrictionService).createPrisonerContactRestriction(prisonerContactId, request, user)
    verify(outboundEventsService, never()).send(any(), any(), any(), any(), any(), any(), any())
  }

  @Test
  fun `should send updated prisoner contact restriction event on success`() {
    val request = UpdatePrisonerContactRestrictionRequest(
      restrictionType = "BAN",
      startDate = LocalDate.of(2020, 1, 1),
      expiryDate = LocalDate.of(2022, 2, 2),
      comments = "Some comments",
    )

    val expected = PrisonerContactRestrictionDetails(
      prisonerContactRestrictionId = prisonerContactRestrictionId,
      prisonerContactId = prisonerContactId,
      contactId = contactId,
      prisonerNumber = prisonerNumber,
      restrictionType = "BAN",
      restrictionTypeDescription = "Banned",
      startDate = LocalDate.of(2020, 1, 1),
      expiryDate = LocalDate.of(2022, 2, 2),
      comments = "Some comments",
      enteredByUsername = "updated",
      enteredByDisplayName = "Updated User",
      createdBy = "created",
      createdTime = LocalDateTime.now(),
      updatedBy = "updated",
      updatedTime = LocalDateTime.now(),
    )
    whenever(restrictionService.updatePrisonerContactRestriction(prisonerContactId, prisonerContactRestrictionId, request, user)).thenReturn(expected)

    val result = facade.updatePrisonerContactRestriction(prisonerContactId, prisonerContactRestrictionId, request, user)

    assertThat(result).isEqualTo(expected)
    verify(restrictionService).updatePrisonerContactRestriction(prisonerContactId, prisonerContactRestrictionId, request, user)
    verify(outboundEventsService).send(
      outboundEvent = OutboundEvent.PRISONER_CONTACT_RESTRICTION_UPDATED,
      identifier = prisonerContactRestrictionId,
      contactId = contactId,
      noms = prisonerNumber,
      source = Source.DPS,
      user = user,
    )
  }

  @Test
  fun `should not send updated prisoner contact restriction event on failure`() {
    val request = UpdatePrisonerContactRestrictionRequest(
      restrictionType = "BAN",
      startDate = LocalDate.of(2020, 1, 1),
      expiryDate = LocalDate.of(2022, 2, 2),
      comments = "Some comments",
    )

    val expected = RuntimeException("Bang!")
    whenever(restrictionService.updatePrisonerContactRestriction(prisonerContactId, prisonerContactRestrictionId, request, user)).thenThrow(expected)

    val result = assertThrows<RuntimeException> {
      facade.updatePrisonerContactRestriction(prisonerContactId, prisonerContactRestrictionId, request, user)
    }

    assertThat(result).isEqualTo(expected)
    verify(restrictionService).updatePrisonerContactRestriction(prisonerContactId, prisonerContactRestrictionId, request, user)
    verify(outboundEventsService, never()).send(any(), any(), any(), any(), any(), any(), any())
  }
}
