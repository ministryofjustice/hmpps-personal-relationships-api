package uk.gov.justice.digital.hmpps.hmppscontactsapi.facade

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
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

  fun create(contactId: Long, request: CreateEmailRequest): ContactEmailDetails = contactEmailService.create(contactId, request).also {
    outboundEventsService.send(
      outboundEvent = OutboundEvent.CONTACT_EMAIL_CREATED,
      identifier = it.contactEmailId,
      contactId = contactId,
    )
  }

  fun createMultiple(contactId: Long, request: CreateMultipleEmailsRequest): List<ContactEmailDetails> = contactEmailService.createMultiple(
    contactId,
    request.createdBy,
    request.emailAddresses,
  ).also { created ->
    created.forEach {
      outboundEventsService.send(
        outboundEvent = OutboundEvent.CONTACT_EMAIL_CREATED,
        identifier = it.contactEmailId,
        contactId = contactId,
      )
    }
  }

  fun update(contactId: Long, contactEmailId: Long, request: UpdateEmailRequest): ContactEmailDetails = contactEmailService.update(contactId, contactEmailId, request).also {
    outboundEventsService.send(
      outboundEvent = OutboundEvent.CONTACT_EMAIL_UPDATED,
      identifier = contactEmailId,
      contactId = contactId,
    )
  }

  fun delete(contactId: Long, contactEmailId: Long) {
    contactEmailService.delete(contactId, contactEmailId).also {
      outboundEventsService.send(
        outboundEvent = OutboundEvent.CONTACT_EMAIL_DELETED,
        identifier = contactEmailId,
        contactId = contactId,
      )
    }
  }

  fun get(contactId: Long, contactEmailId: Long): ContactEmailDetails = contactEmailService.get(contactId, contactEmailId) ?: throw EntityNotFoundException("Contact email with id ($contactEmailId) not found for contact ($contactId)")
}
