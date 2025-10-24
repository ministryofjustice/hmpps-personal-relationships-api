package uk.gov.justice.digital.hmpps.personalrelationships.service.sync

import jakarta.persistence.EntityNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.PrisonerContactEntity
import uk.gov.justice.digital.hmpps.personalrelationships.entity.PrisonerContactEntity
import uk.gov.justice.digital.hmpps.personalrelationships.exception.DuplicateRelationshipException
import uk.gov.justice.digital.hmpps.personalrelationships.mapping.sync.toEntity
import uk.gov.justice.digital.hmpps.personalrelationships.mapping.sync.toResponse
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.sync.SyncCreatePrisonerContactRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.sync.SyncUpdatePrisonerContactRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.sync.SyncPrisonerContact
import uk.gov.justice.digital.hmpps.personalrelationships.repository.PrisonerContactRepository
import java.time.LocalDateTime

@Service
@Transactional
class SyncPrisonerContactService(
  val prisonerContactRepository: PrisonerContactRepository,
) {
  companion object {
    private val logger = LoggerFactory.getLogger(this::class.java)
  }

  @Transactional(readOnly = true)
  fun getPrisonerContactById(prisonerContactId: Long): SyncPrisonerContact {
    val contactEntity = prisonerContactRepository.findById(prisonerContactId)
      .orElseThrow { EntityNotFoundException("Prisoner contact with ID $prisonerContactId not found") }
    return contactEntity.toResponse()
  }

  fun createPrisonerContact(request: SyncCreatePrisonerContactRequest): SyncPrisonerContact {
    if (request.currentTerm == true && prisonerContactRepository.findDuplicateRelationships(request.prisonerNumber, request.contactId, request.relationshipType).isNotEmpty()) {
      throw DuplicateRelationshipException(request.prisonerNumber, request.contactId, request.relationshipType)
    }
    return prisonerContactRepository.saveAndFlush(request.toEntity()).toResponse()
  }

  fun updatePrisonerContact(prisonerContactId: Long, request: SyncUpdatePrisonerContactRequest): SyncPrisonerContact {
    val relationship = prisonerContactRepository.findById(prisonerContactId)
      .orElseThrow { EntityNotFoundException("Prisoner contact with ID $prisonerContactId not found") }

    val changedPrisonerContact = relationship.copy(
      contactId = request.contactId,
      prisonerNumber = request.prisonerNumber,
      relationshipType = request.contactType,
      relationshipToPrisoner = request.relationshipType,
      nextOfKin = request.nextOfKin,
      emergencyContact = request.emergencyContact,
      active = request.active,
      approvedVisitor = request.approvedVisitor.also {
        if (relationship.approvedVisitor != request.approvedVisitor) {
          logger.info("Approval status has been changed from NOMIS for contactId=${relationship.contactId}, prisonerNumber=${relationship.prisonerNumber}: from ${relationship.approvedVisitor} to ${request.approvedVisitor}, updated By ${request.updatedBy}")
        }
      },
      currentTerm = request.currentTerm,
      comments = request.comments,
    ).also {
      setApprovedVisitor(contact, request, it)

      it.expiryDate = request.expiryDate
      it.createdAtPrison = request.createdAtPrison
      it.updatedBy = request.updatedBy
      it.updatedTime = request.updatedTime
    }

    return prisonerContactRepository.saveAndFlush(changedPrisonerContact).toResponse()
  }

  private fun setApprovedVisitor(
    contact: PrisonerContactEntity,
    request: SyncUpdatePrisonerContactRequest,
    it: PrisonerContactEntity,
  ) {
    if (contact.approvedVisitor != true and request.approvedVisitor) {
      // Contact has been approved to visit - set approvedBy and approvedTime
      it.approvedBy = request.updatedBy
      it.approvedTime = LocalDateTime.now()
    } else {
      // Contact is not approved to visit - clear approvedBy and approvedTime
      if (!(contact.approvedVisitor == true and request.approvedVisitor)) {
        it.approvedBy = null
        it.approvedTime = null
      }
    }
  }

  fun deletePrisonerContact(prisonerContactId: Long): SyncPrisonerContact {
    val rowToDelete = prisonerContactRepository.findById(prisonerContactId)
      .orElseThrow { EntityNotFoundException("Prisoner contact with ID $prisonerContactId not found") }
    prisonerContactRepository.deleteById(prisonerContactId)
    return rowToDelete.toResponse()
  }
}
