package uk.gov.justice.digital.hmpps.personalrelationships.service.sync

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.personalrelationships.mapping.sync.toEntity
import uk.gov.justice.digital.hmpps.personalrelationships.mapping.sync.toResponse
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.sync.SyncCreatePrisonerContactRestrictionRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.sync.SyncUpdatePrisonerContactRestrictionRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.sync.SyncPrisonerContactRestriction
import uk.gov.justice.digital.hmpps.personalrelationships.repository.PrisonerContactRepository
import uk.gov.justice.digital.hmpps.personalrelationships.repository.PrisonerContactRestrictionRepository

@Service
@Transactional
class SyncPrisonerContactRestrictionService(
  val prisonerContactRestrictionRepository: PrisonerContactRestrictionRepository,
  val prisonerContactRepository: PrisonerContactRepository,
) {

  @Transactional(readOnly = true)
  fun getPrisonerContactRestrictionById(prisonerContactRestrictionId: Long): SyncPrisonerContactRestriction {
    val restriction = prisonerContactRestrictionRepository.findById(prisonerContactRestrictionId)
      .orElseThrow { EntityNotFoundException("Prisoner contact restriction with ID $prisonerContactRestrictionId not found") }

    val relationship = prisonerContactRepository.findById(restriction.prisonerContactId)
      .orElseThrow { EntityNotFoundException("Prisoner contact with ID ${restriction.prisonerContactId} not found") }

    return restriction.toResponse(relationship.contactId, relationship.prisonerNumber)
  }

  fun createPrisonerContactRestriction(request: SyncCreatePrisonerContactRestrictionRequest): SyncPrisonerContactRestriction {
    val relationship = prisonerContactRepository.findById(request.prisonerContactId)
      .orElseThrow { EntityNotFoundException("Prisoner contact with ID ${request.prisonerContactId} not found") }

    return prisonerContactRestrictionRepository
      .saveAndFlush(request.toEntity())
      .toResponse(relationship.contactId, relationship.prisonerNumber)
  }

  fun updatePrisonerContactRestriction(prisonerContactRestrictionId: Long, request: SyncUpdatePrisonerContactRestrictionRequest): SyncPrisonerContactRestriction {
    val restriction = prisonerContactRestrictionRepository.findById(prisonerContactRestrictionId)
      .orElseThrow { EntityNotFoundException("Prisoner contact restriction with ID $prisonerContactRestrictionId not found") }

    val relationship = prisonerContactRepository.findById(restriction.prisonerContactId)
      .orElseThrow { EntityNotFoundException("Prisoner contact with ID ${restriction.prisonerContactId} not found") }

    val changedPrisonerContactRestriction = restriction.copy(
      restrictionType = request.restrictionType,
      startDate = request.startDate,
      expiryDate = request.expiryDate,
      comments = request.comments,
      updatedBy = request.updatedBy,
      updatedTime = request.updatedTime,
    )

    return prisonerContactRestrictionRepository
      .saveAndFlush(changedPrisonerContactRestriction)
      .toResponse(relationship.contactId, relationship.prisonerNumber)
  }

  fun deletePrisonerContactRestriction(prisonerContactRestrictionId: Long): SyncPrisonerContactRestriction {
    val rowToDelete = prisonerContactRestrictionRepository.findById(prisonerContactRestrictionId)
      .orElseThrow { EntityNotFoundException("Prisoner contact restriction with ID $prisonerContactRestrictionId not found") }

    val relationship = prisonerContactRepository.findById(rowToDelete.prisonerContactId)
      .orElseThrow { EntityNotFoundException("Prisoner contact with ID ${rowToDelete.prisonerContactId} not found") }

    prisonerContactRestrictionRepository.deleteById(prisonerContactRestrictionId)

    return rowToDelete.toResponse(relationship.contactId, relationship.prisonerNumber)
  }
}
