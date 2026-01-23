package uk.gov.justice.digital.hmpps.personalrelationships.facade

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.personalrelationships.config.User
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.phone.CreateContactAddressPhoneRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.phone.CreateMultiplePhoneNumbersRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.phone.UpdateContactAddressPhoneRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.ContactAddressPhoneDetails
import uk.gov.justice.digital.hmpps.personalrelationships.service.ContactAddressPhoneService
import uk.gov.justice.digital.hmpps.personalrelationships.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.personalrelationships.service.events.OutboundEventsService

@Service
class ContactAddressPhoneFacade(
  private val contactAddressPhoneService: ContactAddressPhoneService,
  private val outboundEventsService: OutboundEventsService,
) {

  fun create(contactId: Long, contactAddressId: Long, request: CreateContactAddressPhoneRequest, user: User): ContactAddressPhoneDetails = contactAddressPhoneService.create(contactId, contactAddressId, request, user).also {
    outboundEventsService.send(
      outboundEvent = OutboundEvent.CONTACT_ADDRESS_PHONE_CREATED,
      identifier = it.contactAddressPhoneId,
      secondIdentifier = it.contactAddressId,
      contactId = contactId,
      user = user,
    )
  }

  fun createMultiple(contactId: Long, contactAddressId: Long, request: CreateMultiplePhoneNumbersRequest, user: User): List<ContactAddressPhoneDetails> = contactAddressPhoneService.createMultiple(contactId, contactAddressId, request, user).also { created ->
    created.map {
      outboundEventsService.send(
        outboundEvent = OutboundEvent.CONTACT_ADDRESS_PHONE_CREATED,
        identifier = it.contactAddressPhoneId,
        secondIdentifier = it.contactAddressId,
        contactId = contactId,
        user = user,
      )
    }
  }

  fun update(contactId: Long, contactAddressPhoneId: Long, request: UpdateContactAddressPhoneRequest, user: User): ContactAddressPhoneDetails = contactAddressPhoneService.update(contactId, contactAddressPhoneId, request, user).also {
    outboundEventsService.send(
      outboundEvent = OutboundEvent.CONTACT_ADDRESS_PHONE_UPDATED,
      identifier = it.contactAddressPhoneId,
      secondIdentifier = it.contactAddressId,
      contactId = contactId,
      user = user,
    )
  }

  fun delete(contactId: Long, contactAddressPhoneId: Long, user: User) {
    contactAddressPhoneService.delete(contactId, contactAddressPhoneId).also {
      outboundEventsService.send(
        outboundEvent = OutboundEvent.CONTACT_ADDRESS_PHONE_DELETED,
        identifier = it.contactAddressPhoneId,
        secondIdentifier = it.contactAddressId,
        contactId = contactId,
        user = user,
      )
    }
  }

  fun get(contactId: Long, contactAddressPhoneId: Long) = contactAddressPhoneService.get(contactId, contactAddressPhoneId)
}
