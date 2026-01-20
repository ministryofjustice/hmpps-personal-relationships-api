package uk.gov.justice.digital.hmpps.personalrelationships.facade

import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.personalrelationships.helpers.aUser
import uk.gov.justice.digital.hmpps.personalrelationships.helpers.createContactAddressPhoneDetails
import uk.gov.justice.digital.hmpps.personalrelationships.helpers.createContactAddressPhoneRequest
import uk.gov.justice.digital.hmpps.personalrelationships.helpers.updateContactAddressPhoneRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.phone.CreateMultiplePhoneNumbersRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.phone.PhoneNumber
import uk.gov.justice.digital.hmpps.personalrelationships.service.ContactAddressPhoneService
import uk.gov.justice.digital.hmpps.personalrelationships.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.personalrelationships.service.events.OutboundEventsService
import uk.gov.justice.digital.hmpps.personalrelationships.service.events.Source

class ContactAddressPhoneFacadeTest {
  private val addressPhoneService: ContactAddressPhoneService = mock()
  private val eventsService: OutboundEventsService = mock()

  private val facade = ContactAddressPhoneFacade(addressPhoneService, eventsService)

  private val contactId = 1L
  private val contactAddressId = 2L
  private val contactPhoneId = 3L
  private val contactAddressPhoneId = 4L
  private val user = aUser()

  @Test
  fun `should send event if create success`() {
    val request = createContactAddressPhoneRequest(contactAddressId)
    val response = createContactAddressPhoneDetails(contactAddressPhoneId, contactAddressId, contactPhoneId, contactId)

    whenever(addressPhoneService.create(any(), any(), any(), any())).thenReturn(response)
    whenever(eventsService.send(any(), any(), any(), any(), any(), any(), any())).then {}

    val result = facade.create(contactId, contactAddressId, request, user)

    assertThat(result).isEqualTo(response)
    verify(addressPhoneService).create(contactId, contactAddressId, request, user)
    verify(eventsService).send(
      outboundEvent = OutboundEvent.CONTACT_ADDRESS_PHONE_CREATED,
      identifier = contactAddressPhoneId,
      secondIdentifier = contactAddressId,
      contactId = contactId,
      source = Source.DPS,
      user = user,
    )
  }

  @Test
  fun `should not send event if create throws exception and propagate the exception`() {
    val expectedException = RuntimeException("Bang!")
    val request = createContactAddressPhoneRequest(contactAddressId)

    whenever(addressPhoneService.create(any(), any(), any(), any())).thenThrow(expectedException)
    whenever(eventsService.send(any(), any(), any(), any(), any(), any(), any())).then {}

    val exception = assertThrows<RuntimeException> {
      facade.create(contactId, contactAddressId, request, user)
    }

    assertThat(exception.message).isEqualTo(expectedException.message)

    verify(addressPhoneService).create(contactId, contactAddressId, request, user)
    verify(eventsService, never()).send(any(), any(), any(), any(), any(), any(), any())
  }

  @Test
  fun `should send event for all created`() {
    val request = CreateMultiplePhoneNumbersRequest(
      listOf(
        PhoneNumber(
          "MOB",
          "+447777777777",
          "0123",
        ),
        PhoneNumber(
          phoneType = "HOME",
          phoneNumber = "01234 567890",
          extNumber = null,
        ),
      ),
    )

    val response = listOf(
      createContactAddressPhoneDetails(9999, contactAddressId, 6666, contactId),
      createContactAddressPhoneDetails(8888, contactAddressId, 5555, contactId),
    )

    whenever(addressPhoneService.createMultiple(any(), any(), any(), any())).thenReturn(response)
    whenever(eventsService.send(any(), any(), any(), any(), any(), any(), any())).then {}

    val result = facade.createMultiple(contactId, contactAddressId, request, user)

    assertThat(result).isEqualTo(response)
    verify(addressPhoneService).createMultiple(contactId, contactAddressId, request, user)
    verify(eventsService).send(
      outboundEvent = OutboundEvent.CONTACT_ADDRESS_PHONE_CREATED,
      identifier = 9999,
      secondIdentifier = contactAddressId,
      contactId = contactId,
      source = Source.DPS,
      user = user,
    )
    verify(eventsService).send(
      outboundEvent = OutboundEvent.CONTACT_ADDRESS_PHONE_CREATED,
      identifier = 8888,
      secondIdentifier = contactAddressId,
      contactId = contactId,
      source = Source.DPS,
      user = user,
    )
  }

  @Test
  fun `should send event if update success`() {
    val response = createContactAddressPhoneDetails(contactAddressPhoneId, contactAddressId, contactPhoneId, contactId)
    val request = updateContactAddressPhoneRequest()

    whenever(addressPhoneService.update(any(), any(), any(), any())).thenReturn(response)
    whenever(eventsService.send(any(), any(), any(), any(), any(), any(), any())).then {}

    val result = facade.update(
      contactId = contactId,
      contactAddressPhoneId = contactAddressPhoneId,
      request = request,
      user = user,
    )

    assertThat(result).isEqualTo(response)
    verify(addressPhoneService).update(contactId, contactAddressPhoneId, request, user)
    verify(eventsService).send(
      outboundEvent = OutboundEvent.CONTACT_ADDRESS_PHONE_UPDATED,
      identifier = contactAddressPhoneId,
      secondIdentifier = contactAddressId,
      contactId = contactId,
      source = Source.DPS,
      user = user,
    )
  }

  @Test
  fun `should not send event if update address throws exception`() {
    val expectedException = RuntimeException("Bang!")
    val request = updateContactAddressPhoneRequest()

    whenever(addressPhoneService.update(any(), any(), any(), any())).thenThrow(expectedException)
    whenever(eventsService.send(any(), any(), any(), any(), any(), any(), any())).then {}

    val exception = assertThrows<RuntimeException> {
      facade.update(contactId, contactAddressPhoneId, request, user)
    }

    assertThat(exception).isEqualTo(exception)
    verify(addressPhoneService).update(contactId, contactAddressPhoneId, request, user)
    verify(eventsService, never()).send(any(), any(), any(), any(), any(), any(), any())
  }

  @Test
  fun `should not send an event on get requests`() {
    val response = createContactAddressPhoneDetails(contactAddressPhoneId, contactAddressId, contactPhoneId, contactId)
    whenever(addressPhoneService.get(any(), any())).thenReturn(response)

    val result = facade.get(contactId, contactAddressPhoneId)

    assertThat(result).isEqualTo(response)
    verify(addressPhoneService).get(contactId, contactAddressPhoneId)
    verify(eventsService, never()).send(any(), any(), any(), any(), any(), any(), any())
  }

  @Test
  fun `should throw exception if not found`() {
    val expectedException = EntityNotFoundException("Bang!")
    whenever(addressPhoneService.get(any(), any())).thenThrow(expectedException)

    val exception = assertThrows<EntityNotFoundException> {
      facade.get(contactId, contactAddressPhoneId)
    }

    assertThat(exception.message).isEqualTo(expectedException.message)
    verify(addressPhoneService).get(contactId, contactAddressPhoneId)
    verify(eventsService, never()).send(any(), any(), any(), any(), any(), any(), any())
  }

  @Test
  fun `should send event if delete success`() {
    val response = createContactAddressPhoneDetails(contactAddressPhoneId, contactAddressId, contactPhoneId, contactId)
    whenever(addressPhoneService.delete(any(), any())).thenReturn(response)
    whenever(eventsService.send(any(), any(), any(), any(), any(), any(), any())).then {}

    facade.delete(contactId, contactAddressPhoneId, user)

    verify(addressPhoneService).delete(contactId, contactAddressPhoneId)
    verify(eventsService).send(
      outboundEvent = OutboundEvent.CONTACT_ADDRESS_PHONE_DELETED,
      identifier = contactAddressPhoneId,
      secondIdentifier = contactAddressId,
      contactId = contactId,
      source = Source.DPS,
      user = user,
    )
  }

  @Test
  fun `should not send event if delete throws exception`() {
    val expectedException = RuntimeException("Bang!")
    whenever(addressPhoneService.delete(any(), any())).thenThrow(expectedException)
    whenever(eventsService.send(any(), any(), any(), any(), any(), any(), any())).then {}

    val exception = assertThrows<RuntimeException> {
      facade.delete(contactId, contactAddressPhoneId, user)
    }

    assertThat(exception.message).isEqualTo(expectedException.message)
    verify(addressPhoneService).delete(contactId, contactAddressPhoneId)
    verify(eventsService, never()).send(any(), any(), any(), any(), any(), any(), any())
  }
}
