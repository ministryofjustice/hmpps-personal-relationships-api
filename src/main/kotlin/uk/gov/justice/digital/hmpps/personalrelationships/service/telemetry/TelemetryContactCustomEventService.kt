package uk.gov.justice.digital.hmpps.personalrelationships.service.telemetry

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.personalrelationships.config.User
import uk.gov.justice.digital.hmpps.personalrelationships.model.internal.DeletedRelationshipIds
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.ContactAddressDetails
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.ContactAddressPhoneDetails
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.ContactAddressResponse
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.ContactCreationResult
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.ContactDetails
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.ContactEmailDetails
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.ContactIdentityDetails
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.ContactPhoneDetails
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.ContactRestrictionDetails
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.CreateAddressResponse
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.EmploymentDetails
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.PatchContactResponse
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.PrisonerContactRelationshipDetails
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.PrisonerContactRestrictionDetails
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.RelationshipsApproved
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.UpdateAddressResponse
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.sync.SyncContact
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.sync.SyncContactAddress
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.sync.SyncContactAddressPhone
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.sync.SyncContactEmail
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.sync.SyncContactIdentity
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.sync.SyncContactPhone
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.sync.SyncContactRestriction
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.sync.SyncEmployment
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.sync.SyncPrisonerContact
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.sync.SyncPrisonerContactRestriction
import uk.gov.justice.digital.hmpps.personalrelationships.model.telemetry.contact.ContactAddressCustomEvent
import uk.gov.justice.digital.hmpps.personalrelationships.model.telemetry.contact.ContactAddressPhoneCustomEvent
import uk.gov.justice.digital.hmpps.personalrelationships.model.telemetry.contact.ContactApprovedVisitorCustomEvent
import uk.gov.justice.digital.hmpps.personalrelationships.model.telemetry.contact.ContactCustomEvent
import uk.gov.justice.digital.hmpps.personalrelationships.model.telemetry.contact.ContactEmailCustomEvent
import uk.gov.justice.digital.hmpps.personalrelationships.model.telemetry.contact.ContactEmergencyContactCustomEvent
import uk.gov.justice.digital.hmpps.personalrelationships.model.telemetry.contact.ContactEmploymentCustomEvent
import uk.gov.justice.digital.hmpps.personalrelationships.model.telemetry.contact.ContactIdentityCustomEvent
import uk.gov.justice.digital.hmpps.personalrelationships.model.telemetry.contact.ContactNextOfKinCustomEvent
import uk.gov.justice.digital.hmpps.personalrelationships.model.telemetry.contact.ContactPhoneCustomEvent
import uk.gov.justice.digital.hmpps.personalrelationships.model.telemetry.contact.ContactRestrictionCustomEvent
import uk.gov.justice.digital.hmpps.personalrelationships.model.telemetry.contact.PrisonerContactCustomEvent
import uk.gov.justice.digital.hmpps.personalrelationships.model.telemetry.contact.PrisonerContactRestrictionCustomEvent
import uk.gov.justice.digital.hmpps.personalrelationships.service.LinkedPrisonersService
import uk.gov.justice.digital.hmpps.personalrelationships.service.events.Source

@Service
class TelemetryContactCustomEventService(
  private val telemetryService: TelemetryService,
  private val linkedPrisonersService: LinkedPrisonersService,
) {
  fun trackCreateContactEvent(contactCreationResult: ContactCreationResult, source: Source, user: User) {
    trackCreateContactEvent(contactCreationResult.createdContact, source, user)
    contactCreationResult.createdRelationship?.let { createdRelationship ->
      trackCreatePrisonerContactEvent(createdRelationship, source, user)
    }

    contactCreationResult.createdContact.identities.forEach { identityDetails ->
      trackCreateContactIdentityEvent(identityDetails, source, user)
    }

    contactCreationResult.createdContact.addresses.forEach { addressDetails ->
      trackCreateContactAddressEvent(addressDetails, source, user)
      addressDetails.phoneNumbers.forEach { phoneNumberDetails ->
        trackCreateContactAddressPhoneEvent(phoneNumberDetails, source, user)
      }
    }

    contactCreationResult.createdContact.phoneNumbers.forEach { contactPhone ->
      trackCreateContactPhoneEvent(contactPhone, source, user)
    }

    contactCreationResult.createdContact.emailAddresses.forEach { emailAddressDetails ->
      trackCreateContactEmailEvent(emailAddressDetails, source, user)
    }

    contactCreationResult.createdContact.employments.forEach { employmentDetails ->
      trackCreateContactEmploymentEvent(employmentDetails, source, user)
    }
  }

  fun trackCreateContactEvent(syncContact: SyncContact, source: Source, user: User) {
    val event = ContactCustomEvent(
      contactId = syncContact.id,
      syncContact = syncContact,
      linkedPrisonersCount = getLinkedPrisonersCount(syncContact.id),
      eventActionType = EventActionType.CREATE,
      eventSource = source,
      eventUser = user,
    )
    telemetryService.track(event)
  }

  fun trackUpdateContactEvent(patchContactResponse: PatchContactResponse, source: Source, user: User) {
    val event = ContactCustomEvent(
      contactId = patchContactResponse.id,
      patchContactResponse = patchContactResponse,
      linkedPrisonersCount = getLinkedPrisonersCount(patchContactResponse.id),
      eventActionType = EventActionType.UPDATE,
      eventSource = source,
      eventUser = user,
    )
    telemetryService.track(event)
  }

  fun trackUpdateContactEvent(syncContact: SyncContact, source: Source, user: User) {
    val event = ContactCustomEvent(
      contactId = syncContact.id,
      syncContact = syncContact,
      linkedPrisonersCount = getLinkedPrisonersCount(syncContact.id),
      eventActionType = EventActionType.UPDATE,
      eventSource = source,
      eventUser = user,
    )
    telemetryService.track(event)
  }

  fun trackUpdateContactEvent(contactId: Long, source: Source, user: User) {
    val event = ContactCustomEvent(
      contactId = contactId,
      linkedPrisonersCount = getLinkedPrisonersCount(contactId),
      eventActionType = EventActionType.UPDATE,
      eventSource = source,
      eventUser = user,
    )
    telemetryService.track(event)
  }

  fun trackDeleteContactEvent(contactId: Long, source: Source, user: User) {
    val event = ContactCustomEvent(
      contactId = contactId,
      linkedPrisonersCount = getLinkedPrisonersCount(contactId),
      eventActionType = EventActionType.DELETE,
      eventSource = source,
      eventUser = user,
    )
    telemetryService.track(event)
  }

  fun trackCreateContactAddressEvent(createAddressResponse: CreateAddressResponse, source: Source, user: User) {
    val contactId = createAddressResponse.created.contactId
    trackCreateContactAddressEvent(createAddressResponse.created, source, user)
    createAddressResponse.created.phoneNumberIds.forEach { contactAddressPhoneId ->
      trackCreateContactAddressPhoneEvent(contactId, contactAddressPhoneId, source, user)
    }
    createAddressResponse.otherUpdatedAddressIds.forEach { otherUpdatedAddressId ->
      trackUpdateContactAddressEvent(contactId, otherUpdatedAddressId, source, user)
    }
  }

  fun trackCreateContactAddressEvent(syncContactAddress: SyncContactAddress, source: Source, user: User) {
    val event = ContactAddressCustomEvent(
      contactId = syncContactAddress.contactId,
      linkedPrisonersCount = getLinkedPrisonersCount(syncContactAddress.contactId),
      syncContactAddress = syncContactAddress,
      eventActionType = EventActionType.CREATE,
      eventSource = source,
      eventUser = user,
    )
    telemetryService.track(event)
  }

  fun trackUpdateContactAddressEvent(updateAddressResponse: UpdateAddressResponse, source: Source, user: User) {
    val contactId = updateAddressResponse.updated.contactId
    trackUpdateContactAddressEvent(contactId = contactId, contactAddressId = updateAddressResponse.updated.contactAddressId, source, user)
    updateAddressResponse.otherUpdatedAddressIds.forEach { otherUpdatedAddressId ->
      trackUpdateContactAddressEvent(contactId = contactId, contactAddressId = otherUpdatedAddressId, source, user)
    }
  }

  fun trackUpdateContactAddressEvent(syncContactAddress: SyncContactAddress, source: Source, user: User) {
    val event = ContactAddressCustomEvent(
      contactId = syncContactAddress.contactId,
      linkedPrisonersCount = getLinkedPrisonersCount(syncContactAddress.contactId),
      syncContactAddress = syncContactAddress,
      eventActionType = EventActionType.UPDATE,
      eventSource = source,
      eventUser = user,
    )
    telemetryService.track(event)
  }

  fun trackDeleteContactAddressEvent(contactAddressResponse: ContactAddressResponse, source: Source, user: User) {
    trackDeleteContactAddressEvent(contactAddressResponse.contactId, contactAddressResponse.contactAddressId, source, user)
  }

  fun trackDeleteContactAddressEvent(syncContactAddress: SyncContactAddress, source: Source, user: User) {
    val event = ContactAddressCustomEvent(
      contactId = syncContactAddress.contactId,
      linkedPrisonersCount = getLinkedPrisonersCount(syncContactAddress.contactId),
      syncContactAddress = syncContactAddress,
      eventActionType = EventActionType.DELETE,
      eventSource = source,
      eventUser = user,
    )
    telemetryService.track(event)
  }

  fun trackDeleteContactAddressEvent(contactId: Long, contactAddressId: Long, source: Source, user: User) {
    val event = ContactAddressCustomEvent(
      contactId = contactId,
      contactAddressId = contactAddressId,
      linkedPrisonersCount = getLinkedPrisonersCount(contactId),
      eventActionType = EventActionType.DELETE,
      eventSource = source,
      eventUser = user,
    )
    telemetryService.track(event)
  }

  fun trackCreatePrisonerContactEvent(prisonerContactRelationship: PrisonerContactRelationshipDetails, source: Source, user: User) {
    val event = PrisonerContactCustomEvent(
      contactId = prisonerContactRelationship.contactId,
      linkedPrisonersCount = getLinkedPrisonersCount(prisonerContactRelationship.contactId),
      prisonerContactRelationship = prisonerContactRelationship,
      eventActionType = EventActionType.CREATE,
      eventSource = source,
      eventUser = user,
    )
    telemetryService.track(event)

    // log all child events
    with(prisonerContactRelationship) {
      trackCreatePrisonerContactChildEvents(contactId = contactId, prisonerContactId = prisonerContactId, linkedPrisonersCount = getLinkedPrisonersCount(prisonerContactRelationship.contactId), prisonerNumber = prisonerNumber, isNextOfKin = isNextOfKin, isEmergencyContact = isEmergencyContact, isApprovedVisitor = isApprovedVisitor, source = source, user = user)
    }
  }

  fun trackCreatePrisonerContactEvent(syncPrisonerContact: SyncPrisonerContact, source: Source, user: User) {
    val event = PrisonerContactCustomEvent(
      contactId = syncPrisonerContact.contactId,
      linkedPrisonersCount = getLinkedPrisonersCount(syncPrisonerContact.contactId),
      syncPrisonerContact = syncPrisonerContact,
      eventActionType = EventActionType.CREATE,
      eventSource = source,
      eventUser = user,
    )
    telemetryService.track(event)

    // log all child events
    with(syncPrisonerContact) {
      trackCreatePrisonerContactChildEvents(
        contactId = contactId,
        prisonerContactId = id,
        prisonerNumber = prisonerNumber,
        isNextOfKin = nextOfKin,
        isEmergencyContact = emergencyContact,
        isApprovedVisitor = approvedVisitor,
        linkedPrisonersCount = getLinkedPrisonersCount(contactId),
        source = source,
        user = user,
      )
    }
  }

  fun trackCreatePrisonerContactEvent(contactId: Long, prisonerContactId: Long, prisonerNumber: String, source: Source, user: User) {
    val event = PrisonerContactCustomEvent(
      contactId = contactId,
      prisonerContactId = prisonerContactId,
      prisonerNumber = prisonerNumber,
      linkedPrisonersCount = getLinkedPrisonersCount(contactId),
      eventActionType = EventActionType.CREATE,
      eventSource = source,
      eventUser = user,
    )
    telemetryService.track(event)
  }

  fun trackUpdatePrisonerContactEvent(prisonerContactRelationship: PrisonerContactRelationshipDetails, nextOfKinEventActionType: EventActionType?, approvedVisitorEventActionType: EventActionType?, emergencyContactEventActionType: EventActionType?, source: Source, user: User) {
    val event = PrisonerContactCustomEvent(
      contactId = prisonerContactRelationship.contactId,
      linkedPrisonersCount = getLinkedPrisonersCount(prisonerContactRelationship.contactId),
      prisonerContactRelationship = prisonerContactRelationship,
      eventActionType = EventActionType.UPDATE,
      eventSource = source,
      eventUser = user,
    )
    telemetryService.track(event)
    trackUpdatePrisonerContactChildEvents(contactId = prisonerContactRelationship.contactId, prisonerContactId = prisonerContactRelationship.prisonerContactId, prisonerNumber = prisonerContactRelationship.prisonerNumber, nextOfKinEventActionType = nextOfKinEventActionType, emergencyContactEventActionType = emergencyContactEventActionType, approvedVisitorEventActionType = approvedVisitorEventActionType, source = source, user = user)
  }

  fun trackUpdatePrisonerContactEvent(relationshipApproved: RelationshipsApproved, source: Source, user: User) {
    val event = PrisonerContactCustomEvent(
      contactId = relationshipApproved.contactId,
      relationshipApproved = relationshipApproved,
      linkedPrisonersCount = getLinkedPrisonersCount(relationshipApproved.contactId),
      eventActionType = EventActionType.UPDATE,
      eventSource = source,
      eventUser = user,
    )
    telemetryService.track(event)

    if (relationshipApproved.approvedToVisit) {
      trackUpdatePrisonerContactChildEvents(
        contactId = relationshipApproved.contactId,
        prisonerContactId = relationshipApproved.prisonerContactId,
        prisonerNumber = relationshipApproved.prisonerNumber,
        nextOfKinEventActionType = null,
        emergencyContactEventActionType = null,
        approvedVisitorEventActionType = EventActionType.CREATE,
        source = source,
        user = user,
      )
    }
  }

  fun trackUpdatePrisonerContactEvent(syncPrisonerContact: SyncPrisonerContact, nextOfKinEventActionType: EventActionType?, approvedVisitorEventActionType: EventActionType?, emergencyContactEventActionType: EventActionType?, source: Source, user: User) {
    val event = PrisonerContactCustomEvent(
      contactId = syncPrisonerContact.contactId,
      linkedPrisonersCount = getLinkedPrisonersCount(syncPrisonerContact.contactId),
      syncPrisonerContact = syncPrisonerContact,
      eventActionType = EventActionType.UPDATE,
      eventSource = source,
      eventUser = user,
    )
    telemetryService.track(event)
    trackUpdatePrisonerContactChildEvents(
      contactId = syncPrisonerContact.contactId,
      prisonerContactId = syncPrisonerContact.id,
      prisonerNumber = syncPrisonerContact.prisonerNumber,
      nextOfKinEventActionType = nextOfKinEventActionType,
      emergencyContactEventActionType = emergencyContactEventActionType,
      approvedVisitorEventActionType = approvedVisitorEventActionType,
      source = source,
      user = user,
    )
  }

  fun trackDeletePrisonerContactEvent(syncPrisonerContact: SyncPrisonerContact, source: Source, user: User) {
    trackDeletePrisonerContactEvent(
      contactId = syncPrisonerContact.contactId,
      prisonerContactId = syncPrisonerContact.id,
      relationshipTypeCode = syncPrisonerContact.contactType,
      relationshipToPrisonerCode = syncPrisonerContact.relationshipType,
      activeRelationship = syncPrisonerContact.active,
      prisonerNumber = syncPrisonerContact.prisonerNumber,
      source = source,
      user = user,
    )

    if (syncPrisonerContact.nextOfKin) {
      trackDeleteNextOfKinEvent(
        contactId = syncPrisonerContact.contactId,
        prisonerContactId = syncPrisonerContact.id,
        prisonerNumber = syncPrisonerContact.prisonerNumber,
        linkedPrisonersCount = getLinkedPrisonersCount(syncPrisonerContact.contactId),
        source = source,
        user = user,
      )
    }
    if (syncPrisonerContact.approvedVisitor) {
      trackDeleteApprovedVisitorEvent(
        contactId = syncPrisonerContact.contactId,
        prisonerContactId = syncPrisonerContact.id,
        prisonerNumber = syncPrisonerContact.prisonerNumber,
        source = source,
        user = user,
      )
    }
    if (syncPrisonerContact.emergencyContact) {
      trackDeleteEmergencyContactEvent(
        contactId = syncPrisonerContact.contactId,
        prisonerContactId = syncPrisonerContact.id,
        prisonerNumber = syncPrisonerContact.prisonerNumber,
        source = source,
        user = user,
      )
    }
  }

  fun trackDeletePrisonerContactEvent(deletedRelationships: DeletedRelationshipIds, relationshipTypeCode: String, relationshipToPrisonerCode: String, activeRelationship: Boolean, nextOfKinEventActionType: EventActionType?, approvedVisitorEventActionType: EventActionType?, emergencyContactEventActionType: EventActionType?, source: Source, user: User) {
    trackDeletePrisonerContactEvent(
      contactId = deletedRelationships.contactId,
      prisonerContactId = deletedRelationships.prisonerContactId,
      prisonerNumber = deletedRelationships.prisonerNumber,
      relationshipTypeCode = relationshipTypeCode,
      relationshipToPrisonerCode = relationshipToPrisonerCode,
      activeRelationship = activeRelationship,
      source = source,
      user = user,
    )
    if (nextOfKinEventActionType != null && nextOfKinEventActionType == EventActionType.DELETE) {
      trackDeleteNextOfKinEvent(
        contactId = deletedRelationships.contactId,
        prisonerContactId = deletedRelationships.prisonerContactId,
        prisonerNumber = deletedRelationships.prisonerNumber,
        linkedPrisonersCount = getLinkedPrisonersCount(deletedRelationships.contactId),
        source = source,
        user = user,
      )
    }
    if (approvedVisitorEventActionType != null && approvedVisitorEventActionType == EventActionType.DELETE) {
      trackDeleteApprovedVisitorEvent(
        contactId = deletedRelationships.contactId,
        prisonerContactId = deletedRelationships.prisonerContactId,
        prisonerNumber = deletedRelationships.prisonerNumber,
        source = source,
        user = user,
      )
    }
    if (emergencyContactEventActionType != null && emergencyContactEventActionType == EventActionType.DELETE) {
      trackDeleteEmergencyContactEvent(
        contactId = deletedRelationships.contactId,
        prisonerContactId = deletedRelationships.prisonerContactId,
        prisonerNumber = deletedRelationships.prisonerNumber,
        source = source,
        user = user,
      )
    }
  }

  fun trackDeletePrisonerContactEvent(contactId: Long, prisonerContactId: Long, prisonerNumber: String, relationshipTypeCode: String?, relationshipToPrisonerCode: String?, activeRelationship: Boolean?, source: Source, user: User) {
    val event = PrisonerContactCustomEvent(
      contactId = contactId,
      prisonerContactId = prisonerContactId,
      prisonerNumber = prisonerNumber,
      relationshipTypeCode = relationshipTypeCode,
      relationshipToPrisonerCode = relationshipToPrisonerCode,
      activeRelationship = activeRelationship,
      linkedPrisonersCount = getLinkedPrisonersCount(contactId),
      eventActionType = EventActionType.DELETE,
      eventSource = source,
      eventUser = user,
    )

    telemetryService.track(event)
  }

  fun trackCreateContactAddressPhoneEvent(contactAddressPhone: ContactAddressPhoneDetails, source: Source, user: User) {
    val event = ContactAddressPhoneCustomEvent(
      contactId = contactAddressPhone.contactId,
      contactAddressPhoneDetails = contactAddressPhone,
      linkedPrisonersCount = getLinkedPrisonersCount(contactAddressPhone.contactId),
      eventActionType = EventActionType.CREATE,
      eventSource = source,
      eventUser = user,
    )
    telemetryService.track(event)
  }

  fun trackCreateContactAddressPhoneEvent(syncContactAddressPhone: SyncContactAddressPhone, source: Source, user: User) {
    val event = ContactAddressPhoneCustomEvent(
      contactId = syncContactAddressPhone.contactId,
      contactAddressPhoneId = syncContactAddressPhone.contactAddressPhoneId,
      linkedPrisonersCount = getLinkedPrisonersCount(syncContactAddressPhone.contactId),
      eventActionType = EventActionType.CREATE,
      eventSource = source,
      eventUser = user,
    )
    telemetryService.track(event)
  }

  fun trackUpdateContactAddressPhoneEvent(contactAddressPhone: ContactAddressPhoneDetails, source: Source, user: User) {
    val event = ContactAddressPhoneCustomEvent(
      contactId = contactAddressPhone.contactId,
      contactAddressPhoneDetails = contactAddressPhone,
      linkedPrisonersCount = getLinkedPrisonersCount(contactAddressPhone.contactId),
      eventActionType = EventActionType.UPDATE,
      eventSource = source,
      eventUser = user,
    )
    telemetryService.track(event)
  }

  fun trackUpdateContactAddressPhoneEvent(syncContactAddressPhone: SyncContactAddressPhone, source: Source, user: User) {
    val event = ContactAddressPhoneCustomEvent(
      contactId = syncContactAddressPhone.contactId,
      syncContactAddressPhone = syncContactAddressPhone,
      linkedPrisonersCount = getLinkedPrisonersCount(syncContactAddressPhone.contactId),
      eventActionType = EventActionType.UPDATE,
      eventSource = source,
      eventUser = user,
    )
    telemetryService.track(event)
  }

  fun trackDeleteContactAddressPhoneEvent(syncContactAddressPhone: SyncContactAddressPhone, source: Source, user: User) {
    val event = ContactAddressPhoneCustomEvent(
      contactId = syncContactAddressPhone.contactId,
      syncContactAddressPhone = syncContactAddressPhone,
      linkedPrisonersCount = getLinkedPrisonersCount(syncContactAddressPhone.contactId),
      eventActionType = EventActionType.DELETE,
      eventSource = source,
      eventUser = user,
    )
    telemetryService.track(event)
  }

  fun trackDeleteContactAddressPhoneEvent(contactAddressPhone: ContactAddressPhoneDetails, source: Source, user: User) {
    val event = ContactAddressPhoneCustomEvent(
      contactId = contactAddressPhone.contactId,
      contactAddressPhoneDetails = contactAddressPhone,
      linkedPrisonersCount = getLinkedPrisonersCount(contactAddressPhone.contactId),
      eventActionType = EventActionType.DELETE,
      eventSource = source,
      eventUser = user,
    )
    telemetryService.track(event)
  }

  fun trackCreateContactEmailEvent(contactEmailDetails: ContactEmailDetails, source: Source, user: User) {
    val event = ContactEmailCustomEvent(
      contactId = contactEmailDetails.contactId,
      contactEmailDetails = contactEmailDetails,
      linkedPrisonersCount = getLinkedPrisonersCount(contactEmailDetails.contactId),
      eventActionType = EventActionType.CREATE,
      eventSource = source,
      eventUser = user,
    )
    telemetryService.track(event)
  }

  fun trackCreateContactEmailEvent(syncContactEmail: SyncContactEmail, source: Source, user: User) {
    val event = ContactEmailCustomEvent(
      contactId = syncContactEmail.contactId,
      syncContactEmail = syncContactEmail,
      linkedPrisonersCount = getLinkedPrisonersCount(syncContactEmail.contactId),
      eventActionType = EventActionType.CREATE,
      eventSource = source,
      eventUser = user,
    )
    telemetryService.track(event)
  }

  fun trackUpdateContactEmailEvent(contactEmailDetails: ContactEmailDetails, source: Source, user: User) {
    val event = ContactEmailCustomEvent(
      contactId = contactEmailDetails.contactId,
      contactEmailDetails = contactEmailDetails,
      linkedPrisonersCount = getLinkedPrisonersCount(contactEmailDetails.contactId),
      eventActionType = EventActionType.UPDATE,
      eventSource = source,
      eventUser = user,
    )
    telemetryService.track(event)
  }

  fun trackUpdateContactEmailEvent(syncContactEmail: SyncContactEmail, source: Source, user: User) {
    val event = ContactEmailCustomEvent(
      contactId = syncContactEmail.contactId,
      syncContactEmail = syncContactEmail,
      linkedPrisonersCount = getLinkedPrisonersCount(syncContactEmail.contactId),
      eventActionType = EventActionType.UPDATE,
      eventSource = source,
      eventUser = user,
    )
    telemetryService.track(event)
  }

  fun trackDeleteContactEmailEvent(syncContactEmail: SyncContactEmail, source: Source, user: User) {
    val event = ContactEmailCustomEvent(
      contactId = syncContactEmail.contactId,
      syncContactEmail = syncContactEmail,
      linkedPrisonersCount = getLinkedPrisonersCount(syncContactEmail.contactId),
      eventActionType = EventActionType.DELETE,
      eventSource = source,
      eventUser = user,
    )
    telemetryService.track(event)
  }

  fun trackDeleteContactEmailEvent(contactId: Long, contactEmailId: Long, source: Source, user: User) {
    val event = ContactEmailCustomEvent(
      contactId = contactId,
      contactEmailId = contactEmailId,
      linkedPrisonersCount = getLinkedPrisonersCount(contactId),
      eventActionType = EventActionType.DELETE,
      eventSource = source,
      eventUser = user,
    )
    telemetryService.track(event)
  }

  fun trackCreateContactRestrictionEvent(contactRestriction: ContactRestrictionDetails, source: Source, user: User) {
    val event = ContactRestrictionCustomEvent(
      contactId = contactRestriction.contactId,
      contactRestrictionDetails = contactRestriction,
      linkedPrisonersCount = getLinkedPrisonersCount(contactRestriction.contactId),
      eventActionType = EventActionType.CREATE,
      eventSource = source,
      eventUser = user,
    )
    telemetryService.track(event)
  }

  fun trackCreateContactRestrictionEvent(syncContactRestriction: SyncContactRestriction, source: Source, user: User) {
    val event = ContactRestrictionCustomEvent(
      contactId = syncContactRestriction.contactId,
      syncContactRestriction = syncContactRestriction,
      linkedPrisonersCount = getLinkedPrisonersCount(syncContactRestriction.contactId),
      eventActionType = EventActionType.CREATE,
      eventSource = source,
      eventUser = user,
    )
    telemetryService.track(event)
  }

  fun trackUpdateContactRestrictionEvent(contactRestriction: ContactRestrictionDetails, source: Source, user: User) {
    val event = ContactRestrictionCustomEvent(
      contactId = contactRestriction.contactId,
      contactRestrictionDetails = contactRestriction,
      linkedPrisonersCount = getLinkedPrisonersCount(contactRestriction.contactId),
      eventActionType = EventActionType.UPDATE,
      eventSource = source,
      eventUser = user,
    )
    telemetryService.track(event)
  }

  fun trackUpdateContactRestrictionEvent(syncContactRestriction: SyncContactRestriction, source: Source, user: User) {
    val event = ContactRestrictionCustomEvent(
      contactId = syncContactRestriction.contactId,
      syncContactRestriction = syncContactRestriction,
      linkedPrisonersCount = getLinkedPrisonersCount(syncContactRestriction.contactId),
      eventActionType = EventActionType.UPDATE,
      eventSource = source,
      eventUser = user,
    )
    telemetryService.track(event)
  }

  fun trackDeleteContactRestrictionEvent(syncContactRestriction: SyncContactRestriction, source: Source, user: User) {
    val event = ContactRestrictionCustomEvent(
      contactId = syncContactRestriction.contactId,
      syncContactRestriction = syncContactRestriction,
      linkedPrisonersCount = getLinkedPrisonersCount(syncContactRestriction.contactId),
      eventActionType = EventActionType.DELETE,
      eventSource = source,
      eventUser = user,
    )
    telemetryService.track(event)
  }

  fun trackCreatePrisonerContactRestrictionEvent(prisonerContactRestriction: PrisonerContactRestrictionDetails, source: Source, user: User) {
    val event = PrisonerContactRestrictionCustomEvent(
      contactId = prisonerContactRestriction.contactId,
      prisonerContactRestrictionDetails = prisonerContactRestriction,
      linkedPrisonersCount = getLinkedPrisonersCount(prisonerContactRestriction.contactId),
      eventActionType = EventActionType.CREATE,
      eventSource = source,
      eventUser = user,
    )
    telemetryService.track(event)
  }

  fun trackCreatePrisonerContactRestrictionEvent(contactId: Long, prisonerContactRestrictionId: Long, prisonerNumber: String, source: Source, user: User) {
    val event = PrisonerContactRestrictionCustomEvent(
      contactId = contactId,
      contactRestrictionId = prisonerContactRestrictionId,
      prisonerNumber = prisonerNumber,
      linkedPrisonersCount = getLinkedPrisonersCount(contactId),
      eventActionType = EventActionType.CREATE,
      eventSource = source,
      eventUser = user,
    )
    telemetryService.track(event)
  }

  fun trackCreatePrisonerContactRestrictionEvent(syncPrisonerContactRestriction: SyncPrisonerContactRestriction, source: Source, user: User) {
    val event = PrisonerContactRestrictionCustomEvent(
      contactId = syncPrisonerContactRestriction.contactId,
      syncPrisonerContactRestriction = syncPrisonerContactRestriction,
      linkedPrisonersCount = getLinkedPrisonersCount(syncPrisonerContactRestriction.contactId),
      eventActionType = EventActionType.CREATE,
      eventSource = source,
      eventUser = user,
    )
    telemetryService.track(event)
  }

  fun trackUpdatePrisonerContactRestrictionEvent(prisonerContactRestriction: PrisonerContactRestrictionDetails, source: Source, user: User) {
    val event = PrisonerContactRestrictionCustomEvent(
      contactId = prisonerContactRestriction.contactId,
      prisonerContactRestrictionDetails = prisonerContactRestriction,
      linkedPrisonersCount = getLinkedPrisonersCount(prisonerContactRestriction.contactId),
      eventActionType = EventActionType.UPDATE,
      eventSource = source,
      eventUser = user,
    )
    telemetryService.track(event)
  }

  fun trackUpdatePrisonerContactRestrictionEvent(syncPrisonerContactRestriction: SyncPrisonerContactRestriction, source: Source, user: User) {
    val event = PrisonerContactRestrictionCustomEvent(
      contactId = syncPrisonerContactRestriction.contactId,
      syncPrisonerContactRestriction = syncPrisonerContactRestriction,
      linkedPrisonersCount = getLinkedPrisonersCount(syncPrisonerContactRestriction.contactId),
      eventActionType = EventActionType.UPDATE,
      eventSource = source,
      eventUser = user,
    )
    telemetryService.track(event)
  }

  fun trackDeletePrisonerContactRestrictionEvent(syncPrisonerContactRestriction: SyncPrisonerContactRestriction, source: Source, user: User) {
    val event = PrisonerContactRestrictionCustomEvent(
      contactId = syncPrisonerContactRestriction.contactId,
      syncPrisonerContactRestriction = syncPrisonerContactRestriction,
      linkedPrisonersCount = getLinkedPrisonersCount(syncPrisonerContactRestriction.contactId),
      eventActionType = EventActionType.DELETE,
      eventSource = source,
      eventUser = user,
    )
    telemetryService.track(event)
  }

  fun trackDeletePrisonerContactRestrictionEvent(contactId: Long, contactRestrictionId: Long, prisonerNumber: String, source: Source, user: User) {
    val event = PrisonerContactRestrictionCustomEvent(
      contactId = contactId,
      contactRestrictionId = contactRestrictionId,
      prisonerNumber = prisonerNumber,
      linkedPrisonersCount = getLinkedPrisonersCount(contactId),
      eventActionType = EventActionType.DELETE,
      eventSource = source,
      eventUser = user,
    )
    telemetryService.track(event)
  }

  fun trackCreateContactIdentityEvent(contactIdentity: ContactIdentityDetails, source: Source, user: User) {
    val event = ContactIdentityCustomEvent(
      contactId = contactIdentity.contactId,
      contactIdentityDetails = contactIdentity,
      linkedPrisonersCount = getLinkedPrisonersCount(contactIdentity.contactId),
      eventActionType = EventActionType.CREATE,
      eventSource = source,
      eventUser = user,
    )
    telemetryService.track(event)
  }

  fun trackCreateContactIdentityEvent(syncContactIdentity: SyncContactIdentity, source: Source, user: User) {
    val event = ContactIdentityCustomEvent(
      contactId = syncContactIdentity.contactId,
      syncContactIdentity = syncContactIdentity,
      linkedPrisonersCount = getLinkedPrisonersCount(syncContactIdentity.contactId),
      eventActionType = EventActionType.CREATE,
      eventSource = source,
      eventUser = user,
    )
    telemetryService.track(event)
  }

  fun trackUpdateContactIdentityEvent(contactIdentity: ContactIdentityDetails, source: Source, user: User) {
    val event = ContactIdentityCustomEvent(
      contactId = contactIdentity.contactId,
      contactIdentityDetails = contactIdentity,
      linkedPrisonersCount = getLinkedPrisonersCount(contactIdentity.contactId),
      eventActionType = EventActionType.UPDATE,
      eventSource = source,
      eventUser = user,
    )
    telemetryService.track(event)
  }

  fun trackUpdateContactIdentityEvent(syncContactIdentity: SyncContactIdentity, source: Source, user: User) {
    val event = ContactIdentityCustomEvent(
      contactId = syncContactIdentity.contactId,
      syncContactIdentity = syncContactIdentity,
      linkedPrisonersCount = getLinkedPrisonersCount(syncContactIdentity.contactId),
      eventActionType = EventActionType.UPDATE,
      eventSource = source,
      eventUser = user,
    )
    telemetryService.track(event)
  }

  fun trackDeleteContactIdentityEvent(syncContactIdentity: SyncContactIdentity, source: Source, user: User) {
    trackDeleteContactIdentityEvent(
      contactId = syncContactIdentity.contactId,
      contactIdentityId = syncContactIdentity.contactIdentityId,
      source = source,
      user = user,
    )
  }

  fun trackDeleteContactIdentityEvent(contactId: Long, contactIdentityId: Long, source: Source, user: User) {
    val event = ContactIdentityCustomEvent(
      contactId = contactId,
      contactIdentityId = contactIdentityId,
      linkedPrisonersCount = getLinkedPrisonersCount(contactId),
      eventActionType = EventActionType.DELETE,
      eventSource = source,
      eventUser = user,
    )
    telemetryService.track(event)
  }

  fun trackCreateContactPhoneEvent(contactPhone: ContactPhoneDetails, source: Source, user: User) {
    val event = ContactPhoneCustomEvent(
      contactId = contactPhone.contactId,
      contactPhoneDetails = contactPhone,
      linkedPrisonersCount = getLinkedPrisonersCount(contactPhone.contactId),
      eventActionType = EventActionType.CREATE,
      eventSource = source,
      eventUser = user,
    )
    telemetryService.track(event)
  }

  fun trackCreateContactPhoneEvent(contactPhone: SyncContactPhone, source: Source, user: User) {
    val event = ContactPhoneCustomEvent(
      contactId = contactPhone.contactId,
      syncContactPhone = contactPhone,
      linkedPrisonersCount = getLinkedPrisonersCount(contactPhone.contactId),
      eventActionType = EventActionType.CREATE,
      eventSource = source,
      eventUser = user,
    )
    telemetryService.track(event)
  }

  fun trackUpdateContactPhoneEvent(contactPhone: ContactPhoneDetails, source: Source, user: User) {
    val event = ContactPhoneCustomEvent(
      contactId = contactPhone.contactId,
      contactPhoneDetails = contactPhone,
      linkedPrisonersCount = getLinkedPrisonersCount(contactPhone.contactId),
      eventActionType = EventActionType.UPDATE,
      eventSource = source,
      eventUser = user,
    )
    telemetryService.track(event)
  }

  fun trackUpdateContactPhoneEvent(contactPhone: SyncContactPhone, source: Source, user: User) {
    val event = ContactPhoneCustomEvent(
      contactId = contactPhone.contactId,
      syncContactPhone = contactPhone,
      linkedPrisonersCount = getLinkedPrisonersCount(contactPhone.contactId),
      eventActionType = EventActionType.UPDATE,
      eventSource = source,
      eventUser = user,
    )
    telemetryService.track(event)
  }

  fun trackDeleteContactPhoneEvent(contactPhone: SyncContactPhone, source: Source, user: User) {
    val event = ContactPhoneCustomEvent(
      contactId = contactPhone.contactId,
      syncContactPhone = contactPhone,
      linkedPrisonersCount = getLinkedPrisonersCount(contactPhone.contactId),
      eventActionType = EventActionType.DELETE,
      eventSource = source,
      eventUser = user,
    )
    telemetryService.track(event)
  }

  fun trackDeleteContactPhoneEvent(contactId: Long, contactPhoneId: Long, source: Source, user: User) {
    val event = ContactPhoneCustomEvent(
      contactId = contactId,
      contactPhoneId = contactPhoneId,
      linkedPrisonersCount = getLinkedPrisonersCount(contactId),
      eventActionType = EventActionType.DELETE,
      eventSource = source,
      eventUser = user,
    )
    telemetryService.track(event)
  }

  fun trackCreateEmploymentEvent(contactEmployment: EmploymentDetails, source: Source, user: User) {
    val event = ContactEmploymentCustomEvent(
      contactId = contactEmployment.contactId,
      employmentDetails = contactEmployment,
      linkedPrisonersCount = getLinkedPrisonersCount(contactEmployment.contactId),
      eventActionType = EventActionType.CREATE,
      eventSource = source,
      eventUser = user,
    )
    telemetryService.track(event)
  }

  fun trackCreateEmploymentEvent(syncEmployment: SyncEmployment, source: Source, user: User) {
    val event = ContactEmploymentCustomEvent(
      contactId = syncEmployment.contactId,
      syncEmployment = syncEmployment,
      linkedPrisonersCount = getLinkedPrisonersCount(syncEmployment.contactId),
      eventActionType = EventActionType.CREATE,
      eventSource = source,
      eventUser = user,
    )
    telemetryService.track(event)
  }

  fun trackCreateEmploymentEvent(contactId: Long, employmentId: Long, source: Source, user: User) {
    val event = ContactEmploymentCustomEvent(
      contactId = contactId,
      employmentId = employmentId,
      linkedPrisonersCount = getLinkedPrisonersCount(contactId),
      eventActionType = EventActionType.CREATE,
      eventSource = source,
      eventUser = user,
    )
    telemetryService.track(event)
  }

  fun trackUpdateEmploymentEvent(contactEmployment: EmploymentDetails, source: Source, user: User) {
    val event = ContactEmploymentCustomEvent(
      contactId = contactEmployment.contactId,
      employmentDetails = contactEmployment,
      linkedPrisonersCount = getLinkedPrisonersCount(contactEmployment.contactId),
      eventActionType = EventActionType.UPDATE,
      eventSource = source,
      eventUser = user,
    )
    telemetryService.track(event)
  }

  fun trackUpdateEmploymentEvent(syncEmployment: SyncEmployment, source: Source, user: User) {
    val event = ContactEmploymentCustomEvent(
      contactId = syncEmployment.contactId,
      syncEmployment = syncEmployment,
      linkedPrisonersCount = getLinkedPrisonersCount(syncEmployment.contactId),
      eventActionType = EventActionType.UPDATE,
      eventSource = source,
      eventUser = user,
    )
    telemetryService.track(event)
  }

  fun trackUpdateEmploymentEvent(contactId: Long, employmentId: Long, source: Source, user: User) {
    val event = ContactEmploymentCustomEvent(
      contactId = contactId,
      employmentId = employmentId,
      linkedPrisonersCount = getLinkedPrisonersCount(contactId),
      eventActionType = EventActionType.UPDATE,
      eventSource = source,
      eventUser = user,
    )
    telemetryService.track(event)
  }

  fun trackDeleteEmploymentEvent(syncEmployment: SyncEmployment, source: Source, user: User) {
    val event = ContactEmploymentCustomEvent(
      contactId = syncEmployment.contactId,
      syncEmployment = syncEmployment,
      linkedPrisonersCount = getLinkedPrisonersCount(syncEmployment.contactId),
      eventActionType = EventActionType.DELETE,
      eventSource = source,
      eventUser = user,
    )
    telemetryService.track(event)
  }

  fun trackDeleteEmploymentEvent(contactId: Long, employmentId: Long, source: Source, user: User) {
    val event = ContactEmploymentCustomEvent(
      contactId = contactId,
      employmentId = employmentId,
      linkedPrisonersCount = getLinkedPrisonersCount(contactId),
      eventActionType = EventActionType.DELETE,
      eventSource = source,
      eventUser = user,
    )
    telemetryService.track(event)
  }

  private fun trackNextOfKinEventOnUpdate(nextOfKinEventActionType: EventActionType?, contactId: Long, prisonerContactId: Long, prisonerNumber: String, source: Source, user: User) {
    nextOfKinEventActionType?.let { actionType ->
      when (actionType) {
        EventActionType.CREATE -> trackCreateNextOfKinEvent(contactId = contactId, prisonerContactId = prisonerContactId, prisonerNumber = prisonerNumber, linkedPrisonersCount = getLinkedPrisonersCount(contactId), source = source, user = user)
        EventActionType.UPDATE -> {} // do nothing
        EventActionType.DELETE -> trackDeleteNextOfKinEvent(contactId = contactId, prisonerContactId = prisonerContactId, prisonerNumber = prisonerNumber, linkedPrisonersCount = getLinkedPrisonersCount(contactId), source = source, user = user)
      }
    }
  }

  private fun trackEmergencyContactEventOnUpdate(emergencyContactEventActionType: EventActionType?, contactId: Long, prisonerContactId: Long, prisonerNumber: String, source: Source, user: User) {
    emergencyContactEventActionType?.let { actionType ->
      when (actionType) {
        EventActionType.CREATE -> trackCreateEmergencyContactEvent(contactId = contactId, prisonerContactId = prisonerContactId, prisonerNumber = prisonerNumber, source = source, user = user)
        EventActionType.UPDATE -> {} // do nothing
        EventActionType.DELETE -> trackDeleteEmergencyContactEvent(contactId = contactId, prisonerContactId = prisonerContactId, prisonerNumber = prisonerNumber, source = source, user = user)
      }
    }
  }

  private fun trackApprovedVisitorEventOnUpdate(approvedVisitorEventActionType: EventActionType?, contactId: Long, prisonerContactId: Long, prisonerNumber: String, source: Source, user: User) {
    approvedVisitorEventActionType?.let { actionType ->
      when (actionType) {
        EventActionType.CREATE -> trackCreateApprovedVisitorEvent(contactId = contactId, prisonerContactId = prisonerContactId, prisonerNumber = prisonerNumber, source = source, user = user)
        EventActionType.UPDATE -> {} // do nothing
        EventActionType.DELETE -> trackDeleteApprovedVisitorEvent(contactId = contactId, prisonerContactId = prisonerContactId, prisonerNumber = prisonerNumber, source = source, user = user)
      }
    }
  }

  fun getUpdateEventType(oldContactValue: Boolean, updatedContactValue: Boolean): EventActionType? = if (!oldContactValue && updatedContactValue) {
    EventActionType.CREATE
  } else if (oldContactValue && !updatedContactValue) {
    EventActionType.DELETE
  } else {
    null
  }

  private fun trackCreateNextOfKinEvent(contactId: Long, prisonerContactId: Long, prisonerNumber: String, linkedPrisonersCount: Long, source: Source, user: User) {
    val event = ContactNextOfKinCustomEvent(
      contactId = contactId,
      prisonerContactId = prisonerContactId,
      prisonerNumber = prisonerNumber,
      linkedPrisonersCount = linkedPrisonersCount,
      eventActionType = EventActionType.CREATE,
      eventSource = source,
      eventUser = user,
    )
    telemetryService.track(event)
  }

  private fun trackDeleteNextOfKinEvent(contactId: Long, prisonerContactId: Long, prisonerNumber: String, linkedPrisonersCount: Long, source: Source, user: User) {
    val event = ContactNextOfKinCustomEvent(
      contactId = contactId,
      prisonerContactId = prisonerContactId,
      prisonerNumber = prisonerNumber,
      linkedPrisonersCount = linkedPrisonersCount,
      eventActionType = EventActionType.DELETE,
      eventSource = source,
      eventUser = user,
    )
    telemetryService.track(event)
  }

  private fun trackCreateApprovedVisitorEvent(contactId: Long, prisonerContactId: Long, prisonerNumber: String, source: Source, user: User) {
    val event = ContactApprovedVisitorCustomEvent(
      contactId = contactId,
      prisonerContactId = prisonerContactId,
      prisonerNumber = prisonerNumber,
      linkedPrisonersCount = getLinkedPrisonersCount(contactId),
      eventActionType = EventActionType.CREATE,
      eventSource = source,
      eventUser = user,
    )
    telemetryService.track(event)
  }

  private fun trackDeleteApprovedVisitorEvent(contactId: Long, prisonerContactId: Long, prisonerNumber: String, source: Source, user: User) {
    val event = ContactApprovedVisitorCustomEvent(
      contactId = contactId,
      prisonerContactId = prisonerContactId,
      prisonerNumber = prisonerNumber,
      linkedPrisonersCount = getLinkedPrisonersCount(contactId),
      eventActionType = EventActionType.DELETE,
      eventSource = source,
      eventUser = user,
    )
    telemetryService.track(event)
  }

  private fun trackCreateEmergencyContactEvent(contactId: Long, prisonerContactId: Long, prisonerNumber: String, source: Source, user: User) {
    val event = ContactEmergencyContactCustomEvent(
      contactId = contactId,
      prisonerContactId = prisonerContactId,
      prisonerNumber = prisonerNumber,
      linkedPrisonersCount = getLinkedPrisonersCount(contactId),
      eventActionType = EventActionType.CREATE,
      eventSource = source,
      eventUser = user,
    )
    telemetryService.track(event)
  }

  private fun trackDeleteEmergencyContactEvent(contactId: Long, prisonerContactId: Long, prisonerNumber: String, source: Source, user: User) {
    val event = ContactEmergencyContactCustomEvent(
      contactId = contactId,
      prisonerContactId = prisonerContactId,
      prisonerNumber = prisonerNumber,
      linkedPrisonersCount = getLinkedPrisonersCount(contactId),
      eventActionType = EventActionType.DELETE,
      eventSource = source,
      eventUser = user,
    )
    telemetryService.track(event)
  }

  private fun trackCreateContactEvent(contact: ContactDetails, source: Source, user: User) {
    val event = ContactCustomEvent(
      contactId = contact.id,
      contactDetails = contact,
      linkedPrisonersCount = getLinkedPrisonersCount(contact.id),
      eventActionType = EventActionType.CREATE,
      eventSource = source,
      eventUser = user,
    )
    telemetryService.track(event)
  }

  private fun trackCreateContactAddressEvent(contactAddress: ContactAddressDetails, source: Source, user: User) {
    val event = ContactAddressCustomEvent(
      contactId = contactAddress.contactId,
      linkedPrisonersCount = getLinkedPrisonersCount(contactAddress.contactId),
      contactAddressDetails = contactAddress,
      eventActionType = EventActionType.CREATE,
      eventSource = source,
      eventUser = user,
    )
    telemetryService.track(event)
  }

  private fun trackCreateContactAddressEvent(contactAddressResponse: ContactAddressResponse, source: Source, user: User) {
    val event = ContactAddressCustomEvent(
      contactId = contactAddressResponse.contactId,
      linkedPrisonersCount = getLinkedPrisonersCount(contactAddressResponse.contactId),
      contactAddressResponse = contactAddressResponse,
      eventActionType = EventActionType.CREATE,
      eventSource = source,
      eventUser = user,
    )
    telemetryService.track(event)
  }

  private fun trackCreateContactAddressPhoneEvent(contactId: Long, contactAddressPhoneId: Long, source: Source, user: User) {
    val event = ContactAddressPhoneCustomEvent(
      contactId = contactId,
      contactAddressPhoneId = contactAddressPhoneId,
      linkedPrisonersCount = getLinkedPrisonersCount(contactId),
      eventActionType = EventActionType.CREATE,
      eventSource = source,
      eventUser = user,
    )
    telemetryService.track(event)
  }

  private fun trackUpdateContactAddressEvent(contactId: Long, contactAddressId: Long, source: Source, user: User) {
    val event = ContactAddressCustomEvent(
      contactId = contactId,
      contactAddressId = contactAddressId,
      linkedPrisonersCount = getLinkedPrisonersCount(contactId),
      eventActionType = EventActionType.UPDATE,
      eventSource = source,
      eventUser = user,
    )
    telemetryService.track(event)
  }

  private fun trackCreateContactEmploymentEvent(employmentDetails: EmploymentDetails, source: Source, user: User) {
    val event = ContactEmploymentCustomEvent(
      contactId = employmentDetails.contactId,
      employmentDetails = employmentDetails,
      linkedPrisonersCount = getLinkedPrisonersCount(employmentDetails.contactId),
      eventActionType = EventActionType.CREATE,
      eventSource = source,
      eventUser = user,
    )
    telemetryService.track(event)
  }

  private fun trackCreatePrisonerContactChildEvents(contactId: Long, prisonerContactId: Long, prisonerNumber: String, isNextOfKin: Boolean, isEmergencyContact: Boolean, isApprovedVisitor: Boolean, linkedPrisonersCount: Long, source: Source, user: User) {
    if (isNextOfKin) {
      trackCreateNextOfKinEvent(contactId = contactId, prisonerContactId = prisonerContactId, prisonerNumber = prisonerNumber, linkedPrisonersCount = linkedPrisonersCount, source = source, user = user)
    }
    if (isEmergencyContact) {
      trackCreateEmergencyContactEvent(contactId = contactId, prisonerContactId = prisonerContactId, prisonerNumber = prisonerNumber, source = source, user = user)
    }
    if (isApprovedVisitor) {
      trackCreateApprovedVisitorEvent(contactId = contactId, prisonerContactId = prisonerContactId, prisonerNumber = prisonerNumber, source = source, user = user)
    }
  }

  private fun trackUpdatePrisonerContactChildEvents(contactId: Long, prisonerContactId: Long, prisonerNumber: String, nextOfKinEventActionType: EventActionType?, approvedVisitorEventActionType: EventActionType?, emergencyContactEventActionType: EventActionType?, source: Source, user: User) {
    trackNextOfKinEventOnUpdate(nextOfKinEventActionType = nextOfKinEventActionType, contactId = contactId, prisonerContactId = prisonerContactId, prisonerNumber = prisonerNumber, source = source, user = user)
    trackEmergencyContactEventOnUpdate(emergencyContactEventActionType = emergencyContactEventActionType, contactId = contactId, prisonerContactId = prisonerContactId, prisonerNumber = prisonerNumber, source = source, user = user)
    trackApprovedVisitorEventOnUpdate(approvedVisitorEventActionType = approvedVisitorEventActionType, contactId = contactId, prisonerContactId = prisonerContactId, prisonerNumber = prisonerNumber, source = source, user = user)
  }

  private fun getLinkedPrisonersCount(contactId: Long): Long = linkedPrisonersService.getCurrentTermLinkedPrisonerCount(contactId)
}
