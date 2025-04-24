package uk.gov.justice.digital.hmpps.hmppscontactsapi.facade

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppscontactsapi.config.User
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.email.CreateEmailRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.email.CreateMultipleEmailsRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.email.UpdateEmailRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.ContactEmailDetails
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.ContactEmailService
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.OutboundEventsService

@Service
class ContactEmailFacade(
  private val contactEmailService: ContactEmailService,
  private val outboundEventsService: OutboundEventsService,
) {

  fun create(contactId: Long, request: CreateEmailRequest, user: User): ContactEmailDetails = contactEmailService.create(contactId, request, user).also {
    outboundEventsService.send(
      outboundEvent = OutboundEvent.CONTACT_EMAIL_CREATED,
      identifier = it.contactEmailId,
      contactId = contactId,
      user = user,
    )
  }

  fun createMultiple(contactId: Long, request: CreateMultipleEmailsRequest, user: User): List<ContactEmailDetails> = contactEmailService.createMultiple(
    contactId,
    user.username,
    request.emailAddresses,
  ).onEach {
    outboundEventsService.send(
      outboundEvent = OutboundEvent.CONTACT_EMAIL_CREATED,
      identifier = it.contactEmailId,
      contactId = contactId,
      user = user,
    )
  }

  fun update(contactId: Long, contactEmailId: Long, request: UpdateEmailRequest, user: User): ContactEmailDetails = contactEmailService.update(contactId, contactEmailId, request, user).also {
    outboundEventsService.send(
      outboundEvent = OutboundEvent.CONTACT_EMAIL_UPDATED,
      identifier = contactEmailId,
      contactId = contactId,
      user = user,
    )
  }

  fun delete(contactId: Long, contactEmailId: Long, user: User) {
    contactEmailService.delete(contactId, contactEmailId).also {
      outboundEventsService.send(
        outboundEvent = OutboundEvent.CONTACT_EMAIL_DELETED,
        identifier = contactEmailId,
        contactId = contactId,
        user = user,
      )
    }
  }

  fun get(contactId: Long, contactEmailId: Long): ContactEmailDetails = contactEmailService.get(contactId, contactEmailId) ?: throw EntityNotFoundException("Contact email with id ($contactEmailId) not found for contact ($contactId)")
}
