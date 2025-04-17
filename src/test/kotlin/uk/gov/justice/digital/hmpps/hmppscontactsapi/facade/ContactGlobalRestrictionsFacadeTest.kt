package uk.gov.justice.digital.hmpps.hmppscontactsapi.facade

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.CreateContactRestrictionRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.UpdateContactRestrictionRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.ContactRestrictionDetails
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.RestrictionsService
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.OutboundEventsService
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.Source
import java.time.LocalDate
import java.time.LocalDateTime

class ContactGlobalRestrictionsFacadeTest {

  private val restrictionService: RestrictionsService = mock()
  private val outboundEventsService: OutboundEventsService = mock()
  private val facade = ContactGlobalRestrictionsFacade(restrictionService, outboundEventsService)

  private val contactId: Long = 99
  private val contactRestrictionId: Long = 66

  @Test
  fun `should send created global restriction event on success`() {
    val request = CreateContactRestrictionRequest(
      restrictionType = "BAN",
      startDate = LocalDate.of(2020, 1, 1),
      expiryDate = LocalDate.of(2022, 2, 2),
      comments = "Some comments",
      createdBy = "created",
    )

    val expected = ContactRestrictionDetails(
      contactRestrictionId = contactRestrictionId,
      contactId = contactId,
      restrictionType = "BAN",
      restrictionTypeDescription = "Banned",
      startDate = LocalDate.of(2020, 1, 1),
      expiryDate = LocalDate.of(2022, 2, 2),
      comments = "Some comments",
      createdBy = "created",
      createdTime = LocalDateTime.now(),
      enteredByUsername = "created",
      enteredByDisplayName = "John Created",
      updatedBy = null,
      updatedTime = null,
    )
    whenever(restrictionService.createContactGlobalRestriction(contactId, request)).thenReturn(expected)

    val result = facade.createContactGlobalRestriction(contactId, request)

    assertThat(result).isEqualTo(expected)
    verify(restrictionService).createContactGlobalRestriction(contactId, request)
    verify(outboundEventsService).send(
      outboundEvent = OutboundEvent.CONTACT_RESTRICTION_CREATED,
      identifier = contactRestrictionId,
      contactId = contactId,
      noms = "",
      source = Source.DPS,
    )
  }

  @Test
  fun `should not send created global restriction event on failure`() {
    val request = CreateContactRestrictionRequest(
      restrictionType = "BAN",
      startDate = LocalDate.of(2020, 1, 1),
      expiryDate = LocalDate.of(2022, 2, 2),
      comments = "Some comments",
      createdBy = "created",
    )

    val expected = RuntimeException("Bang!")
    whenever(restrictionService.createContactGlobalRestriction(contactId, request)).thenThrow(expected)

    val result = assertThrows<RuntimeException> {
      facade.createContactGlobalRestriction(contactId, request)
    }

    assertThat(result).isEqualTo(expected)
    verify(restrictionService).createContactGlobalRestriction(contactId, request)
    verify(outboundEventsService, never()).send(any(), any(), any(), any(), any(), any(), any())
  }

  @Test
  fun `should send updated global restriction event on success`() {
    val request = UpdateContactRestrictionRequest(
      restrictionType = "BAN",
      startDate = LocalDate.of(2020, 1, 1),
      expiryDate = LocalDate.of(2022, 2, 2),
      comments = "Some comments",
      updatedBy = "updated",
    )

    val expected = ContactRestrictionDetails(
      contactRestrictionId = contactRestrictionId,
      contactId = contactId,
      restrictionType = "BAN",
      restrictionTypeDescription = "Banned",
      startDate = LocalDate.of(2020, 1, 1),
      expiryDate = LocalDate.of(2022, 2, 2),
      comments = "Some comments",
      enteredByUsername = "updated",
      enteredByDisplayName = "John Updated",
      createdBy = "created",
      createdTime = LocalDateTime.now(),
      updatedBy = "updated",
      updatedTime = LocalDateTime.now(),
    )
    whenever(restrictionService.updateContactGlobalRestriction(contactId, contactRestrictionId, request)).thenReturn(expected)

    val result = facade.updateContactGlobalRestriction(contactId, contactRestrictionId, request)

    assertThat(result).isEqualTo(expected)
    verify(restrictionService).updateContactGlobalRestriction(contactId, contactRestrictionId, request)
    verify(outboundEventsService).send(
      outboundEvent = OutboundEvent.CONTACT_RESTRICTION_UPDATED,
      identifier = contactRestrictionId,
      contactId = contactId,
      noms = "",
      source = Source.DPS,
    )
  }

  @Test
  fun `should not send updated global restriction event on failure`() {
    val request = UpdateContactRestrictionRequest(
      restrictionType = "BAN",
      startDate = LocalDate.of(2020, 1, 1),
      expiryDate = LocalDate.of(2022, 2, 2),
      comments = "Some comments",
      updatedBy = "updated",
    )

    val expected = RuntimeException("Bang!")
    whenever(restrictionService.updateContactGlobalRestriction(contactId, contactRestrictionId, request)).thenThrow(expected)

    val result = assertThrows<RuntimeException> {
      facade.updateContactGlobalRestriction(contactId, contactRestrictionId, request)
    }

    assertThat(result).isEqualTo(expected)
    verify(restrictionService).updateContactGlobalRestriction(contactId, contactRestrictionId, request)
    verify(outboundEventsService, never()).send(any(), any(), any(), any(), any(), any(), any())
  }
}
