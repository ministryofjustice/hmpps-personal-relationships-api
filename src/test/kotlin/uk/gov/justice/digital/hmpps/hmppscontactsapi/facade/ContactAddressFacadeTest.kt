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
import uk.gov.justice.digital.hmpps.hmppscontactsapi.helpers.contactAddressResponse
import uk.gov.justice.digital.hmpps.hmppscontactsapi.helpers.createContactAddressRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.helpers.patchContactAddressRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.helpers.updateContactAddressRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.CreateAddressResponse
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.UpdateAddressResponse
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.ContactAddressService
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.OutboundEventsService
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.Source

class ContactAddressFacadeTest {
  private val addressService: ContactAddressService = mock()
  private val eventsService: OutboundEventsService = mock()

  private val facade = ContactAddressFacade(addressService, eventsService)

  private val contactId = 1L
  private val contactAddressId = 2L

  @Test
  fun `should send event if create success`() {
    val request = createContactAddressRequest()
    val addressSpecificPhoneId1 = 1L
    val addressSpecificPhoneId2 = 2L
    val response = contactAddressResponse(contactAddressId, contactId, phoneNumberIds = listOf(addressSpecificPhoneId1, addressSpecificPhoneId2))

    whenever(addressService.create(any(), any())).thenReturn(CreateAddressResponse(response, emptySet()))
    whenever(eventsService.send(any(), any(), any(), any(), any(), any())).then {}

    val result = facade.create(contactId, request)

    assertThat(result).isEqualTo(response)
    verify(addressService).create(contactId, request)
    verify(eventsService).send(
      outboundEvent = OutboundEvent.CONTACT_ADDRESS_CREATED,
      identifier = contactAddressId,
      contactId = contactId,
      source = Source.DPS,
    )

    verify(eventsService).send(
      outboundEvent = OutboundEvent.CONTACT_ADDRESS_PHONE_CREATED,
      identifier = addressSpecificPhoneId1,
      secondIdentifier = contactAddressId,
      contactId = contactId,
      source = Source.DPS,
    )
    verify(eventsService).send(
      outboundEvent = OutboundEvent.CONTACT_ADDRESS_PHONE_CREATED,
      identifier = addressSpecificPhoneId2,
      secondIdentifier = contactAddressId,
      contactId = contactId,
      source = Source.DPS,
    )
  }

  @Test
  fun `should not send event if create throws exception and propagate the exception`() {
    val expectedException = RuntimeException("Bang!")
    val request = createContactAddressRequest()

    whenever(addressService.create(any(), any())).thenThrow(expectedException)
    whenever(eventsService.send(any(), any(), any(), any(), any(), any())).then {}

    val exception = assertThrows<RuntimeException> {
      facade.create(contactId, request)
    }

    assertThat(exception.message).isEqualTo(expectedException.message)

    verify(addressService).create(contactId, request)
    verify(eventsService, never()).send(any(), any(), any(), any(), any(), any())
  }

  @Test
  fun `should send event if update success`() {
    val response = contactAddressResponse(contactAddressId, contactId)
    val request = updateContactAddressRequest()

    whenever(addressService.update(any(), any(), any())).thenReturn(UpdateAddressResponse(response, emptySet()))
    whenever(eventsService.send(any(), any(), any(), any(), any(), any())).then {}

    val result = facade.update(
      contactId = contactId,
      contactAddressId = contactAddressId,
      request = request,
    )

    assertThat(result).isEqualTo(response)
    verify(addressService).update(contactId, contactAddressId, request)
    verify(eventsService).send(
      outboundEvent = OutboundEvent.CONTACT_ADDRESS_UPDATED,
      identifier = contactAddressId,
      contactId = contactId,
      source = Source.DPS,
    )
  }

  @Test
  fun `should not send event if update address throws exception`() {
    val expectedException = RuntimeException("Bang!")
    val request = updateContactAddressRequest()

    whenever(addressService.update(any(), any(), any())).thenThrow(expectedException)
    whenever(eventsService.send(any(), any(), any(), any(), any(), any())).then {}

    val exception = assertThrows<RuntimeException> {
      facade.update(contactId, contactAddressId, request)
    }

    assertThat(exception).isEqualTo(exception)
    verify(addressService).update(contactId, contactAddressId, request)
    verify(eventsService, never()).send(any(), any(), any(), any(), any(), any())
  }

  @Test
  fun `should send event if patch success`() {
    val response = contactAddressResponse(contactAddressId, contactId)
    val request = patchContactAddressRequest()

    whenever(addressService.patch(any(), any(), any())).thenReturn(UpdateAddressResponse(response, emptySet()))
    whenever(eventsService.send(any(), any(), any(), any(), any(), any())).then {}

    val result = facade.patch(
      contactId = contactId,
      contactAddressId = contactAddressId,
      request = request,
    )

    assertThat(result).isEqualTo(response)
    verify(addressService).patch(contactId, contactAddressId, request)
    verify(eventsService).send(
      outboundEvent = OutboundEvent.CONTACT_ADDRESS_UPDATED,
      identifier = contactAddressId,
      contactId = contactId,
      source = Source.DPS,
    )
  }

  @Test
  fun `should not send event if patch address throws exception`() {
    val expectedException = RuntimeException("Bang!")
    val request = patchContactAddressRequest()

    whenever(addressService.patch(any(), any(), any())).thenThrow(expectedException)
    whenever(eventsService.send(any(), any(), any(), any(), any(), any())).then {}

    val exception = assertThrows<RuntimeException> {
      facade.patch(contactId, contactAddressId, request)
    }

    assertThat(exception).isEqualTo(exception)
    verify(addressService).patch(contactId, contactAddressId, request)
    verify(eventsService, never()).send(any(), any(), any(), any(), any(), any())
  }

  @Test
  fun `should not send an event on get requests`() {
    val response = contactAddressResponse(contactAddressId, contactId)
    whenever(addressService.get(any(), any())).thenReturn(response)

    val result = facade.get(contactId, contactAddressId)

    assertThat(result).isEqualTo(response)
    verify(addressService).get(contactId, contactAddressId)
    verify(eventsService, never()).send(any(), any(), any(), any(), any(), any())
  }

  @Test
  fun `should throw exception if not found`() {
    val expectedException = EntityNotFoundException("Bang!")
    whenever(addressService.get(any(), any())).thenThrow(expectedException)

    val exception = assertThrows<EntityNotFoundException> {
      facade.get(contactId, contactAddressId)
    }

    assertThat(exception.message).isEqualTo(expectedException.message)
    verify(addressService).get(contactId, contactAddressId)
    verify(eventsService, never()).send(any(), any(), any(), any(), any(), any())
  }

  @Test
  fun `should send event if delete success`() {
    val response = contactAddressResponse(contactAddressId, contactId)
    whenever(addressService.delete(any(), any())).thenReturn(response)
    whenever(eventsService.send(any(), any(), any(), any(), any(), any())).then {}

    facade.delete(contactId, contactAddressId)

    verify(addressService).delete(contactId, contactAddressId)
    verify(eventsService).send(OutboundEvent.CONTACT_ADDRESS_DELETED, contactAddressId, contactId)
  }

  @Test
  fun `should not send event if delete throws exception`() {
    val expectedException = RuntimeException("Bang!")
    whenever(addressService.delete(any(), any())).thenThrow(expectedException)
    whenever(eventsService.send(any(), any(), any(), any(), any(), any())).then {}

    val exception = assertThrows<RuntimeException> {
      facade.delete(contactId, contactAddressId)
    }

    assertThat(exception.message).isEqualTo(expectedException.message)
    verify(addressService).delete(contactId, contactAddressId)
    verify(eventsService, never()).send(any(), any(), any(), any(), any(), any())
  }
}
