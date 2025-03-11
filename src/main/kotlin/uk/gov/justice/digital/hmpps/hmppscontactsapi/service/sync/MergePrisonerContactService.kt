package uk.gov.justice.digital.hmpps.hmppscontactsapi.service.sync

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.PrisonerContactEntity
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.PrisonerContactRestrictionEntity
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync.MergePrisonerContactRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.migrate.ContactsAndRestrictions
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.migrate.ElementType
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.migrate.IdPair
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.sync.MergePrisonerContactResponse
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.sync.PrisonerRelationshipIds
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.PrisonerContactRepository
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.PrisonerContactRestrictionRepository
import java.time.LocalDateTime

@Service
@Transactional
class MergePrisonerContactService(
  val prisonerContactRepository: PrisonerContactRepository,
  val prisonerContactRestrictionRepository: PrisonerContactRestrictionRepository,
) {

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

    // Delete the restrictions
    relationshipsForRemovedPrisoner.map { relationship ->
      prisonerContactRestrictionRepository.deleteAllByPrisonerContactId(relationship.prisonerContactId)
    }
    relationshipsForRetainedPrisoner.map { relationship ->
      prisonerContactRestrictionRepository.deleteAllByPrisonerContactId(relationship.prisonerContactId)
    }

    // Delete the relationships for both prisoner numbers
    prisonerContactRepository.deleteAllByPrisonerNumber(request.removedPrisonerNumber)
    prisonerContactRepository.deleteAllByPrisonerNumber(request.retainedPrisonerNumber)

    // Recreate the relationships and restrictions provided for the retained prisoner number only
    val relationshipPairs = extractAndSavePrisonerContacts(request)
    val restrictionPairs = extractAndSavePrisonerContactRestrictions(request, relationshipPairs)

    // Build the response objects for relationships and restrictions that were removed
    val relationshipsRemovedPrisoner = buildRelationshipsRemoved(relationshipsForRemovedPrisoner, restrictionsForRemovedPrisoner)
    val relationshipsRetainedPrisoner = buildRelationshipsRemoved(relationshipsForRetainedPrisoner, restrictionsForRetainedPrisoner)

    return MergePrisonerContactResponse(
      relationshipsCreated = buildContactsAndRestrictionsResponse(relationshipPairs, restrictionPairs),
      relationshipsRemoved = relationshipsRemovedPrisoner + relationshipsRetainedPrisoner,
    )
  }

  fun buildRelationshipsRemoved(
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

  fun buildContactsAndRestrictionsResponse(
    relationships: List<Pair<Long, PrisonerContactEntity>>,
    restrictions: List<Pair<Long, List<Pair<Long, PrisonerContactRestrictionEntity>>>>,
  ) = relationships.map { relationship ->
    val restrictionsForThisContact = restrictions.filter { it.first == relationship.first }
    restrictionsForThisContact.map { restriction ->
      ContactsAndRestrictions(
        relationship = IdPair(ElementType.PRISONER_CONTACT, relationship.first, relationship.second.prisonerContactId),
        restrictions = restriction.second.map {
          IdPair(ElementType.PRISONER_CONTACT_RESTRICTION, it.first, it.second.prisonerContactRestrictionId)
        },
      )
    }
  }.flatten()

  fun extractAndSavePrisonerContacts(req: MergePrisonerContactRequest) = req.prisonerContacts.map { relationship ->
    Pair(
      relationship.id,
      prisonerContactRepository.save(
        PrisonerContactEntity(
          prisonerContactId = 0L,
          contactId = relationship.contactId,
          prisonerNumber = relationship.prisonerNumber,
          relationshipType = relationship.contactType.code,
          relationshipToPrisoner = relationship.relationshipType.code,
          nextOfKin = relationship.nextOfKin,
          emergencyContact = relationship.emergencyContact,
          comments = relationship.comment,
          active = relationship.active,
          approvedVisitor = relationship.approvedVisitor,
          currentTerm = relationship.currentTerm,
          createdBy = relationship.createUsername ?: "MERGE",
          createdTime = relationship.createDateTime ?: LocalDateTime.now(),
        ).also {
          it.updatedBy = relationship.modifyUsername
          it.updatedTime = relationship.modifyDateTime
          it.expiryDate = relationship.expiryDate
        },
      ),
    )
  }

  fun extractAndSavePrisonerContactRestrictions(
    req: MergePrisonerContactRequest,
    prisonerContactPairs: List<Pair<Long, PrisonerContactEntity>>,
  ) = req.prisonerContacts.map { relationship ->
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
              createdBy = restriction.createUsername ?: "MERGE",
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
