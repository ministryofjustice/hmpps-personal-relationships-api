package uk.gov.justice.digital.hmpps.personalrelationships.facade

import org.slf4j.LoggerFactory
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PagedModel
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.personalrelationships.config.User
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.AddContactRelationshipRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.ContactSearchRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.CreateContactRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.PatchContactRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.PatchRelationshipRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.ContactAuditEntry
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.ContactCreationResult
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.ContactDetails
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.ContactNameDetails
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.ContactSearchResultItem
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.PatchContactResponse
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.PrisonerContactRelationshipDetails
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.RelationshipsApproved
import uk.gov.justice.digital.hmpps.personalrelationships.service.ContactPatchService
import uk.gov.justice.digital.hmpps.personalrelationships.service.ContactSearchService
import uk.gov.justice.digital.hmpps.personalrelationships.service.ContactService
import uk.gov.justice.digital.hmpps.personalrelationships.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.personalrelationships.service.events.OutboundEventsService
import uk.gov.justice.digital.hmpps.personalrelationships.service.events.Source
import uk.gov.justice.digital.hmpps.personalrelationships.service.telemetry.EventActionType
import uk.gov.justice.digital.hmpps.personalrelationships.service.telemetry.TelemetryContactCustomEventService

@Service
class ContactFacade(
  private val outboundEventsService: OutboundEventsService,
  private val contactPatchService: ContactPatchService,
  private val contactService: ContactService,
  private val contactSearchService: ContactSearchService,
  private val telemetryContactCustomEventService: TelemetryContactCustomEventService,
) {
  companion object {
    private val logger = LoggerFactory.getLogger(this::class.java)
  }

  fun createContact(request: CreateContactRequest, user: User): ContactCreationResult {
    logger.debug("createContact called, user: {}", user)
    val contact = contactService.createContact(request, user)
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
      }.also { contactCreationResult ->
        telemetryContactCustomEventService.trackCreateContactEvent(contactCreationResult, Source.DPS, user)
      }

    return contact
  }

  fun addContactRelationship(request: AddContactRelationshipRequest, user: User): PrisonerContactRelationshipDetails {
    logger.debug("addContactRelationship called, user: {}", user)
    val createdRelationship = contactService.addContactRelationship(request, user)
    outboundEventsService.send(
      outboundEvent = OutboundEvent.PRISONER_CONTACT_CREATED,
      identifier = createdRelationship.prisonerContactId,
      contactId = createdRelationship.contactId,
      noms = request.relationship.prisonerNumber,
      user = user,
    ).also {
      telemetryContactCustomEventService.trackCreatePrisonerContactEvent(createdRelationship, Source.DPS, user)
    }

    return createdRelationship
  }

  fun patch(id: Long, request: PatchContactRequest, user: User): PatchContactResponse {
    logger.debug("patch contact called, user: {}", user)
    return contactPatchService.patch(id, request, user)
      .also {
        logger.debug("Send patch domain event to {} {} ", OutboundEvent.CONTACT_UPDATED, id)
        outboundEventsService.send(
          outboundEvent = OutboundEvent.CONTACT_UPDATED,
          identifier = id,
          contactId = id,
          user = user,
        )
      }
      .also {
        telemetryContactCustomEventService.trackUpdateContactEvent(it, Source.DPS, user)
      }
  }

  fun getContact(id: Long): ContactDetails? = contactService.getContact(id)

  fun getContactName(id: Long): ContactNameDetails? = contactService.getContactName(id)

  fun getContactHistory(contactId: Long): List<ContactAuditEntry>? = contactService.getContactHistory(contactId)

  fun searchContacts(pageable: Pageable, request: ContactSearchRequest): PagedModel<ContactSearchResultItem> {
    logger.debug("searchContacts called")
    return PagedModel(contactSearchService.searchContacts(request, pageable))
  }

  fun patchRelationship(prisonerContactId: Long, request: PatchRelationshipRequest, user: User) {
    logger.debug("patchRelationship called, prisonerContactId:{}, user: {}", prisonerContactId, user)
    val existingPrisonerContact = contactService.requirePrisonerContactEntity(prisonerContactId)

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
      .also {
        val nextOfKinEventType = telemetryContactCustomEventService.getNextOfKinEventType(oldPrisonerContactNextOfKin = existingPrisonerContact.nextOfKin, updatedPrisonerContactNextOfKin = it.isNextOfKin)
        telemetryContactCustomEventService.trackUpdatePrisonerContactEvent(it, nextOfKinEventType, Source.DPS, user)
      }
  }

  fun deleteContactRelationship(prisonerContactId: Long, user: User) {
    logger.debug("deleteContactRelationship called, prisonerContactId:{}, user: {}", prisonerContactId, user)
    val existingPrisonerContact = contactService.requirePrisonerContactEntity(prisonerContactId)
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
    val nextOfKinEventType = if (existingPrisonerContact.nextOfKin) EventActionType.DELETE else null
    telemetryContactCustomEventService.trackDeletePrisonerContactEvent(deletedResponse.ids, nextOfKinEventType, Source.DPS, user)

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

  fun assessIfRelationshipCanBeDeleted(prisonerContactId: Long) = contactService.assessIfRelationshipCanBeDeleted(prisonerContactId)

  fun removeInternalOfficialDateOfBirth(): List<Long> {
    logger.debug("removeInternalOfficialDateOfBirth called")
    return contactService.removeInternalOfficialContactsDateOfBirth().also {
      sendEventsForContactsUpdated(it)
    }.also {
      it.forEach { contactId ->
        telemetryContactCustomEventService.trackUpdateContactEvent(contactId, Source.DPS, User.SYS_USER)
      }
    }
  }

  private fun sendEventsForContactsUpdated(listOfContactIds: List<Long>) = listOfContactIds.map { updated ->
    outboundEventsService.send(
      outboundEvent = OutboundEvent.CONTACT_UPDATED,
      identifier = updated,
      contactId = updated,
      source = Source.DPS,
      user = User.SYS_USER,
    )
  }

  fun approveRelationships(createdByList: List<String>, daysAgo: Long): List<RelationshipsApproved> {
    logger.debug("approveRelationships called, createdByList:{} daysAgo:{}", createdByList, daysAgo)
    return contactService.approveRelationships(createdByList, daysAgo).also {
      sendEventsForRelationshipsUpdated(it)
    }.also { relationshipsApprovedLists ->
      relationshipsApprovedLists.forEach {
        telemetryContactCustomEventService.trackUpdatePrisonerContactEvent(it, source = Source.DPS, user = User.SYS_USER)
      }
    }
  }

  private fun sendEventsForRelationshipsUpdated(approved: List<RelationshipsApproved>) = approved.map { rel ->
    outboundEventsService.send(
      outboundEvent = OutboundEvent.PRISONER_CONTACT_UPDATED,
      identifier = rel.prisonerContactId,
      contactId = rel.contactId,
      noms = rel.prisonerNumber,
      source = Source.DPS,
      user = User.SYS_USER,
    )
  }
}
