package uk.gov.justice.digital.hmpps.personalrelationships.facade

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.personalrelationships.config.User
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.address.CreateContactAddressRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.address.PatchContactAddressRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.address.UpdateContactAddressRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.ContactAddressResponse
import uk.gov.justice.digital.hmpps.personalrelationships.service.ContactAddressService
import uk.gov.justice.digital.hmpps.personalrelationships.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.personalrelationships.service.events.OutboundEventsService

@Service
class ContactAddressFacade(
  private val contactAddressService: ContactAddressService,
  private val outboundEventsService: OutboundEventsService,
) {

  fun create(contactId: Long, request: CreateContactAddressRequest, user: User): ContactAddressResponse = contactAddressService.create(contactId, request, user).also { (created, otherUpdatedAddressIds) ->
    outboundEventsService.send(
      outboundEvent = OutboundEvent.CONTACT_ADDRESS_CREATED,
      identifier = created.contactAddressId,
      contactId = contactId,
      user = user,
    )
    created.phoneNumberIds.map {
      outboundEventsService.send(
        outboundEvent = OutboundEvent.CONTACT_ADDRESS_PHONE_CREATED,
        identifier = it,
        secondIdentifier = created.contactAddressId,
        contactId = contactId,
        user = user,
      )
    }
    sendOtherUpdatedAddressEvents(otherUpdatedAddressIds, contactId, user)
  }.created

  fun update(contactId: Long, contactAddressId: Long, request: UpdateContactAddressRequest, user: User): ContactAddressResponse = contactAddressService.update(contactId, contactAddressId, request, user).also { (updated, otherUpdatedAddressIds) ->
    outboundEventsService.send(
      outboundEvent = OutboundEvent.CONTACT_ADDRESS_UPDATED,
      identifier = updated.contactAddressId,
      contactId = contactId,
      user = user,
    )
    sendOtherUpdatedAddressEvents(otherUpdatedAddressIds, contactId, user)
  }.updated

  fun patch(contactId: Long, contactAddressId: Long, request: PatchContactAddressRequest, user: User): ContactAddressResponse = contactAddressService.patch(contactId, contactAddressId, request, user).also { (updated, otherUpdatedAddressIds) ->
    outboundEventsService.send(
      outboundEvent = OutboundEvent.CONTACT_ADDRESS_UPDATED,
      identifier = updated.contactAddressId,
      contactId = contactId,
      user = user,
    )
    sendOtherUpdatedAddressEvents(otherUpdatedAddressIds, contactId, user)
  }.updated

  private fun sendOtherUpdatedAddressEvents(
    otherUpdatedAddressIds: Set<Long>,
    contactId: Long,
    user: User,
  ) {
    otherUpdatedAddressIds.forEach {
      outboundEventsService.send(
        outboundEvent = OutboundEvent.CONTACT_ADDRESS_UPDATED,
        identifier = it,
        contactId = contactId,
        user = user,
      )
    }
  }

  fun delete(contactId: Long, contactAddressId: Long, user: User) {
    contactAddressService.delete(contactId, contactAddressId).also {
      outboundEventsService.send(
        outboundEvent = OutboundEvent.CONTACT_ADDRESS_DELETED,
        identifier = it.contactAddressId,
        contactId = contactId,
        user = user,
      )
    }
  }

  fun get(contactId: Long, contactAddressId: Long) = contactAddressService.get(contactId, contactAddressId)
}
