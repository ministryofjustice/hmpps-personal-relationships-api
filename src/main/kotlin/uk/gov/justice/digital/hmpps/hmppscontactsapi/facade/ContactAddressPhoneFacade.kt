package uk.gov.justice.digital.hmpps.hmppscontactsapi.facade

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.phone.CreateContactAddressPhoneRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.phone.CreateMultiplePhoneNumbersRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.phone.UpdateContactAddressPhoneRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.ContactAddressPhoneDetails
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.ContactAddressPhoneService
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.OutboundEventsService

@Service
class ContactAddressPhoneFacade(
  private val contactAddressPhoneService: ContactAddressPhoneService,
  private val outboundEventsService: OutboundEventsService,
) {

  fun create(contactId: Long, contactAddressId: Long, request: CreateContactAddressPhoneRequest): ContactAddressPhoneDetails = contactAddressPhoneService.create(contactId, contactAddressId, request).also {
    outboundEventsService.send(
      outboundEvent = OutboundEvent.CONTACT_ADDRESS_PHONE_CREATED,
      identifier = it.contactAddressPhoneId,
      secondIdentifier = it.contactAddressId,
      contactId = contactId,
    )
  }

  fun createMultiple(contactId: Long, contactAddressId: Long, request: CreateMultiplePhoneNumbersRequest): List<ContactAddressPhoneDetails> = contactAddressPhoneService.createMultiple(contactId, contactAddressId, request).also { created ->
    created.map {
      outboundEventsService.send(
        outboundEvent = OutboundEvent.CONTACT_ADDRESS_PHONE_CREATED,
        identifier = it.contactAddressPhoneId,
        secondIdentifier = it.contactAddressId,
        contactId = contactId,
      )
    }
  }

  fun update(contactId: Long, contactAddressPhoneId: Long, request: UpdateContactAddressPhoneRequest): ContactAddressPhoneDetails = contactAddressPhoneService.update(contactId, contactAddressPhoneId, request).also {
    outboundEventsService.send(
      outboundEvent = OutboundEvent.CONTACT_ADDRESS_PHONE_UPDATED,
      identifier = it.contactAddressPhoneId,
      secondIdentifier = it.contactAddressId,
      contactId = contactId,
    )
  }

  fun delete(contactId: Long, contactAddressPhoneId: Long) {
    contactAddressPhoneService.delete(contactId, contactAddressPhoneId).also {
      outboundEventsService.send(
        outboundEvent = OutboundEvent.CONTACT_ADDRESS_PHONE_DELETED,
        identifier = it.contactAddressPhoneId,
        secondIdentifier = it.contactAddressId,
        contactId = contactId,
      )
    }
  }

  fun get(contactId: Long, contactAddressPhoneId: Long) = contactAddressPhoneService.get(contactId, contactAddressPhoneId)
}
