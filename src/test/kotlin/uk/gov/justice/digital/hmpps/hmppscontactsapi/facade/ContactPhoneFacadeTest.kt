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
import uk.gov.justice.digital.hmpps.hmppscontactsapi.helpers.createContactPhoneNumberDetails
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.phone.CreateMultiplePhoneNumbersRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.phone.CreatePhoneRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.phone.PhoneNumber
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.phone.UpdatePhoneRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.ContactPhoneService
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.OutboundEventsService
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.Source

class ContactPhoneFacadeTest {

  private val phoneService: ContactPhoneService = mock()
  private val eventsService: OutboundEventsService = mock()
  private val facade = ContactPhoneFacade(phoneService, eventsService)

  private val contactId = 11L
  private val contactPhoneId = 99L
  private val contactPhoneDetails = createContactPhoneNumberDetails(id = contactPhoneId, contactId = contactId)
  private val user = aUser()

  @Test
  fun `should send event if create success`() {
    whenever(phoneService.create(any(), any(), any())).thenReturn(contactPhoneDetails)
    whenever(eventsService.send(any(), any(), any(), any(), any(), any(), any())).then {}
    val request = CreatePhoneRequest(
      phoneType = "MOB",
      phoneNumber = "0777777777",
    )

    val result = facade.create(contactId, request, user)

    assertThat(result).isEqualTo(contactPhoneDetails)
    verify(phoneService).create(contactId, request, user)
    verify(eventsService).send(
      outboundEvent = OutboundEvent.CONTACT_PHONE_CREATED,
      identifier = contactPhoneId,
      contactId = contactId,
      source = Source.DPS,
      user = user,
    )
  }

  @Test
  fun `should not send event if create throws exception and propagate the exception`() {
    val expectedException = RuntimeException("Bang!")
    whenever(phoneService.create(any(), any(), any())).thenThrow(expectedException)
    whenever(eventsService.send(any(), any(), any(), any(), any(), any(), any())).then {}
    val request = CreatePhoneRequest(
      phoneType = "MOB",
      phoneNumber = "0777777777",
    )

    val exception = assertThrows<RuntimeException> {
      facade.create(contactId, request, user)
    }

    assertThat(exception).isEqualTo(exception)
    verify(phoneService).create(contactId, request, user)
    verify(eventsService, never()).send(any(), any(), any(), any(), any(), any(), any())
  }

  @Test
  fun `should send events for all if creating multiple`() {
    val expectedCreated = listOf(
      contactPhoneDetails,
      contactPhoneDetails.copy(contactPhoneId = 123456789),
    )
    whenever(phoneService.createMultiple(any(), any(), any())).thenReturn(expectedCreated)
    whenever(eventsService.send(any(), any(), any(), any(), any(), any(), any())).then {}
    val request = CreateMultiplePhoneNumbersRequest(
      listOf(
        PhoneNumber(
          phoneType = "MOB",
          phoneNumber = "0777777777",
        ),
        PhoneNumber(
          phoneType = "HOME",
          phoneNumber = "01234 567890",
        ),
      ),
    )

    val result = facade.createMultiple(contactId, request, user)

    assertThat(result).isEqualTo(expectedCreated)
    verify(phoneService).createMultiple(contactId, user.username, request.phoneNumbers)
    verify(eventsService).send(
      outboundEvent = OutboundEvent.CONTACT_PHONE_CREATED,
      identifier = contactPhoneId,
      contactId = contactId,
      source = Source.DPS,
      user = user,
    )
    verify(eventsService).send(
      outboundEvent = OutboundEvent.CONTACT_PHONE_CREATED,
      identifier = 123456789,
      contactId = contactId,
      source = Source.DPS,
      user = user,
    )
  }

  @Test
  fun `should send event if update success`() {
    whenever(phoneService.update(any(), any(), any(), any())).thenReturn(contactPhoneDetails)
    whenever(eventsService.send(any(), any(), any(), any(), any(), any(), any())).then {}
    val request = UpdatePhoneRequest(
      phoneType = "MOB",
      phoneNumber = "0777777777",
    )

    val result = facade.update(contactId, contactPhoneId, request, user)

    assertThat(result).isEqualTo(contactPhoneDetails)
    verify(phoneService).update(contactId, contactPhoneId, request, user)
    verify(eventsService).send(
      outboundEvent = OutboundEvent.CONTACT_PHONE_UPDATED,
      identifier = contactPhoneId,
      contactId = contactId,
      source = Source.DPS,
      user = user,
    )
  }

  @Test
  fun `should not send event if update throws exception and propagate the exception`() {
    val expectedException = RuntimeException("Bang!")
    whenever(phoneService.update(any(), any(), any(), any())).thenThrow(expectedException)
    whenever(eventsService.send(any(), any(), any(), any(), any(), any(), any())).then {}
    val request = UpdatePhoneRequest(
      phoneType = "MOB",
      phoneNumber = "0777777777",
    )

    val exception = assertThrows<RuntimeException> {
      facade.update(contactId, contactPhoneId, request, user)
    }

    assertThat(exception).isEqualTo(exception)
    verify(phoneService).update(contactId, contactPhoneId, request, user)
    verify(eventsService, never()).send(any(), any(), any(), any(), any(), any(), any())
  }

  @Test
  fun `should send event if delete success`() {
    whenever(phoneService.delete(any(), any())).then {}
    whenever(eventsService.send(any(), any(), any(), any(), any(), any(), any())).then {}

    facade.delete(contactId, contactPhoneId, user)

    verify(phoneService).delete(contactId, contactPhoneId)
    verify(eventsService).send(
      outboundEvent = OutboundEvent.CONTACT_PHONE_DELETED,
      identifier = contactPhoneId,
      contactId = contactId,
      source = Source.DPS,
      user = user,
    )
  }

  @Test
  fun `should not send event if delete throws exception and propagate the exception`() {
    val expectedException = RuntimeException("Bang!")
    whenever(phoneService.delete(any(), any())).thenThrow(expectedException)
    whenever(eventsService.send(any(), any(), any(), any(), any(), any(), any())).then {}

    val exception = assertThrows<RuntimeException> {
      facade.delete(contactId, contactPhoneId, user)
    }

    assertThat(exception).isEqualTo(exception)
    verify(phoneService).delete(contactId, contactPhoneId)
    verify(eventsService, never()).send(any(), any(), any(), any(), any(), any(), any())
  }

  @Test
  fun `should not send event on get`() {
    whenever(phoneService.get(any(), any())).thenReturn(contactPhoneDetails)

    val result = facade.get(contactId, contactPhoneId)

    assertThat(result).isEqualTo(contactPhoneDetails)
    verify(phoneService).get(contactId, contactPhoneId)
    verify(eventsService, never()).send(any(), any(), any(), any(), any(), any(), any())
  }
}
