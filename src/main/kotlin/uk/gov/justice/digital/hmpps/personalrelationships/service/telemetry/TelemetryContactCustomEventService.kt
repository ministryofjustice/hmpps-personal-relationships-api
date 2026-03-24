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
import uk.gov.justice.digital.hmpps.personalrelationships.model.telemetry.contact.ContactCustomEvent
import uk.gov.justice.digital.hmpps.personalrelationships.model.telemetry.contact.ContactEmailCustomEvent
import uk.gov.justice.digital.hmpps.personalrelationships.model.telemetry.contact.ContactEmploymentCustomEvent
import uk.gov.justice.digital.hmpps.personalrelationships.model.telemetry.contact.ContactIdentityCustomEvent
import uk.gov.justice.digital.hmpps.personalrelationships.model.telemetry.contact.ContactNextOfKinCustomEvent
import uk.gov.justice.digital.hmpps.personalrelationships.model.telemetry.contact.ContactPhoneCustomEvent
import uk.gov.justice.digital.hmpps.personalrelationships.model.telemetry.contact.ContactRestrictionCustomEvent
import uk.gov.justice.digital.hmpps.personalrelationships.model.telemetry.contact.PrisonerContactCustomEvent
import uk.gov.justice.digital.hmpps.personalrelationships.model.telemetry.contact.PrisonerContactRestrictionCustomEvent
import uk.gov.justice.digital.hmpps.personalrelationships.service.events.Source

@Service
class TelemetryContactCustomEventService(private val telemetryService: TelemetryService) {
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

    contactCreationResult.createdRelationship?.let {
      if (it.isNextOfKin) {
        trackCreateNextOfKinEvent(contactId = it.contactId, it.prisonerContactId, source, user)
      }
    }
  }

  fun trackCreateContactEvent(syncContact: SyncContact, source: Source, user: User) {
    val event = ContactCustomEvent(syncContact.id, syncContact, EventActionType.CREATE, source, user)
    telemetryService.track(event)
  }

  fun trackUpdateContactEvent(patchContactResponse: PatchContactResponse, source: Source, user: User) {
    val event = ContactCustomEvent(patchContactResponse.id, patchContactResponse, EventActionType.UPDATE, source, user)
    telemetryService.track(event)
  }

  fun trackUpdateContactEvent(syncContact: SyncContact, source: Source, user: User) {
    val event = ContactCustomEvent(syncContact.id, syncContact, EventActionType.UPDATE, source, user)
    telemetryService.track(event)
  }

  fun trackUpdateContactEvent(contactId: Long, source: Source, user: User) {
    val event = ContactCustomEvent(contactId, EventActionType.UPDATE, source, user)
    telemetryService.track(event)
  }

  fun trackDeleteContactEvent(contactId: Long, source: Source, user: User) {
    val event = ContactCustomEvent(contactId, EventActionType.DELETE, source, user)
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
    val event = ContactAddressCustomEvent(syncContactAddress.contactId, syncContactAddress, EventActionType.CREATE, source, user)
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
    val event = ContactAddressCustomEvent(syncContactAddress.contactId, syncContactAddress, EventActionType.UPDATE, source, user)
    telemetryService.track(event)
  }

  fun trackDeleteContactAddressEvent(contactAddressResponse: ContactAddressResponse, source: Source, user: User) {
    trackDeleteContactAddressEvent(contactAddressResponse.contactId, contactAddressResponse.contactAddressId, source, user)
  }

  fun trackDeleteContactAddressEvent(syncContactAddress: SyncContactAddress, source: Source, user: User) {
    val event = ContactAddressCustomEvent(syncContactAddress.contactId, syncContactAddress, EventActionType.DELETE, source, user)
    telemetryService.track(event)
  }

  fun trackDeleteContactAddressEvent(contactId: Long, contactAddressId: Long, source: Source, user: User) {
    val event = ContactAddressCustomEvent(contactId, contactAddressId, EventActionType.DELETE, source, user)
    telemetryService.track(event)
  }

  fun trackCreatePrisonerContactEvent(prisonerContactRelationship: PrisonerContactRelationshipDetails, source: Source, user: User) {
    val event = PrisonerContactCustomEvent(contactId = prisonerContactRelationship.contactId, prisonerContactRelationship, EventActionType.CREATE, source, user)
    if (prisonerContactRelationship.isNextOfKin) {
      trackCreateNextOfKinEvent(contactId = prisonerContactRelationship.contactId, prisonerContactId = prisonerContactRelationship.prisonerContactId, source = source, user = user)
    }

    telemetryService.track(event)
  }

  fun trackCreatePrisonerContactEvent(syncPrisonerContact: SyncPrisonerContact, source: Source, user: User) {
    val event = PrisonerContactCustomEvent(contactId = syncPrisonerContact.contactId, syncPrisonerContact, EventActionType.CREATE, source, user)
    if (syncPrisonerContact.nextOfKin) {
      trackCreateNextOfKinEvent(contactId = syncPrisonerContact.contactId, prisonerContactId = syncPrisonerContact.id, source = source, user = user)
    }

    telemetryService.track(event)
  }

  fun trackCreatePrisonerContactEvent(contactId: Long, prisonerContactId: Long, prisonerNumber: String, source: Source, user: User) {
    val event = PrisonerContactCustomEvent(contactId, prisonerContactId, prisonerNumber, EventActionType.CREATE, source, user)
    telemetryService.track(event)
  }

  fun trackUpdatePrisonerContactEvent(prisonerContactRelationship: PrisonerContactRelationshipDetails, nextOfKinEventActionType: EventActionType?, source: Source, user: User) {
    val event = PrisonerContactCustomEvent(prisonerContactRelationship.contactId, prisonerContactRelationship, EventActionType.UPDATE, source, user)
    telemetryService.track(event)
    nextOfKinEventActionType?.let { actionType ->
      when (actionType) {
        EventActionType.CREATE -> trackCreateNextOfKinEvent(contactId = prisonerContactRelationship.contactId, prisonerContactId = prisonerContactRelationship.prisonerContactId, source = source, user = user)
        EventActionType.UPDATE -> {} // do nothing
        EventActionType.DELETE -> trackDeleteNextOfKinEvent(contactId = prisonerContactRelationship.contactId, prisonerContactId = prisonerContactRelationship.prisonerContactId, source = source, user = user)
      }
    }
  }

  fun trackUpdatePrisonerContactEvent(relationshipApproved: RelationshipsApproved, source: Source, user: User) {
    val event = PrisonerContactCustomEvent(relationshipApproved.contactId, relationshipApproved, EventActionType.UPDATE, source, user)
    telemetryService.track(event)
  }

  fun trackUpdatePrisonerContactEvent(syncPrisonerContact: SyncPrisonerContact, nextOfKinEventActionType: EventActionType?, source: Source, user: User) {
    val event = PrisonerContactCustomEvent(contactId = syncPrisonerContact.contactId, syncPrisonerContact, EventActionType.UPDATE, source, user)
    nextOfKinEventActionType?.let { actionType ->
      when (actionType) {
        EventActionType.CREATE -> trackCreateNextOfKinEvent(contactId = syncPrisonerContact.contactId, prisonerContactId = syncPrisonerContact.id, source = source, user = user)
        EventActionType.UPDATE -> {} // do nothing
        EventActionType.DELETE -> trackDeleteNextOfKinEvent(contactId = syncPrisonerContact.contactId, prisonerContactId = syncPrisonerContact.id, source = source, user = user)
      }
    }
    telemetryService.track(event)
  }

  fun trackDeletePrisonerContactEvent(syncPrisonerContact: SyncPrisonerContact, source: Source, user: User) {
    val event = PrisonerContactCustomEvent(contactId = syncPrisonerContact.contactId, syncPrisonerContact, EventActionType.DELETE, source, user)
    if (syncPrisonerContact.nextOfKin) {
      trackDeleteNextOfKinEvent(contactId = syncPrisonerContact.contactId, prisonerContactId = syncPrisonerContact.id, source = source, user = user)
    }

    telemetryService.track(event)
  }

  fun trackDeletePrisonerContactEvent(deletedRelationships: DeletedRelationshipIds, nextOfKinEventType: EventActionType?, source: Source, user: User) {
    trackDeletePrisonerContactEvent(deletedRelationships.contactId, deletedRelationships.prisonerContactId, deletedRelationships.prisonerNumber, source, user)
    if (nextOfKinEventType != null && nextOfKinEventType == EventActionType.DELETE) {
      trackDeleteNextOfKinEvent(contactId = deletedRelationships.contactId, prisonerContactId = deletedRelationships.prisonerContactId, source = source, user = user)
    }
  }

  fun trackDeletePrisonerContactEvent(contactId: Long, contactPrisonerId: Long, prisonerNumber: String, source: Source, user: User) {
    val event = PrisonerContactCustomEvent(contactId, contactPrisonerId, prisonerNumber, EventActionType.DELETE, source, user)
    telemetryService.track(event)
  }

  fun trackCreateContactAddressPhoneEvent(contactAddressPhone: ContactAddressPhoneDetails, source: Source, user: User) {
    val event = ContactAddressPhoneCustomEvent(contactAddressPhone.contactId, contactAddressPhone, EventActionType.CREATE, source, user)
    telemetryService.track(event)
  }

  fun trackCreateContactAddressPhoneEvent(syncContactAddressPhone: SyncContactAddressPhone, source: Source, user: User) {
    val event = ContactAddressPhoneCustomEvent(syncContactAddressPhone.contactId, syncContactAddressPhone.contactAddressPhoneId, EventActionType.CREATE, source, user)
    telemetryService.track(event)
  }

  fun trackUpdateContactAddressPhoneEvent(contactAddressPhone: ContactAddressPhoneDetails, source: Source, user: User) {
    val event = ContactAddressPhoneCustomEvent(contactAddressPhone.contactId, contactAddressPhone, EventActionType.UPDATE, source, user)
    telemetryService.track(event)
  }

  fun trackUpdateContactAddressPhoneEvent(syncContactAddressPhone: SyncContactAddressPhone, source: Source, user: User) {
    val event = ContactAddressPhoneCustomEvent(syncContactAddressPhone.contactId, syncContactAddressPhone, EventActionType.UPDATE, source, user)
    telemetryService.track(event)
  }

  fun trackDeleteContactAddressPhoneEvent(syncContactAddressPhone: SyncContactAddressPhone, source: Source, user: User) {
    val event = ContactAddressPhoneCustomEvent(syncContactAddressPhone.contactId, syncContactAddressPhone, EventActionType.DELETE, source, user)
    telemetryService.track(event)
  }

  fun trackDeleteContactAddressPhoneEvent(contactAddressPhone: ContactAddressPhoneDetails, source: Source, user: User) {
    val event = ContactAddressPhoneCustomEvent(contactAddressPhone.contactId, contactAddressPhone, EventActionType.DELETE, source, user)
    telemetryService.track(event)
  }

  fun trackCreateContactEmailEvent(contactEmailDetails: ContactEmailDetails, source: Source, user: User) {
    val event = ContactEmailCustomEvent(contactEmailDetails.contactId, contactEmailDetails, EventActionType.CREATE, source, user)
    telemetryService.track(event)
  }

  fun trackCreateContactEmailEvent(syncContactEmail: SyncContactEmail, source: Source, user: User) {
    val event = ContactEmailCustomEvent(syncContactEmail.contactId, syncContactEmail, EventActionType.CREATE, source, user)
    telemetryService.track(event)
  }

  fun trackUpdateContactEmailEvent(contactEmailDetails: ContactEmailDetails, source: Source, user: User) {
    val event = ContactEmailCustomEvent(contactEmailDetails.contactId, contactEmailDetails, EventActionType.UPDATE, source, user)
    telemetryService.track(event)
  }

  fun trackUpdateContactEmailEvent(syncContactEmail: SyncContactEmail, source: Source, user: User) {
    val event = ContactEmailCustomEvent(syncContactEmail.contactId, syncContactEmail, EventActionType.UPDATE, source, user)
    telemetryService.track(event)
  }

  fun trackDeleteContactEmailEvent(syncContactEmail: SyncContactEmail, source: Source, user: User) {
    val event = ContactEmailCustomEvent(syncContactEmail.contactId, syncContactEmail, EventActionType.DELETE, source, user)
    telemetryService.track(event)
  }

  fun trackDeleteContactEmailEvent(contactId: Long, contactEmailId: Long, source: Source, user: User) {
    val event = ContactEmailCustomEvent(contactId, contactEmailId, EventActionType.DELETE, source, user)
    telemetryService.track(event)
  }

  fun trackCreateContactRestrictionEvent(contactRestriction: ContactRestrictionDetails, source: Source, user: User) {
    val event = ContactRestrictionCustomEvent(contactRestriction.contactId, contactRestriction, EventActionType.CREATE, source, user)
    telemetryService.track(event)
  }

  fun trackCreateContactRestrictionEvent(syncContactRestriction: SyncContactRestriction, source: Source, user: User) {
    val event = ContactRestrictionCustomEvent(syncContactRestriction.contactId, syncContactRestriction, EventActionType.CREATE, source, user)
    telemetryService.track(event)
  }

  fun trackUpdateContactRestrictionEvent(contactRestriction: ContactRestrictionDetails, source: Source, user: User) {
    val event = ContactRestrictionCustomEvent(contactRestriction.contactId, contactRestriction, EventActionType.UPDATE, source, user)
    telemetryService.track(event)
  }

  fun trackUpdateContactRestrictionEvent(syncContactRestriction: SyncContactRestriction, source: Source, user: User) {
    val event = ContactRestrictionCustomEvent(syncContactRestriction.contactId, syncContactRestriction, EventActionType.UPDATE, source, user)
    telemetryService.track(event)
  }

  fun trackDeleteContactRestrictionEvent(syncContactRestriction: SyncContactRestriction, source: Source, user: User) {
    val event = ContactRestrictionCustomEvent(syncContactRestriction.contactId, syncContactRestriction, EventActionType.DELETE, source, user)
    telemetryService.track(event)
  }

  fun trackCreatePrisonerContactRestrictionEvent(prisonerContactRestriction: PrisonerContactRestrictionDetails, source: Source, user: User) {
    val event = PrisonerContactRestrictionCustomEvent(prisonerContactRestriction.contactId, prisonerContactRestriction, EventActionType.CREATE, source, user)
    telemetryService.track(event)
  }

  fun trackCreatePrisonerContactRestrictionEvent(contactId: Long, prisonerContactRestrictionId: Long, source: Source, user: User) {
    val event = PrisonerContactRestrictionCustomEvent(contactId, prisonerContactRestrictionId, EventActionType.CREATE, source, user)
    telemetryService.track(event)
  }

  fun trackCreatePrisonerContactRestrictionEvent(syncPrisonerContactRestriction: SyncPrisonerContactRestriction, source: Source, user: User) {
    val event = PrisonerContactRestrictionCustomEvent(syncPrisonerContactRestriction.contactId, syncPrisonerContactRestriction, EventActionType.CREATE, source, user)
    telemetryService.track(event)
  }

  fun trackUpdatePrisonerContactRestrictionEvent(prisonerContactRestriction: PrisonerContactRestrictionDetails, source: Source, user: User) {
    val event = PrisonerContactRestrictionCustomEvent(prisonerContactRestriction.contactId, prisonerContactRestriction, EventActionType.UPDATE, source, user)
    telemetryService.track(event)
  }

  fun trackUpdatePrisonerContactRestrictionEvent(syncPrisonerContactRestriction: SyncPrisonerContactRestriction, source: Source, user: User) {
    val event = PrisonerContactRestrictionCustomEvent(syncPrisonerContactRestriction.contactId, syncPrisonerContactRestriction, EventActionType.UPDATE, source, user)
    telemetryService.track(event)
  }

  fun trackDeletePrisonerContactRestrictionEvent(syncPrisonerContactRestriction: SyncPrisonerContactRestriction, source: Source, user: User) {
    val event = PrisonerContactRestrictionCustomEvent(syncPrisonerContactRestriction.contactId, syncPrisonerContactRestriction, EventActionType.DELETE, source, user)
    telemetryService.track(event)
  }

  fun trackDeletePrisonerContactRestrictionEvent(contactId: Long, contactRestrictionId: Long, source: Source, user: User) {
    val event = PrisonerContactRestrictionCustomEvent(contactId, contactRestrictionId, EventActionType.DELETE, source, user)
    telemetryService.track(event)
  }

  fun trackCreateContactIdentityEvent(contactIdentity: ContactIdentityDetails, source: Source, user: User) {
    val event = ContactIdentityCustomEvent(contactIdentity.contactId, contactIdentity, EventActionType.CREATE, source, user)
    telemetryService.track(event)
  }

  fun trackCreateContactIdentityEvent(syncContactIdentity: SyncContactIdentity, source: Source, user: User) {
    val event = ContactIdentityCustomEvent(syncContactIdentity.contactId, syncContactIdentity, EventActionType.CREATE, source, user)
    telemetryService.track(event)
  }

  fun trackUpdateContactIdentityEvent(contactIdentity: ContactIdentityDetails, source: Source, user: User) {
    val event = ContactIdentityCustomEvent(contactIdentity.contactId, contactIdentity, EventActionType.UPDATE, source, user)
    telemetryService.track(event)
  }

  fun trackUpdateContactIdentityEvent(syncContactIdentity: SyncContactIdentity, source: Source, user: User) {
    val event = ContactIdentityCustomEvent(syncContactIdentity.contactId, syncContactIdentity, EventActionType.UPDATE, source, user)
    telemetryService.track(event)
  }

  fun trackDeleteContactIdentityEvent(syncContactIdentity: SyncContactIdentity, source: Source, user: User) {
    trackDeleteContactIdentityEvent(syncContactIdentity.contactId, syncContactIdentity.contactIdentityId, source, user)
  }

  fun trackDeleteContactIdentityEvent(contactId: Long, contactIdentityId: Long, source: Source, user: User) {
    val event = ContactIdentityCustomEvent(contactId, contactIdentityId, EventActionType.DELETE, source, user)
    telemetryService.track(event)
  }

  fun trackCreateContactPhoneEvent(contactPhone: ContactPhoneDetails, source: Source, user: User) {
    val event = ContactPhoneCustomEvent(contactPhone.contactId, contactPhone, EventActionType.CREATE, source, user)
    telemetryService.track(event)
  }

  fun trackCreateContactPhoneEvent(contactPhone: SyncContactPhone, source: Source, user: User) {
    val event = ContactPhoneCustomEvent(contactPhone.contactId, contactPhone, EventActionType.CREATE, source, user)
    telemetryService.track(event)
  }

  fun trackUpdateContactPhoneEvent(contactPhone: ContactPhoneDetails, source: Source, user: User) {
    val event = ContactPhoneCustomEvent(contactPhone.contactId, contactPhone, EventActionType.UPDATE, source, user)
    telemetryService.track(event)
  }

  fun trackUpdateContactPhoneEvent(contactPhone: SyncContactPhone, source: Source, user: User) {
    val event = ContactPhoneCustomEvent(contactPhone.contactId, contactPhone, EventActionType.UPDATE, source, user)
    telemetryService.track(event)
  }

  fun trackDeleteContactPhoneEvent(contactPhone: SyncContactPhone, source: Source, user: User) {
    val event = ContactPhoneCustomEvent(contactPhone.contactId, contactPhone, EventActionType.DELETE, source, user)
    telemetryService.track(event)
  }

  fun trackDeleteContactPhoneEvent(contactId: Long, contactPhoneId: Long, source: Source, user: User) {
    val event = ContactPhoneCustomEvent(contactId, contactPhoneId, EventActionType.DELETE, source, user)
    telemetryService.track(event)
  }

  fun trackCreateEmploymentEvent(contactEmployment: EmploymentDetails, source: Source, user: User) {
    val event = ContactEmploymentCustomEvent(contactEmployment.contactId, contactEmployment, EventActionType.CREATE, source, user)
    telemetryService.track(event)
  }

  fun trackCreateEmploymentEvent(syncEmployment: SyncEmployment, source: Source, user: User) {
    val event = ContactEmploymentCustomEvent(syncEmployment.contactId, syncEmployment, EventActionType.CREATE, source, user)
    telemetryService.track(event)
  }

  fun trackCreateEmploymentEvent(contactId: Long, employmentId: Long, source: Source, user: User) {
    val event = ContactEmploymentCustomEvent(contactId, employmentId, EventActionType.CREATE, source, user)
    telemetryService.track(event)
  }

  fun trackUpdateEmploymentEvent(contactEmployment: EmploymentDetails, source: Source, user: User) {
    val event = ContactEmploymentCustomEvent(contactEmployment.contactId, contactEmployment, EventActionType.UPDATE, source, user)
    telemetryService.track(event)
  }

  fun trackUpdateEmploymentEvent(syncEmployment: SyncEmployment, source: Source, user: User) {
    val event = ContactEmploymentCustomEvent(syncEmployment.contactId, syncEmployment, EventActionType.UPDATE, source, user)
    telemetryService.track(event)
  }

  fun trackUpdateEmploymentEvent(contactId: Long, employmentId: Long, source: Source, user: User) {
    val event = ContactEmploymentCustomEvent(contactId, employmentId, EventActionType.UPDATE, source, user)
    telemetryService.track(event)
  }

  fun trackDeleteEmploymentEvent(syncEmployment: SyncEmployment, source: Source, user: User) {
    val event = ContactEmploymentCustomEvent(syncEmployment.contactId, syncEmployment, EventActionType.DELETE, source, user)
    telemetryService.track(event)
  }

  fun trackDeleteEmploymentEvent(contactId: Long, employmentId: Long, source: Source, user: User) {
    val event = ContactEmploymentCustomEvent(contactId, employmentId, EventActionType.DELETE, source, user)
    telemetryService.track(event)
  }

  private fun trackCreateNextOfKinEvent(contactId: Long, prisonerContactId: Long, source: Source, user: User) {
    val event = ContactNextOfKinCustomEvent(contactId, prisonerContactId, EventActionType.CREATE, source, user)
    telemetryService.track(event)
  }

  private fun trackDeleteNextOfKinEvent(contactId: Long, prisonerContactId: Long, source: Source, user: User) {
    val event = ContactNextOfKinCustomEvent(contactId, prisonerContactId, EventActionType.DELETE, source, user)
    telemetryService.track(event)
  }

  private fun trackCreateContactEvent(contact: ContactDetails, source: Source, user: User) {
    val event = ContactCustomEvent(contact.id, contact, EventActionType.CREATE, source, user)
    telemetryService.track(event)
  }

  private fun trackCreateContactAddressEvent(contactAddress: ContactAddressDetails, source: Source, user: User) {
    val event = ContactAddressCustomEvent(contactAddress.contactId, contactAddress, EventActionType.CREATE, source, user)
    telemetryService.track(event)
  }

  private fun trackCreateContactAddressEvent(contactAddressResponse: ContactAddressResponse, source: Source, user: User) {
    val event = ContactAddressCustomEvent(contactAddressResponse.contactId, contactAddressResponse, EventActionType.CREATE, source, user)
    telemetryService.track(event)
  }

  private fun trackCreateContactAddressPhoneEvent(contactId: Long, contactAddressPhoneId: Long, source: Source, user: User) {
    val event = ContactAddressPhoneCustomEvent(contactId, contactAddressPhoneId, EventActionType.CREATE, source, user)
    telemetryService.track(event)
  }

  private fun trackUpdateContactAddressEvent(contactId: Long, contactAddressId: Long, source: Source, user: User) {
    val event = ContactAddressCustomEvent(contactId, contactAddressId, EventActionType.UPDATE, source, user)
    telemetryService.track(event)
  }

  private fun trackCreateContactEmploymentEvent(employmentDetails: EmploymentDetails, source: Source, user: User) {
    val event = ContactEmploymentCustomEvent(employmentDetails.contactId, employmentDetails, EventActionType.CREATE, source, user)
    telemetryService.track(event)
  }
}
