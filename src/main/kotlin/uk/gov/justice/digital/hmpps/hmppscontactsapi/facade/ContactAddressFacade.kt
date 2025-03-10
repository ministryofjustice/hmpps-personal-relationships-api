package uk.gov.justice.digital.hmpps.hmppscontactsapi.facade

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.CreateContactAddressRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.PatchContactAddressRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.UpdateContactAddressRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.ContactAddressResponse
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.ContactAddressService
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.OutboundEventsService

@Service
class ContactAddressFacade(
  private val contactAddressService: ContactAddressService,
  private val outboundEventsService: OutboundEventsService,
) {

  fun create(contactId: Long, request: CreateContactAddressRequest): ContactAddressResponse = contactAddressService.create(contactId, request).also { (created, otherUpdatedAddressIds) ->
    outboundEventsService.send(
      outboundEvent = OutboundEvent.CONTACT_ADDRESS_CREATED,
      identifier = created.contactAddressId,
      contactId = contactId,
    )
    created.phoneNumberIds.map {
      outboundEventsService.send(
        outboundEvent = OutboundEvent.CONTACT_ADDRESS_PHONE_CREATED,
        identifier = it,
        secondIdentifier = created.contactAddressId,
        contactId = contactId,
      )
    }
    sendOtherUpdatedAddressEvents(otherUpdatedAddressIds, contactId)
  }.created

  fun update(contactId: Long, contactAddressId: Long, request: UpdateContactAddressRequest): ContactAddressResponse = contactAddressService.update(contactId, contactAddressId, request).also { (updated, otherUpdatedAddressIds) ->
    outboundEventsService.send(
      outboundEvent = OutboundEvent.CONTACT_ADDRESS_UPDATED,
      identifier = updated.contactAddressId,
      contactId = contactId,
    )
    sendOtherUpdatedAddressEvents(otherUpdatedAddressIds, contactId)
  }.updated

  fun patch(contactId: Long, contactAddressId: Long, request: PatchContactAddressRequest): ContactAddressResponse = contactAddressService.patch(contactId, contactAddressId, request).also { (updated, otherUpdatedAddressIds) ->
    outboundEventsService.send(
      outboundEvent = OutboundEvent.CONTACT_ADDRESS_UPDATED,
      identifier = updated.contactAddressId,
      contactId = contactId,
    )
    sendOtherUpdatedAddressEvents(otherUpdatedAddressIds, contactId)
  }.updated

  private fun sendOtherUpdatedAddressEvents(
    otherUpdatedAddressIds: Set<Long>,
    contactId: Long,
  ) {
    otherUpdatedAddressIds.forEach {
      outboundEventsService.send(
        outboundEvent = OutboundEvent.CONTACT_ADDRESS_UPDATED,
        identifier = it,
        contactId = contactId,
      )
    }
  }

  fun delete(contactId: Long, contactAddressId: Long) {
    contactAddressService.delete(contactId, contactAddressId).also {
      outboundEventsService.send(
        outboundEvent = OutboundEvent.CONTACT_ADDRESS_DELETED,
        identifier = it.contactAddressId,
        contactId = contactId,
      )
    }
  }

  fun get(contactId: Long, contactAddressId: Long) = contactAddressService.get(contactId, contactAddressId)
}
