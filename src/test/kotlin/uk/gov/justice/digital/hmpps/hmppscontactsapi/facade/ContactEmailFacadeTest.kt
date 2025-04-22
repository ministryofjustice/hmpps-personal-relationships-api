package uk.gov.justice.digital.hmpps.hmppscontactsapi.facade

import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppscontactsapi.helpers.createContactEmailDetails
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.email.CreateEmailRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.email.UpdateEmailRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.ContactEmailService
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.OutboundEventsService
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.Source

class ContactEmailFacadeTest {

  private val emailService: ContactEmailService = mock()
  private val eventsService: OutboundEventsService = mock()
  private val facade = ContactEmailFacade(emailService, eventsService)

  private val contactId = 11L
  private val contactEmailId = 99L
  private val contactEmailDetails = createContactEmailDetails(id = contactEmailId, contactId = contactId)

  @Test
  fun `should send event if create success`() {
    whenever(emailService.create(any(), any())).thenReturn(contactEmailDetails)
    whenever(eventsService.send(any(), any(), any(), any(), any(), any(), any())).then {}
    val request = CreateEmailRequest(
      emailAddress = "test@example.com",
      createdBy = "created",
    )

    val result = facade.create(contactId, request)

    assertThat(result).isEqualTo(contactEmailDetails)
    verify(emailService).create(contactId, request)
    verify(eventsService).send(
      outboundEvent = OutboundEvent.CONTACT_EMAIL_CREATED,
      identifier = contactEmailId,
      contactId = contactId,
      source = Source.DPS,
    )
  }

  @Test
  fun `should not send event if create throws exception and propagate the exception`() {
    val expectedException = RuntimeException("Bang!")
    whenever(emailService.create(any(), any())).thenThrow(expectedException)
    whenever(eventsService.send(any(), any(), any(), any(), any(), any(), any())).then {}
    val request = CreateEmailRequest(
      emailAddress = "test@example.com",
      createdBy = "created",
    )

    val exception = assertThrows<RuntimeException> {
      facade.create(contactId, request)
    }

    assertThat(exception).isEqualTo(exception)
    verify(emailService).create(contactId, request)
    verify(eventsService, never()).send(any(), any(), any(), any(), any(), any(), any())
  }

  @Test
  fun `should send event if update success`() {
    whenever(emailService.update(any(), any(), any())).thenReturn(contactEmailDetails)
    whenever(eventsService.send(any(), any(), any(), any(), any(), any(), any())).then {}
    val request = UpdateEmailRequest(
      emailAddress = "test@example.com",
      updatedBy = "updated",
    )

    val result = facade.update(contactId, contactEmailId, request)

    assertThat(result).isEqualTo(contactEmailDetails)
    verify(emailService).update(contactId, contactEmailId, request)
    verify(eventsService).send(
      outboundEvent = OutboundEvent.CONTACT_EMAIL_UPDATED,
      identifier = contactEmailId,
      contactId = contactId,
      source = Source.DPS,
    )
  }

  @Test
  fun `should not send event if update throws exception and propagate the exception`() {
    val expectedException = RuntimeException("Bang!")
    whenever(emailService.update(any(), any(), any())).thenThrow(expectedException)
    whenever(eventsService.send(any(), any(), any(), any(), any(), any(), any())).then {}
    val request = UpdateEmailRequest(
      emailAddress = "test@example.com",
      updatedBy = "updated",
    )

    val exception = assertThrows<RuntimeException> {
      facade.update(contactId, contactEmailId, request)
    }

    assertThat(exception).isEqualTo(exception)
    verify(emailService).update(contactId, contactEmailId, request)
    verify(eventsService, never()).send(any(), any(), any(), any(), any(), any(), any())
  }

  @Test
  fun `should not send no event on get`() {
    whenever(emailService.get(any(), any())).thenReturn(contactEmailDetails)

    val result = facade.get(contactId, contactEmailId)

    assertThat(result).isEqualTo(contactEmailDetails)
    verify(emailService).get(contactId, contactEmailId)
    verify(eventsService, never()).send(any(), any(), any(), any(), any(), any(), any())
  }

  @Test
  fun `should throw exception if there is no email found on get`() {
    whenever(emailService.get(any(), any())).thenReturn(null)

    val exception = assertThrows<EntityNotFoundException> {
      facade.get(contactId, contactEmailId)
    }

    assertThat(exception.message).isEqualTo("Contact email with id (99) not found for contact (11)")
    verify(emailService).get(contactId, contactEmailId)
    verify(eventsService, never()).send(any(), any(), any(), any(), any(), any(), any())
  }

  @Test
  fun `should send event if delete success`() {
    whenever(emailService.delete(any(), any())).then {}
    whenever(eventsService.send(any(), any(), any(), any(), any(), any(), any())).then {}

    facade.delete(contactId, contactEmailId)

    verify(emailService).delete(contactId, contactEmailId)
    verify(eventsService).send(OutboundEvent.CONTACT_EMAIL_DELETED, contactEmailId, contactId)
  }

  @Test
  fun `should not send event if delete throws exception and propagate the exception`() {
    val expectedException = RuntimeException("Bang!")
    whenever(emailService.delete(any(), any())).thenThrow(expectedException)
    whenever(eventsService.send(any(), any(), any(), any(), any(), any(), any())).then {}

    val exception = assertThrows<RuntimeException> {
      facade.delete(contactId, contactEmailId)
    }

    assertThat(exception).isEqualTo(exception)
    verify(emailService).delete(contactId, contactEmailId)
    verify(eventsService, never()).send(any(), any(), any(), any(), any(), any(), any())
  }
}
