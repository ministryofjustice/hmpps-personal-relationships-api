package uk.gov.justice.digital.hmpps.hmppscontactsapi.facade

import org.slf4j.LoggerFactory
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PagedModel
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppscontactsapi.config.User
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync.MergePrisonerContactRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync.ResetPrisonerContactRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync.SyncCreateContactAddressPhoneRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync.SyncCreateContactAddressRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync.SyncCreateContactEmailRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync.SyncCreateContactIdentityRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync.SyncCreateContactPhoneRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync.SyncCreateContactRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync.SyncCreateContactRestrictionRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync.SyncCreateEmploymentRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync.SyncCreatePrisonerContactRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync.SyncCreatePrisonerContactRestrictionRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync.SyncUpdateContactAddressPhoneRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync.SyncUpdateContactAddressRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync.SyncUpdateContactEmailRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync.SyncUpdateContactIdentityRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync.SyncUpdateContactPhoneRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync.SyncUpdateContactRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync.SyncUpdateContactRestrictionRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync.SyncUpdateEmploymentRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync.SyncUpdatePrisonerContactRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync.SyncUpdatePrisonerContactRestrictionRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.sync.PrisonerContactAndRestrictionIds
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.sync.PrisonerRelationshipIds
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.ManageUsersService
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.OutboundEventsService
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.Source
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.sync.SyncAdminService
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.sync.SyncContactAddressPhoneService
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.sync.SyncContactAddressService
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.sync.SyncContactEmailService
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.sync.SyncContactIdentityService
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.sync.SyncContactPhoneService
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.sync.SyncContactReconciliationService
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.sync.SyncContactRestrictionService
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.sync.SyncContactService
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.sync.SyncEmploymentService
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.sync.SyncPrisonerContactRestrictionService
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.sync.SyncPrisonerContactService

/**
 * This class is a facade over the sync services as a thin layer
 * which is called by the sync controllers and in-turn calls the sync
 * service methods.
 *
 * Each method provides two purposes:
 * - To call the underlying sync services and apply the changes in a transactional method.
 * - To generate a domain event to inform subscribed services what has happened.
 *
 * All events generated as a result of a sync operation should generate domain events with the
 * additionalInformation source = "NOMIS", which indicates that the actual source of the change
 * was NOMIS.
 *
 * This is important as the Syscon sync service will ignore domain events with
 * a source of NOMIS, but will action those with a source of DPS for changes which
 * originate within this service via the UI or local processes.
 */
@Service
class SyncFacade(
  private val syncContactService: SyncContactService,
  private val syncContactPhoneService: SyncContactPhoneService,
  private val syncContactAddressService: SyncContactAddressService,
  private val syncContactAddressPhoneService: SyncContactAddressPhoneService,
  private val syncContactEmailService: SyncContactEmailService,
  private val syncContactIdentityService: SyncContactIdentityService,
  private val syncContactRestrictionService: SyncContactRestrictionService,
  private val syncPrisonerContactService: SyncPrisonerContactService,
  private val syncPrisonerContactRestrictionService: SyncPrisonerContactRestrictionService,
  private val syncEmploymentService: SyncEmploymentService,
  private val syncAdminService: SyncAdminService,
  private val syncContactReconciliationService: SyncContactReconciliationService,
  private val outboundEventsService: OutboundEventsService,
  private val manageUsersService: ManageUsersService,
) {
  // ================================================================
  //  Contact
  // ================================================================

  fun getContactById(contactId: Long) = syncContactService.getContactById(contactId)

  fun createContact(request: SyncCreateContactRequest) = syncContactService.createContact(request)
    .also {
      outboundEventsService.send(
        outboundEvent = OutboundEvent.CONTACT_CREATED,
        identifier = it.id,
        contactId = it.id,
        source = Source.NOMIS,
        user = userOrDefault(request.createdBy),
      )
    }

  fun updateContact(contactId: Long, request: SyncUpdateContactRequest) = syncContactService.updateContact(contactId, request)
    .also {
      outboundEventsService.send(
        outboundEvent = OutboundEvent.CONTACT_UPDATED,
        identifier = it.id,
        contactId = it.id,
        source = Source.NOMIS,
        user = userOrDefault(request.updatedBy),
      )
    }

  fun deleteContact(contactId: Long) = syncContactService.deleteContact(contactId)
    .also {
      outboundEventsService.send(
        outboundEvent = OutboundEvent.CONTACT_DELETED,
        identifier = contactId,
        contactId = contactId,
        source = Source.NOMIS,
        user = userOrDefault(),
      )
    }

  fun getContactIds(pageable: Pageable) = PagedModel(syncContactService.getContactIds(pageable))

  // ================================================================
  //  Contact Phone
  // ================================================================

  fun getContactPhoneById(contactPhoneId: Long) = syncContactPhoneService.getContactPhoneById(contactPhoneId)

  fun createContactPhone(request: SyncCreateContactPhoneRequest) = syncContactPhoneService.createContactPhone(request)
    .also {
      outboundEventsService.send(
        outboundEvent = OutboundEvent.CONTACT_PHONE_CREATED,
        identifier = it.contactPhoneId,
        contactId = it.contactId,
        source = Source.NOMIS,
        user = userOrDefault(request.createdBy),
      )
    }

  fun updateContactPhone(contactPhoneId: Long, request: SyncUpdateContactPhoneRequest) = syncContactPhoneService.updateContactPhone(contactPhoneId, request)
    .also {
      outboundEventsService.send(
        outboundEvent = OutboundEvent.CONTACT_PHONE_UPDATED,
        identifier = it.contactPhoneId,
        contactId = it.contactId,
        source = Source.NOMIS,
        user = userOrDefault(request.updatedBy),
      )
    }

  fun deleteContactPhone(contactPhoneId: Long) = syncContactPhoneService.deleteContactPhone(contactPhoneId)
    .also {
      outboundEventsService.send(
        outboundEvent = OutboundEvent.CONTACT_PHONE_DELETED,
        identifier = contactPhoneId,
        contactId = it.contactId,
        source = Source.NOMIS,
        user = userOrDefault(),
      )
    }

  // ================================================================
  //  Contact Email
  // ================================================================

  fun getContactEmailById(contactEmailId: Long) = syncContactEmailService.getContactEmailById(contactEmailId)

  fun createContactEmail(request: SyncCreateContactEmailRequest) = syncContactEmailService.createContactEmail(request)
    .also {
      outboundEventsService.send(
        outboundEvent = OutboundEvent.CONTACT_EMAIL_CREATED,
        identifier = it.contactEmailId,
        contactId = it.contactId,
        source = Source.NOMIS,
        user = userOrDefault(request.createdBy),
      )
    }

  fun updateContactEmail(contactEmailId: Long, request: SyncUpdateContactEmailRequest) = syncContactEmailService.updateContactEmail(contactEmailId, request)
    .also {
      outboundEventsService.send(
        outboundEvent = OutboundEvent.CONTACT_EMAIL_UPDATED,
        identifier = it.contactEmailId,
        contactId = it.contactId,
        source = Source.NOMIS,
        user = userOrDefault(request.updatedBy),
      )
    }

  fun deleteContactEmail(contactEmailId: Long) = syncContactEmailService.deleteContactEmail(contactEmailId)
    .also {
      outboundEventsService.send(
        outboundEvent = OutboundEvent.CONTACT_EMAIL_DELETED,
        identifier = it.contactEmailId,
        contactId = it.contactId,
        source = Source.NOMIS,
        user = userOrDefault(),
      )
    }

  // ================================================================
  //  Contact Identity
  // ================================================================

  fun getContactIdentityById(contactIdentityId: Long) = syncContactIdentityService.getContactIdentityById(contactIdentityId)

  fun createContactIdentity(request: SyncCreateContactIdentityRequest) = syncContactIdentityService.createContactIdentity(request)
    .also {
      outboundEventsService.send(
        outboundEvent = OutboundEvent.CONTACT_IDENTITY_CREATED,
        identifier = it.contactIdentityId,
        contactId = it.contactId,
        source = Source.NOMIS,
        user = userOrDefault(request.createdBy),
      )
    }

  fun updateContactIdentity(contactIdentityId: Long, request: SyncUpdateContactIdentityRequest) = syncContactIdentityService.updateContactIdentity(contactIdentityId, request)
    .also {
      outboundEventsService.send(
        outboundEvent = OutboundEvent.CONTACT_IDENTITY_UPDATED,
        identifier = it.contactIdentityId,
        contactId = it.contactId,
        source = Source.NOMIS,
        user = userOrDefault(request.updatedBy),
      )
    }

  fun deleteContactIdentity(contactIdentityId: Long) = syncContactIdentityService.deleteContactIdentity(contactIdentityId)
    .also {
      outboundEventsService.send(
        outboundEvent = OutboundEvent.CONTACT_IDENTITY_DELETED,
        identifier = it.contactIdentityId,
        contactId = it.contactId,
        source = Source.NOMIS,
        user = userOrDefault(),
      )
    }

  // ================================================================
  //  Contact Restriction
  // ================================================================

  fun getContactRestrictionById(contactRestrictionId: Long) = syncContactRestrictionService.getContactRestrictionById(contactRestrictionId)

  fun createContactRestriction(request: SyncCreateContactRestrictionRequest) = syncContactRestrictionService.createContactRestriction(request)
    .also {
      outboundEventsService.send(
        outboundEvent = OutboundEvent.CONTACT_RESTRICTION_CREATED,
        identifier = it.contactRestrictionId,
        contactId = it.contactId,
        source = Source.NOMIS,
        user = userOrDefault(request.createdBy),
      )
    }

  fun updateContactRestriction(contactRestrictionId: Long, request: SyncUpdateContactRestrictionRequest) = syncContactRestrictionService.updateContactRestriction(contactRestrictionId, request)
    .also {
      outboundEventsService.send(
        outboundEvent = OutboundEvent.CONTACT_RESTRICTION_UPDATED,
        identifier = it.contactRestrictionId,
        contactId = it.contactId,
        source = Source.NOMIS,
        user = userOrDefault(request.updatedBy),
      )
    }

  fun deleteContactRestriction(contactRestrictionId: Long) = syncContactRestrictionService.deleteContactRestriction(contactRestrictionId)
    .also {
      outboundEventsService.send(
        outboundEvent = OutboundEvent.CONTACT_RESTRICTION_DELETED,
        identifier = it.contactRestrictionId,
        contactId = it.contactId,
        source = Source.NOMIS,
        user = userOrDefault(),
      )
    }

  // ================================================================
  //  Contact Address
  // ================================================================

  fun getContactAddressById(contactAddressId: Long) = syncContactAddressService.getContactAddressById(contactAddressId)

  fun createContactAddress(request: SyncCreateContactAddressRequest) = syncContactAddressService.createContactAddress(request)
    .also {
      outboundEventsService.send(
        outboundEvent = OutboundEvent.CONTACT_ADDRESS_CREATED,
        identifier = it.contactAddressId,
        contactId = it.contactId,
        source = Source.NOMIS,
        user = userOrDefault(request.createdBy),
      )
    }

  fun updateContactAddress(contactAddressId: Long, request: SyncUpdateContactAddressRequest) = syncContactAddressService.updateContactAddress(contactAddressId, request)
    .also {
      outboundEventsService.send(
        outboundEvent = OutboundEvent.CONTACT_ADDRESS_UPDATED,
        identifier = it.contactAddressId,
        contactId = it.contactId,
        source = Source.NOMIS,
        user = userOrDefault(request.updatedBy),
      )
    }

  fun deleteContactAddress(contactAddressId: Long) = syncContactAddressService.deleteContactAddress(contactAddressId)
    .also {
      outboundEventsService.send(
        outboundEvent = OutboundEvent.CONTACT_ADDRESS_DELETED,
        identifier = it.contactAddressId,
        contactId = it.contactId,
        source = Source.NOMIS,
        user = userOrDefault(),
      )
    }

  // ================================================================
  //  Contact Address Phone  (address-specific phone numbers)
  // ================================================================
  fun getContactAddressPhoneById(contactAddressPhoneId: Long) = syncContactAddressPhoneService.getContactAddressPhoneById(contactAddressPhoneId)

  fun createContactAddressPhone(request: SyncCreateContactAddressPhoneRequest) = syncContactAddressPhoneService.createContactAddressPhone(request)
    .also {
      outboundEventsService.send(
        outboundEvent = OutboundEvent.CONTACT_ADDRESS_PHONE_CREATED,
        identifier = it.contactAddressPhoneId,
        contactId = it.contactId,
        source = Source.NOMIS,
        secondIdentifier = it.contactAddressId,
        user = userOrDefault(request.createdBy),
      )
    }

  fun updateContactAddressPhone(contactAddressPhoneId: Long, request: SyncUpdateContactAddressPhoneRequest) = syncContactAddressPhoneService.updateContactAddressPhone(contactAddressPhoneId, request)
    .also {
      outboundEventsService.send(
        outboundEvent = OutboundEvent.CONTACT_ADDRESS_PHONE_UPDATED,
        identifier = it.contactAddressPhoneId,
        contactId = it.contactId,
        source = Source.NOMIS,
        secondIdentifier = it.contactAddressId,
        user = userOrDefault(request.updatedBy),
      )
    }

  fun deleteContactAddressPhone(contactAddressPhoneId: Long) = syncContactAddressPhoneService.deleteContactAddressPhone(contactAddressPhoneId)
    .also {
      outboundEventsService.send(
        outboundEvent = OutboundEvent.CONTACT_ADDRESS_PHONE_DELETED,
        identifier = it.contactAddressPhoneId,
        contactId = it.contactId,
        source = Source.NOMIS,
        secondIdentifier = it.contactAddressId,
        user = userOrDefault(),
      )
    }

  // ================================================================
  //  Prisoner Contact
  // ================================================================

  fun getPrisonerContactById(prisonerContactId: Long) = syncPrisonerContactService.getPrisonerContactById(prisonerContactId)

  fun createPrisonerContact(request: SyncCreatePrisonerContactRequest) = syncPrisonerContactService.createPrisonerContact(request)
    .also {
      outboundEventsService.send(
        outboundEvent = OutboundEvent.PRISONER_CONTACT_CREATED,
        identifier = it.id,
        contactId = it.contactId,
        noms = it.prisonerNumber,
        source = Source.NOMIS,
        user = userOrDefault(request.createdBy),
      )
    }

  fun updatePrisonerContact(prisonerContactId: Long, request: SyncUpdatePrisonerContactRequest) = syncPrisonerContactService.updatePrisonerContact(prisonerContactId, request)
    .also {
      outboundEventsService.send(
        outboundEvent = OutboundEvent.PRISONER_CONTACT_UPDATED,
        identifier = it.id,
        contactId = it.contactId,
        noms = it.prisonerNumber,
        source = Source.NOMIS,
        user = userOrDefault(request.updatedBy),
      )
    }

  fun deletePrisonerContact(prisonerContactId: Long) = syncPrisonerContactService.deletePrisonerContact(prisonerContactId)
    .also {
      outboundEventsService.send(
        outboundEvent = OutboundEvent.PRISONER_CONTACT_DELETED,
        identifier = it.id,
        contactId = it.contactId,
        noms = it.prisonerNumber,
        source = Source.NOMIS,
        user = userOrDefault(),
      )
    }

  // ================================================================
  //  Prisoner Contact Restriction
  // ================================================================

  fun getPrisonerContactRestrictionById(prisonerContactRestrictionId: Long) = syncPrisonerContactRestrictionService.getPrisonerContactRestrictionById(prisonerContactRestrictionId)

  fun createPrisonerContactRestriction(request: SyncCreatePrisonerContactRestrictionRequest) = syncPrisonerContactRestrictionService.createPrisonerContactRestriction(request)
    .also {
      outboundEventsService.send(
        outboundEvent = OutboundEvent.PRISONER_CONTACT_RESTRICTION_CREATED,
        identifier = it.prisonerContactRestrictionId,
        contactId = it.contactId,
        noms = it.prisonerNumber,
        source = Source.NOMIS,
        user = userOrDefault(request.createdBy),
      )
    }

  fun updatePrisonerContactRestriction(prisonerContactRestrictionId: Long, request: SyncUpdatePrisonerContactRestrictionRequest) = syncPrisonerContactRestrictionService.updatePrisonerContactRestriction(prisonerContactRestrictionId, request)
    .also {
      outboundEventsService.send(
        outboundEvent = OutboundEvent.PRISONER_CONTACT_RESTRICTION_UPDATED,
        identifier = it.prisonerContactRestrictionId,
        contactId = it.contactId,
        noms = it.prisonerNumber,
        source = Source.NOMIS,
        user = userOrDefault(request.updatedBy),
      )
    }

  fun deletePrisonerContactRestriction(prisonerContactRestrictionId: Long) = syncPrisonerContactRestrictionService.deletePrisonerContactRestriction(prisonerContactRestrictionId)
    .also {
      outboundEventsService.send(
        outboundEvent = OutboundEvent.PRISONER_CONTACT_RESTRICTION_DELETED,
        identifier = it.prisonerContactRestrictionId,
        contactId = it.contactId,
        noms = it.prisonerNumber,
        source = Source.NOMIS,
        user = userOrDefault(),
      )
    }

  // ================================================================
  //  Employment sync
  // ================================================================

  fun getEmploymentById(employmentId: Long) = syncEmploymentService.getEmploymentById(employmentId)

  fun createEmployment(request: SyncCreateEmploymentRequest) = syncEmploymentService.createEmployment(request)
    .also {
      outboundEventsService.send(
        outboundEvent = OutboundEvent.EMPLOYMENT_CREATED,
        identifier = it.employmentId,
        contactId = it.contactId,
        source = Source.NOMIS,
        user = userOrDefault(request.createdBy),
      )
    }

  fun updateEmployment(employmentId: Long, request: SyncUpdateEmploymentRequest) = syncEmploymentService.updateEmployment(employmentId, request)
    .also {
      outboundEventsService.send(
        outboundEvent = OutboundEvent.EMPLOYMENT_UPDATED,
        identifier = it.employmentId,
        contactId = it.contactId,
        source = Source.NOMIS,
        user = userOrDefault(request.updatedBy),
      )
    }

  fun deleteEmployment(employmentId: Long) = syncEmploymentService.deleteEmployment(employmentId)
    .also {
      outboundEventsService.send(
        outboundEvent = OutboundEvent.EMPLOYMENT_DELETED,
        identifier = it.employmentId,
        contactId = it.contactId,
        source = Source.NOMIS,
        user = userOrDefault(),
      )
    }

  // ===============================================================================
  // RECONCILE - a single contact by ID - returns a very basic summary of data for
  // one contact and its sub-entities to reconcile against.
  // ===============================================================================

  fun reconcileSingleContact(contactId: Long) = syncContactReconciliationService.getContactDetailsById(contactId)

  // =====================================================================================
  //  MERGE - Replace the full set of relationships and restrictions for two prisoners
  //  after an offender merge in NOMIS. This request contains a removed prisoner number
  //  and a retained prisoner number along with the full set of relationships and
  //  restrictions as they are in NOMIS after the merge for the retained prisoner.
  // =====================================================================================

  fun mergePrisonerContacts(request: MergePrisonerContactRequest) = syncAdminService.mergePrisonerContacts(request)
    .also {
      sendEventsForRelationshipsRemoved(it.relationshipsRemoved)
      sendEventsForRelationshipsCreated(request.retainedPrisonerNumber, it.relationshipsCreated)
    }

  // ======================================================================================
  //  RESET - Replaces the full set of relationships and restrictions for a single prisoner.
  //  This is called by the Syscon sync service when one of the following events occur:
  //    - A new booking is recorded for the prisoner in NOMIS
  //    - A booking move (from one prisoner to another) is completed in NOMIS
  //    - An old booking (for the same prisoner) is reinstated
  // ===========================================================================================================

  fun resetPrisonerContacts(request: ResetPrisonerContactRequest) = syncAdminService.resetPrisonerContacts(request)
    .also {
      sendEventsForRelationshipsRemoved(it.relationshipsRemoved)
      sendEventsForRelationshipsCreated(request.prisonerNumber, it.relationshipsCreated)
    }

  private fun sendEventsForRelationshipsRemoved(
    relationshipsRemoved: List<PrisonerRelationshipIds>,
  ) = relationshipsRemoved.map { removed ->
    removed.prisonerContactRestrictionIds.map { prisonerContactRestrictionId ->
      outboundEventsService.send(
        outboundEvent = OutboundEvent.PRISONER_CONTACT_RESTRICTION_DELETED,
        identifier = prisonerContactRestrictionId,
        contactId = removed.contactId,
        noms = removed.prisonerNumber,
        source = Source.NOMIS,
        user = userOrDefault(null),
      )
    }

    outboundEventsService.send(
      outboundEvent = OutboundEvent.PRISONER_CONTACT_DELETED,
      identifier = removed.prisonerContactId,
      contactId = removed.contactId,
      noms = removed.prisonerNumber,
      source = Source.NOMIS,
      user = userOrDefault(null),
    )
  }

  private fun sendEventsForRelationshipsCreated(
    prisonerNumber: String,
    relationshipsCreated: List<PrisonerContactAndRestrictionIds>,
  ) = relationshipsCreated.map { created ->
    created.restrictions.map { restriction ->
      outboundEventsService.send(
        outboundEvent = OutboundEvent.PRISONER_CONTACT_RESTRICTION_CREATED,
        identifier = restriction.dpsId,
        contactId = created.contactId,
        noms = prisonerNumber,
        source = Source.NOMIS,
        user = userOrDefault(),
      )
    }

    outboundEventsService.send(
      outboundEvent = OutboundEvent.PRISONER_CONTACT_CREATED,
      identifier = created.relationship.dpsId,
      contactId = created.contactId,
      noms = prisonerNumber,
      source = Source.NOMIS,
      user = userOrDefault(),
    )
  }

  private fun userOrDefault(username: String? = null): User = username?.let { enrichIfPossible(username) } ?: User.SYS_USER

  private fun enrichIfPossible(username: String): User {
    val userDetails = try {
      manageUsersService.getUserByUsername(username)
    } catch (e: Exception) {
      logger.error("Unhandled exception getting user {}", username, e)
      null
    }
    return User(username, userDetails?.activeCaseLoadId)
  }

  companion object {
    private val logger = LoggerFactory.getLogger(this::class.java)
  }
}
