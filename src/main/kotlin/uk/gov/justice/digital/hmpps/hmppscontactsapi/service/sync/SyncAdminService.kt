package uk.gov.justice.digital.hmpps.hmppscontactsapi.service.sync

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.PrisonerContactEntity
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.PrisonerContactRestrictionEntity
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync.MergePrisonerContactRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync.ResetPrisonerContactRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync.SyncPrisonerRelationship
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.migrate.ElementType
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.migrate.IdPair
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.sync.MergePrisonerContactResponse
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.sync.PrisonerContactAndRestrictionIds
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.sync.PrisonerRelationshipIds
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.sync.ResetPrisonerContactResponse
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.PrisonerContactRepository
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.PrisonerContactRestrictionRepository
import java.time.LocalDateTime

@Service
@Transactional
class SyncAdminService(
  val prisonerContactRepository: PrisonerContactRepository,
  val prisonerContactRestrictionRepository: PrisonerContactRestrictionRepository,
) {
  /**
   * MERGE - Called by the Syscon Sync Service when an offender merge happens in NOMIS.
   * This removes the relationships and relationship restrictions for both old and new prisoner numbers,
   * and then recreates the relationships and restrictions provided in the request for the
   * retained prisoner number only.
   */
  fun mergePrisonerContacts(request: MergePrisonerContactRequest): MergePrisonerContactResponse {
    // Get the list of relationships for both prisoner numbers
    val relationshipsForRemovedPrisoner = prisonerContactRepository.findAllByPrisonerNumber(request.removedPrisonerNumber)
    val relationshipsForRetainedPrisoner = prisonerContactRepository.findAllByPrisonerNumber(request.retainedPrisonerNumber)

    // Get the list of restrictions for both prisoner numbers
    val restrictionsForRemovedPrisoner = relationshipsForRemovedPrisoner.map { relationship ->
      prisonerContactRestrictionRepository.findAllByPrisonerContactId(relationship.prisonerContactId)
    }.flatten()

    val restrictionsForRetainedPrisoner = relationshipsForRetainedPrisoner.map { relationship ->
      prisonerContactRestrictionRepository.findAllByPrisonerContactId(relationship.prisonerContactId)
    }.flatten()

    // Delete the relationship restrictions for both prisoners
    relationshipsForRemovedPrisoner.map { relationship ->
      prisonerContactRestrictionRepository.deleteAllByPrisonerContactId(relationship.prisonerContactId)
    }
    relationshipsForRetainedPrisoner.map { relationship ->
      prisonerContactRestrictionRepository.deleteAllByPrisonerContactId(relationship.prisonerContactId)
    }

    // Delete the relationships for both prisoners
    prisonerContactRepository.deleteAllByPrisonerNumber(request.removedPrisonerNumber)
    prisonerContactRepository.deleteAllByPrisonerNumber(request.retainedPrisonerNumber)

    // Recreate the relationships and restrictions provided for the retained prisoner number only
    val relationshipPairs = extractAndSavePrisonerContacts(request.prisonerContacts, relationshipsForRemovedPrisoner, relationshipsForRetainedPrisoner)
    val restrictionPairs = extractAndSavePrisonerContactRestrictions(request.prisonerContacts, relationshipPairs)

    // Build the response objects for relationships and restrictions that were removed
    val relationshipsRemovedPrisoner = buildRelationshipsRemoved(relationshipsForRemovedPrisoner, restrictionsForRemovedPrisoner)
    val relationshipsRetainedPrisoner = buildRelationshipsRemoved(relationshipsForRetainedPrisoner, restrictionsForRetainedPrisoner)

    return MergePrisonerContactResponse(
      relationshipsCreated = buildContactsAndRestrictionsResponse(relationshipPairs, restrictionPairs),
      relationshipsRemoved = relationshipsRemovedPrisoner + relationshipsRetainedPrisoner,
    )
  }

  /**
   * RESET - Called by the Syscon Sync Service for 3 admin scenarios which can happen in NOMIS.
   * 1. New booking - someone comes back into prison on a new booking.
   * 2. Reactivate an old booking - bookings are re-instated over an existing booking.
   * 3. Booking move - to correct misidentified people (i.e. the booking was created against the wrong name)
   *
   * For each of these scenarios the action is the same:
   *  - Remove the relationships and restrictions for a prisoner
   *  - Recreate the relationships and restrictions for this prisoner to match what is present in NOMIS
   */
  fun resetPrisonerContacts(request: ResetPrisonerContactRequest): ResetPrisonerContactResponse {
    // Get the list of relationship entities for the prisoner
    val relationshipsForPrisoner = prisonerContactRepository.findAllByPrisonerNumber(request.prisonerNumber)

    // Get the list of prisoner contact restrictions for the prisoner
    val restrictionsForPrisoner = relationshipsForPrisoner.map { relationship ->
      prisonerContactRestrictionRepository.findAllByPrisonerContactId(relationship.prisonerContactId)
    }.flatten()

    // Delete the prisoner contact restrictions
    relationshipsForPrisoner.map { relationship ->
      prisonerContactRestrictionRepository.deleteAllByPrisonerContactId(relationship.prisonerContactId)
    }

    // Delete the prisoner contacts
    prisonerContactRepository.deleteAllByPrisonerNumber(request.prisonerNumber)

    // Recreate the relationships and restrictions provided for this prisoner
    val relationshipPairs = extractResetAndSavePrisonerContacts(request.prisonerContacts, relationshipsForPrisoner)
    val restrictionPairs = extractAndSavePrisonerContactRestrictions(request.prisonerContacts, relationshipPairs)

    // Build a list of the IDs for relationships that were removed
    val relationshipsRemoved = buildRelationshipsRemoved(relationshipsForPrisoner, restrictionsForPrisoner)

    return ResetPrisonerContactResponse(
      relationshipsCreated = buildContactsAndRestrictionsResponse(relationshipPairs, restrictionPairs),
      relationshipsRemoved = relationshipsRemoved,
    )
  }

  private fun buildRelationshipsRemoved(
    relationships: List<PrisonerContactEntity>,
    restrictions: List<PrisonerContactRestrictionEntity>,
  ): List<PrisonerRelationshipIds> = relationships.map { relationship ->
    val restrictionIdsForRelationship = restrictions
      .filter { restriction -> restriction.prisonerContactId == relationship.prisonerContactId }
      .map { restriction -> restriction.prisonerContactRestrictionId }

    PrisonerRelationshipIds(
      prisonerNumber = relationship.prisonerNumber,
      contactId = relationship.contactId,
      prisonerContactId = relationship.prisonerContactId,
      prisonerContactRestrictionIds = restrictionIdsForRelationship,
    )
  }

  private fun buildContactsAndRestrictionsResponse(
    relationships: List<Pair<Long, PrisonerContactEntity>>,
    restrictions: List<Pair<Long, List<Pair<Long, PrisonerContactRestrictionEntity>>>>,
  ) = relationships.map { relationship ->
    val restrictionsForThisContact = restrictions.filter { it.first == relationship.first }
    restrictionsForThisContact.map { restriction ->
      PrisonerContactAndRestrictionIds(
        contactId = relationship.second.contactId,
        relationship = IdPair(ElementType.PRISONER_CONTACT, relationship.first, relationship.second.prisonerContactId),
        restrictions = restriction.second.map {
          IdPair(ElementType.PRISONER_CONTACT_RESTRICTION, it.first, it.second.prisonerContactRestrictionId)
        },
      )
    }
  }.flatten()

  private fun extractAndSavePrisonerContacts(
    prisonerContacts: List<SyncPrisonerRelationship>,
    relationshipsForRemovedPrisoner: List<PrisonerContactEntity>,
    relationshipsForRetainedPrisoner: List<PrisonerContactEntity>,
  ): List<Pair<Long, PrisonerContactEntity>> {
    // Find the approved by and approved time from the relationshipsForRemovedPrisoner for the

    val resettingPrisonerContacts = prisonerContacts.map { relationship ->
      val approvedByDetails = relationship
        .takeIf { it.approvedVisitor }
        ?.let { findApprovedByDetailsFromExistingRecords(relationshipsForRemovedPrisoner, relationshipsForRetainedPrisoner, it) }
        ?.takeIf { it.approvedVisitor }

      ResetPrisonerContactRequestUpdated(
        prisonerContact = relationship,
        approvedBy = approvedByDetails?.approvedBy,
        approvedTime = approvedByDetails?.approvedTime,
      )
    }

    return getUpdatedRelationships(resettingPrisonerContacts)
  }

  // Find the approved by and approved time from the existing records when approved visitor is set to true in both incoming and existing records
  // this is to ensure that the approved by and approved time is not lost during merge process
  private fun findApprovedByDetailsFromExistingRecords(
    removingPrisonerContacts: List<PrisonerContactEntity>,
    keepingPrisonerContacts: List<PrisonerContactEntity>,
    incomingRelationship: SyncPrisonerRelationship,
  ): PrisonerContactEntity? {
    val allContacts = keepingPrisonerContacts + removingPrisonerContacts

    return allContacts.firstOrNull { contact ->
      contact.contactId == incomingRelationship.contactId &&
        contact.prisonerNumber == incomingRelationship.prisonerNumber &&
        contact.relationshipType == incomingRelationship.contactType.code &&
        contact.relationshipToPrisoner == incomingRelationship.relationshipType.code &&
        contact.approvedVisitor
    }
  }

  private fun extractResetAndSavePrisonerContacts(
    prisonerContacts: List<SyncPrisonerRelationship>,
    existingPrisonerContacts: List<PrisonerContactEntity>,
  ): List<Pair<Long, PrisonerContactEntity>> {
    // iterate through the incoming relationships to see if any have approved visitor true if it is true, then get the approved by and approved time from the resettingPrisonerContacts
    val resettingPrisonerContacts = prisonerContacts.map { relationship ->
      val approvedByDetails = relationship
        .takeIf { it.approvedVisitor }
        ?.let { findApprovedByDetailsFromExistingRecord(existingPrisonerContacts, it) }
        ?.takeIf { it.approvedVisitor }

      ResetPrisonerContactRequestUpdated(
        prisonerContact = relationship,
        approvedBy = approvedByDetails?.approvedBy,
        approvedTime = approvedByDetails?.approvedTime,
      )
    }

    return getUpdatedRelationships(resettingPrisonerContacts)
  }

  private fun getUpdatedRelationships(resettingPrisonerContacts: List<ResetPrisonerContactRequestUpdated>) = resettingPrisonerContacts.map { relationshipUpdate ->
    val prisonerContact = relationshipUpdate.prisonerContact
    Pair(
      prisonerContact.id,
      prisonerContactRepository.save(
        PrisonerContactEntity(
          prisonerContactId = 0L,
          contactId = prisonerContact.contactId,
          prisonerNumber = prisonerContact.prisonerNumber,
          relationshipType = prisonerContact.contactType.code,
          relationshipToPrisoner = prisonerContact.relationshipType.code,
          nextOfKin = prisonerContact.nextOfKin,
          emergencyContact = prisonerContact.emergencyContact,
          comments = prisonerContact.comment,
          active = prisonerContact.active,
          approvedVisitor = prisonerContact.approvedVisitor,
          currentTerm = prisonerContact.currentTerm,
          createdBy = prisonerContact.createUsername ?: "SYSTEM",
          createdTime = prisonerContact.createDateTime ?: LocalDateTime.now(),
        ).also {
          // when recreating relationship during reset records scenarios , approved by and approved time set with value from the resettingPrisonerContacts
          it.approvedBy = relationshipUpdate.approvedBy
          it.approvedTime = relationshipUpdate.approvedTime
          it.updatedBy = prisonerContact.modifyUsername
          it.updatedTime = prisonerContact.modifyDateTime
          it.expiryDate = prisonerContact.expiryDate
        },
      ),
    )
  }

  // Find the approved by and approved time from the resettingPrisonerContacts when approved visitor is set to true in both incoming and resetting records
  // this is to ensure that the approved by and approved time is not lost during reset process
  private fun findApprovedByDetailsFromExistingRecord(
    resettingPrisonerContacts: List<PrisonerContactEntity>,
    incomingRelationship: SyncPrisonerRelationship,
  ) = resettingPrisonerContacts.find {
    it.contactId == incomingRelationship.contactId &&
      it.prisonerNumber == incomingRelationship.prisonerNumber &&
      it.relationshipType == incomingRelationship.contactType.code &&
      it.relationshipToPrisoner == incomingRelationship.relationshipType.code &&
      it.approvedVisitor
  }

  private fun extractAndSavePrisonerContactRestrictions(
    prisonerContacts: List<SyncPrisonerRelationship>,
    prisonerContactPairs: List<Pair<Long, PrisonerContactEntity>>,
  ) = prisonerContacts.map { relationship ->
    // We need to know the saved prisonerContactId for each of the relationship
    val thisRelationship = prisonerContactPairs.find { it.first == relationship.id }

    Pair(
      relationship.id,
      relationship.restrictions.map { restriction ->
        Pair(
          restriction.id,
          prisonerContactRestrictionRepository.save(
            PrisonerContactRestrictionEntity(
              prisonerContactRestrictionId = 0L,
              prisonerContactId = thisRelationship!!.second.prisonerContactId,
              restrictionType = restriction.restrictionType.code,
              startDate = restriction.startDate,
              expiryDate = restriction.expiryDate,
              comments = restriction.comment,
              createdBy = restriction.createUsername ?: "SYSTEM",
              createdTime = restriction.createDateTime ?: LocalDateTime.now(),
              updatedBy = restriction.modifyUsername,
              updatedTime = restriction.modifyDateTime,
            ),
          ),
        )
      },
    )
  }
}

data class ResetPrisonerContactRequestUpdated(

  val prisonerContact: SyncPrisonerRelationship,

  val approvedBy: String? = null,

  val approvedTime: LocalDateTime? = null,

)
