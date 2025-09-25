package uk.gov.justice.digital.hmpps.hmppscontactsapi.facade

import org.openapitools.jackson.nullable.JsonNullable
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PagedModel
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppscontactsapi.config.User
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
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.ContactSearchService
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.ContactService
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.OutboundEventsService
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.Source
import java.time.LocalDateTime

@Service
class ContactFacade(
  private val outboundEventsService: OutboundEventsService,
  private val contactPatchService: ContactPatchService,
  private val contactService: ContactService,
  private val contactSearchService: ContactSearchService,
) {
  companion object {
    private val logger = LoggerFactory.getLogger(this::class.java)
  }

  fun createContact(request: CreateContactRequest, user: User): ContactCreationResult = contactService.createContact(request, user)
    .also { creationResult ->
      outboundEventsService.send(
        outboundEvent = OutboundEvent.CONTACT_CREATED,
        identifier = creationResult.createdContact.id,
        contactId = creationResult.createdContact.id,
        user = user,
      )

      creationResult.createdRelationship?.let {
        outboundEventsService.send(
          outboundEvent = OutboundEvent.PRISONER_CONTACT_CREATED,
          identifier = it.prisonerContactId,
          contactId = creationResult.createdContact.id,
          noms = request.relationship?.prisonerNumber.let { request.relationship!!.prisonerNumber },
          user = user,
        )
      }

      creationResult.createdContact.identities.forEach {
        outboundEventsService.send(
          outboundEvent = OutboundEvent.CONTACT_IDENTITY_CREATED,
          identifier = it.contactIdentityId,
          contactId = creationResult.createdContact.id,
          user = user,
        )
      }

      creationResult.createdContact.addresses.forEach { createdAddress ->
        outboundEventsService.send(
          outboundEvent = OutboundEvent.CONTACT_ADDRESS_CREATED,
          identifier = createdAddress.contactAddressId,
          contactId = creationResult.createdContact.id,
          user = user,
        )
        createdAddress.phoneNumbers.forEach {
          outboundEventsService.send(
            outboundEvent = OutboundEvent.CONTACT_ADDRESS_PHONE_CREATED,
            identifier = it.contactAddressPhoneId,
            secondIdentifier = it.contactAddressId,
            contactId = creationResult.createdContact.id,
            user = user,
          )
        }
      }

      creationResult.createdContact.phoneNumbers.forEach {
        outboundEventsService.send(
          outboundEvent = OutboundEvent.CONTACT_PHONE_CREATED,
          identifier = it.contactPhoneId,
          contactId = creationResult.createdContact.id,
          user = user,
        )
      }

      creationResult.createdContact.emailAddresses.forEach {
        outboundEventsService.send(
          outboundEvent = OutboundEvent.CONTACT_EMAIL_CREATED,
          identifier = it.contactEmailId,
          contactId = creationResult.createdContact.id,
          user = user,
        )
      }

      creationResult.createdContact.employments.forEach {
        outboundEventsService.send(
          outboundEvent = OutboundEvent.EMPLOYMENT_CREATED,
          identifier = it.employmentId,
          contactId = creationResult.createdContact.id,
          user = user,
        )
      }
    }

  fun addContactRelationship(request: AddContactRelationshipRequest, user: User): PrisonerContactRelationshipDetails {
    val createdRelationship = contactService.addContactRelationship(request, user)
    outboundEventsService.send(
      outboundEvent = OutboundEvent.PRISONER_CONTACT_CREATED,
      identifier = createdRelationship.prisonerContactId,
      contactId = createdRelationship.contactId,
      noms = request.relationship.prisonerNumber,
      user = user,
    )
    return createdRelationship
  }

  fun patch(id: Long, request: PatchContactRequest, user: User): PatchContactResponse = contactPatchService.patch(id, request, user)
    .also {
      logger.info("Send patch domain event to {} {} ", OutboundEvent.CONTACT_UPDATED, id)
      outboundEventsService.send(
        outboundEvent = OutboundEvent.CONTACT_UPDATED,
        identifier = id,
        contactId = id,
        user = user,
      )
    }

  fun getContact(id: Long): ContactDetails? = contactService.getContact(id)

  fun getContactName(id: Long): ContactNameDetails? = contactService.getContactName(id)

  fun searchContacts(pageable: Pageable, request: ContactSearchRequest): PagedModel<ContactSearchResultItem> = PagedModel(contactSearchService.searchContacts(pageable, request))

  fun patchGiveRelationship() {
    val createdAfter = LocalDateTime.now().minusDays(120)
    val createdByList = listOf("CQW84B", "EQP53X")
    contactService.getContactList(createdAfter, createdByList).forEach {
      it
        .also { logger.info("Patching contact relationship to give approved visitor for prisonerContactId: ${it.prisonerContactId}") }
      patchRelationship(it.prisonerContactId, PatchRelationshipRequest(isApprovedVisitor = JsonNullable.of(true)), User("SYSTEM SUPPORT"))
    }
  }

  fun patchRelationship(prisonerContactId: Long, request: PatchRelationshipRequest, user: User) {
    contactService.updateContactRelationship(prisonerContactId, request, user)
      .also {
        outboundEventsService.send(
          outboundEvent = OutboundEvent.PRISONER_CONTACT_UPDATED,
          identifier = it.prisonerContactId,
          contactId = it.contactId,
          noms = it.prisonerNumber,
          user = user,
        )
      }
  }

  fun deleteContactRelationship(prisonerContactId: Long, user: User) {
    val deletedResponse = contactService.deleteContactRelationship(prisonerContactId, user)
    deletedResponse.ids.let {
      outboundEventsService.send(
        outboundEvent = OutboundEvent.PRISONER_CONTACT_DELETED,
        identifier = it.prisonerContactId,
        contactId = it.contactId,
        noms = it.prisonerNumber,
        user = user,
      )
    }
    if (deletedResponse.wasUpdated) {
      outboundEventsService.send(
        outboundEvent = OutboundEvent.CONTACT_UPDATED,
        identifier = deletedResponse.ids.contactId,
        contactId = deletedResponse.ids.contactId,
        source = Source.DPS,
        user = user,
      )
    }
  }

  fun assessIfRelationshipCanBeDeleted(prisonerContactId: Long, user: User) = contactService.assessIfRelationshipCanBeDeleted(prisonerContactId, user)

  fun removeInternalOfficialDateOfBirth(): List<Long> = contactService.removeInternalOfficialContactsDateOfBirth().also { sendEventsForContactsUpdated(it) }

  private fun sendEventsForContactsUpdated(listOfContactIds: List<Long>) = listOfContactIds.map { updated ->
    outboundEventsService.send(
      outboundEvent = OutboundEvent.CONTACT_UPDATED,
      identifier = updated,
      contactId = updated,
      source = Source.DPS,
      user = User.SYS_USER,
    )
  }
}
