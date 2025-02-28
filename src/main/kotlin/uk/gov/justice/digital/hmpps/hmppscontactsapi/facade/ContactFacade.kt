package uk.gov.justice.digital.hmpps.hmppscontactsapi.facade

import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.AddContactRelationshipRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.ContactSearchRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.CreateContactRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.PatchContactRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.PatchRelationshipRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.ContactCreationResult
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.ContactDetails
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.ContactNameDetails
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.ContactSearchResultItem
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.PatchContactResponse
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.PrisonerContactRelationshipDetails
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.ContactPatchService
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.ContactService
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.OutboundEventsService

@Service
class ContactFacade(
  private val outboundEventsService: OutboundEventsService,
  private val contactPatchService: ContactPatchService,
  private val contactService: ContactService,
) {
  companion object {
    private val logger = LoggerFactory.getLogger(this::class.java)
  }

  fun createContact(request: CreateContactRequest): ContactCreationResult = contactService.createContact(request)
    .also { creationResult ->
      // Send the contact created event
      outboundEventsService.send(
        outboundEvent = OutboundEvent.CONTACT_CREATED,
        identifier = creationResult.createdContact.id,
        contactId = creationResult.createdContact.id,
      )

      // Send the prisons contact created event
      creationResult.createdRelationship?.let {
        outboundEventsService.send(
          outboundEvent = OutboundEvent.PRISONER_CONTACT_CREATED,
          identifier = it.prisonerContactId,
          contactId = creationResult.createdContact.id,
          noms = request.relationship?.prisonerNumber.let { request.relationship!!.prisonerNumber },
        )
      }
    }

  fun addContactRelationship(request: AddContactRelationshipRequest): PrisonerContactRelationshipDetails {
    val createdRelationship = contactService.addContactRelationship(request)
    outboundEventsService.send(
      outboundEvent = OutboundEvent.PRISONER_CONTACT_CREATED,
      identifier = createdRelationship.prisonerContactId,
      contactId = createdRelationship.contactId,
      noms = request.relationship.prisonerNumber,
    )
    return createdRelationship
  }

  fun patch(id: Long, request: PatchContactRequest): PatchContactResponse = contactPatchService.patch(id, request)
    .also {
      logger.info("Send patch domain event to {} {} ", OutboundEvent.CONTACT_UPDATED, id)
      outboundEventsService.send(
        outboundEvent = OutboundEvent.CONTACT_UPDATED,
        identifier = id,
        contactId = id,
      )
    }

  fun getContact(id: Long): ContactDetails? = contactService.getContact(id)

  fun getContactName(id: Long): ContactNameDetails? = contactService.getContactName(id)

  fun searchContacts(pageable: Pageable, request: ContactSearchRequest): Page<ContactSearchResultItem> = contactService.searchContacts(pageable, request)

  fun patchRelationship(prisonerContactId: Long, request: PatchRelationshipRequest) {
    contactService.updateContactRelationship(prisonerContactId, request)
      .also {
        logger.info("Send patch relationship domain event to {} {} ", OutboundEvent.PRISONER_CONTACT_UPDATED, it.contactId)
        outboundEventsService.send(
          outboundEvent = OutboundEvent.PRISONER_CONTACT_UPDATED,
          identifier = it.prisonerContactId,
          contactId = it.contactId,
          noms = it.prisonerNumber,
        )
      }
  }
}
