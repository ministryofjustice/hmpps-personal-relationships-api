package uk.gov.justice.digital.hmpps.hmppscontactsapi.facade

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppscontactsapi.config.User
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.identity.CreateIdentityRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.identity.CreateMultipleIdentitiesRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.identity.UpdateIdentityRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.ContactIdentityDetails
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.ContactIdentityService
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.OutboundEventsService

@Service
class ContactIdentityFacade(
  private val contactIdentityService: ContactIdentityService,
  private val outboundEventsService: OutboundEventsService,
) {

  fun create(contactId: Long, request: CreateIdentityRequest, user: User): ContactIdentityDetails = contactIdentityService.create(contactId, request, user).also {
    outboundEventsService.send(
      outboundEvent = OutboundEvent.CONTACT_IDENTITY_CREATED,
      identifier = it.contactIdentityId,
      contactId = contactId,
      user = user,
    )
  }

  fun createMultiple(contactId: Long, request: CreateMultipleIdentitiesRequest, user: User): List<ContactIdentityDetails> = contactIdentityService.createMultiple(contactId, request, user).onEach {
    outboundEventsService.send(
      outboundEvent = OutboundEvent.CONTACT_IDENTITY_CREATED,
      identifier = it.contactIdentityId,
      contactId = contactId,
      user = user,
    )
  }

  fun update(contactId: Long, contactIdentityId: Long, request: UpdateIdentityRequest, user: User): ContactIdentityDetails = contactIdentityService.update(contactId, contactIdentityId, request, user).also {
    outboundEventsService.send(
      outboundEvent = OutboundEvent.CONTACT_IDENTITY_UPDATED,
      identifier = contactIdentityId,
      contactId = contactId,
      user = user,
    )
  }

  fun get(contactId: Long, contactIdentityId: Long): ContactIdentityDetails = contactIdentityService.get(contactId, contactIdentityId) ?: throw EntityNotFoundException("Contact identity with id ($contactIdentityId) not found for contact ($contactId)")

  fun delete(contactId: Long, contactIdentityId: Long, user: User) {
    contactIdentityService.delete(contactId, contactIdentityId).also {
      outboundEventsService.send(
        outboundEvent = OutboundEvent.CONTACT_IDENTITY_DELETED,
        identifier = contactIdentityId,
        contactId = contactId,
        user = user,
      )
    }
  }
}
