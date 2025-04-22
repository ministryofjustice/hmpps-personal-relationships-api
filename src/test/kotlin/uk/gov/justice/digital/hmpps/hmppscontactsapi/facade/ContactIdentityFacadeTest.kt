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
import uk.gov.justice.digital.hmpps.hmppscontactsapi.helpers.aUser
import uk.gov.justice.digital.hmpps.hmppscontactsapi.helpers.createContactIdentityDetails
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.identity.CreateIdentityRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.identity.CreateMultipleIdentitiesRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.identity.IdentityDocument
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.identity.UpdateIdentityRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.ContactIdentityService
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.OutboundEventsService
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.Source

class ContactIdentityFacadeTest {

  private val identityService: ContactIdentityService = mock()
  private val eventsService: OutboundEventsService = mock()
  private val facade = ContactIdentityFacade(identityService, eventsService)

  private val contactId = 11L
  private val contactIdentityId = 99L
  private val contactIdentityDetails = createContactIdentityDetails(id = contactIdentityId, contactId = contactId)

  private val user = aUser()

  @Test
  fun `should send event if create success`() {
    whenever(identityService.create(any(), any(), any())).thenReturn(contactIdentityDetails)
    whenever(eventsService.send(any(), any(), any(), any(), any(), any(), any())).then {}
    val request = CreateIdentityRequest(
      identityType = "DL",
      identityValue = "DL123456789",
    )

    val result = facade.create(contactId, request, user)

    assertThat(result).isEqualTo(contactIdentityDetails)
    verify(identityService).create(contactId, request, user)
    verify(eventsService).send(
      outboundEvent = OutboundEvent.CONTACT_IDENTITY_CREATED,
      identifier = contactIdentityId,
      contactId = contactId,
      source = Source.DPS,
      user = user,
    )
  }

  @Test
  fun `should not send event if create throws exception and propagate the exception`() {
    val expectedException = RuntimeException("Bang!")
    whenever(identityService.create(any(), any(), any())).thenThrow(expectedException)
    whenever(eventsService.send(any(), any(), any(), any(), any(), any(), any())).then {}
    val request = CreateIdentityRequest(
      identityType = "DL",
      identityValue = "DL123456789",
    )

    val exception = assertThrows<RuntimeException> {
      facade.create(contactId, request, user)
    }

    assertThat(exception).isEqualTo(exception)
    verify(identityService).create(contactId, request, user)
    verify(eventsService, never()).send(any(), any(), any(), any(), any(), any(), any())
  }

  @Test
  fun `should send event if create multiple success`() {
    val first = createContactIdentityDetails(id = 9999, contactId = contactId)
    val second = createContactIdentityDetails(id = 8888, contactId = contactId)

    whenever(identityService.createMultiple(any(), any(), any())).thenReturn(listOf(first, second))
    whenever(eventsService.send(any(), any(), any(), any(), any(), any(), any())).then {}
    val request = CreateMultipleIdentitiesRequest(
      identities = listOf(
        IdentityDocument(
          identityType = "DL",
          identityValue = "DL123456789",
          issuingAuthority = "DVLA",
        ),
        IdentityDocument(
          identityType = "PASS",
          identityValue = "P897654312",
          issuingAuthority = null,
        ),
      ),
    )

    val result = facade.createMultiple(contactId, request, user)

    assertThat(result).isEqualTo(listOf(first, second))
    verify(identityService).createMultiple(contactId, request, user)
    verify(eventsService).send(
      outboundEvent = OutboundEvent.CONTACT_IDENTITY_CREATED,
      identifier = 9999,
      contactId = contactId,
      source = Source.DPS,
      user = user,
    )
    verify(eventsService).send(
      outboundEvent = OutboundEvent.CONTACT_IDENTITY_CREATED,
      identifier = 8888,
      contactId = contactId,
      source = Source.DPS,
      user = user,
    )
  }

  @Test
  fun `should send event if update success`() {
    whenever(identityService.update(any(), any(), any(), any())).thenReturn(contactIdentityDetails)
    whenever(eventsService.send(any(), any(), any(), any(), any(), any(), any())).then {}
    val request = UpdateIdentityRequest(
      identityType = "PASS",
      identityValue = "P978654312",
    )

    val result = facade.update(contactId, contactIdentityId, request, user)

    assertThat(result).isEqualTo(contactIdentityDetails)
    verify(identityService).update(contactId, contactIdentityId, request, user)
    verify(eventsService).send(
      outboundEvent = OutboundEvent.CONTACT_IDENTITY_UPDATED,
      identifier = contactIdentityId,
      contactId = contactId,
      source = Source.DPS,
      user = user,
    )
  }

  @Test
  fun `should not send event if update throws exception and propagate the exception`() {
    val expectedException = RuntimeException("Bang!")
    whenever(identityService.update(any(), any(), any(), any())).thenThrow(expectedException)
    whenever(eventsService.send(any(), any(), any(), any(), any(), any(), any())).then {}
    val request = UpdateIdentityRequest(
      identityType = "PASS",
      identityValue = "P978654312",
    )

    val exception = assertThrows<RuntimeException> {
      facade.update(contactId, contactIdentityId, request, user)
    }

    assertThat(exception).isEqualTo(exception)
    verify(identityService).update(contactId, contactIdentityId, request, user)
    verify(eventsService, never()).send(any(), any(), any(), any(), any(), any(), any())
  }

  @Test
  fun `should not send event on get`() {
    whenever(identityService.get(any(), any())).thenReturn(contactIdentityDetails)

    val result = facade.get(contactId, contactIdentityId)

    assertThat(result).isEqualTo(contactIdentityDetails)
    verify(identityService).get(contactId, contactIdentityId)
    verify(eventsService, never()).send(any(), any(), any(), any(), any(), any(), any())
  }

  @Test
  fun `should throw exception if there is no identity found on get`() {
    whenever(identityService.get(any(), any())).thenReturn(null)

    val exception = assertThrows<EntityNotFoundException> {
      facade.get(contactId, contactIdentityId)
    }

    assertThat(exception.message).isEqualTo("Contact identity with id (99) not found for contact (11)")
    verify(identityService).get(contactId, contactIdentityId)
    verify(eventsService, never()).send(any(), any(), any(), any(), any(), any(), any())
  }

  @Test
  fun `should send event if delete success`() {
    whenever(identityService.delete(any(), any())).then {}
    whenever(eventsService.send(any(), any(), any(), any(), any(), any(), any())).then {}

    facade.delete(contactId, contactIdentityId, user)

    verify(identityService).delete(contactId, contactIdentityId)
    verify(eventsService).send(
      outboundEvent = OutboundEvent.CONTACT_IDENTITY_DELETED,
      identifier = contactIdentityId,
      contactId = contactId,
      source = Source.DPS,
      user = user,
    )
  }

  @Test
  fun `should not send event if delete throws exception and propagate the exception`() {
    val expectedException = RuntimeException("Bang!")
    whenever(identityService.delete(any(), any())).thenThrow(expectedException)
    whenever(eventsService.send(any(), any(), any(), any(), any(), any(), any())).then {}

    val exception = assertThrows<RuntimeException> {
      facade.delete(contactId, contactIdentityId, user)
    }

    assertThat(exception).isEqualTo(exception)
    verify(identityService).delete(contactId, contactIdentityId)
    verify(eventsService, never()).send(any(), any(), any(), any(), any(), any(), any())
  }
}
