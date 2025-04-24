package uk.gov.justice.digital.hmpps.hmppscontactsapi.facade

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppscontactsapi.config.User
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.phone.CreateMultiplePhoneNumbersRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.phone.CreatePhoneRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.phone.UpdatePhoneRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.ContactPhoneDetails
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.ContactPhoneService
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.OutboundEventsService

@Service
class ContactPhoneFacade(
  private val contactPhoneService: ContactPhoneService,
  private val outboundEventsService: OutboundEventsService,
) {

  fun create(contactId: Long, request: CreatePhoneRequest, user: User): ContactPhoneDetails = contactPhoneService.create(contactId, request, user).also {
    outboundEventsService.send(
      outboundEvent = OutboundEvent.CONTACT_PHONE_CREATED,
      identifier = it.contactPhoneId,
      contactId = contactId,
      user = user,
    )
  }

  fun createMultiple(contactId: Long, request: CreateMultiplePhoneNumbersRequest, user: User): List<ContactPhoneDetails> = contactPhoneService.createMultiple(
    contactId,
    user.username,
    request.phoneNumbers,
  ).onEach {
    outboundEventsService.send(
      outboundEvent = OutboundEvent.CONTACT_PHONE_CREATED,
      identifier = it.contactPhoneId,
      contactId = contactId,
      user = user,
    )
  }

  fun get(contactId: Long, contactPhoneId: Long): ContactPhoneDetails? = contactPhoneService.get(contactId, contactPhoneId)

  fun update(contactId: Long, contactPhoneId: Long, request: UpdatePhoneRequest, user: User): ContactPhoneDetails = contactPhoneService.update(contactId, contactPhoneId, request, user).also {
    outboundEventsService.send(
      outboundEvent = OutboundEvent.CONTACT_PHONE_UPDATED,
      identifier = contactPhoneId,
      contactId = contactId,
      user = user,
    )
  }

  fun delete(contactId: Long, contactPhoneId: Long, user: User) {
    contactPhoneService.delete(contactId, contactPhoneId).also {
      outboundEventsService.send(
        outboundEvent = OutboundEvent.CONTACT_PHONE_DELETED,
        identifier = contactPhoneId,
        contactId = contactId,
        user = user,
      )
    }
  }
}
