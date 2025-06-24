package uk.gov.justice.digital.hmpps.hmppscontactsapi.service

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppscontactsapi.mapping.sync.mapEntityToSyncResponse
import uk.gov.justice.digital.hmpps.hmppscontactsapi.mapping.sync.mapSyncRequestToEntity
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.ReferenceCodeGroup
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync.SyncCreatePrisonerRestrictionRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync.SyncUpdatePrisonerRestrictionRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.sync.SyncPrisonerRestriction
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.PrisonerRestrictionsRepository
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.ReferenceCodeRepository

@Service
@Transactional
class PrisonerRestrictionsService(
  private val prisonerRestrictionsRepository: PrisonerRestrictionsRepository,
  private val referenceCodeRepository: ReferenceCodeRepository,
) {

  @Transactional(readOnly = true)
  fun getPrisonerRestrictionById(prisonerRestrictionId: Long): SyncPrisonerRestriction {
    val entity = prisonerRestrictionsRepository.findById(prisonerRestrictionId)
      .orElseThrow { EntityNotFoundException("PrisonerRestriction with ID $prisonerRestrictionId not found") }
    return entity.mapEntityToSyncResponse()
  }

  fun deletePrisonerRestriction(prisonerRestrictionId: Long): SyncPrisonerRestriction {
    val entity = prisonerRestrictionsRepository.findById(prisonerRestrictionId)
      .orElseThrow { EntityNotFoundException("PrisonerRestriction with ID $prisonerRestrictionId not found") }
    prisonerRestrictionsRepository.deleteById(prisonerRestrictionId)
    return entity.mapEntityToSyncResponse()
  }

  fun createPrisonerRestriction(request: SyncCreatePrisonerRestrictionRequest): SyncPrisonerRestriction {
    // NOMIS allow same restriction type to be applied multiple times
    validateReferenceDataExists(request.restrictionType)
    return prisonerRestrictionsRepository.saveAndFlush(request.mapSyncRequestToEntity()).mapEntityToSyncResponse()
  }

  fun updatePrisonerRestriction(
    prisonerRestrictionId: Long,
    request: SyncUpdatePrisonerRestrictionRequest,
  ): SyncPrisonerRestriction {
    val entity = prisonerRestrictionsRepository.findById(prisonerRestrictionId)
      .orElseThrow { EntityNotFoundException("PrisonerRestriction with ID $prisonerRestrictionId not found") }

    // checking that the prisoner number in the request should match the existing restriction's prisoner number to avoid accidental updates.
    if (entity.prisonerNumber != request.prisonerNumber) {
      throw IllegalArgumentException("Prisoner number in request does not match existing prisoner restriction")
    }

    validateReferenceDataExists(request.restrictionType)

    val changed = entity.copy(
      restrictionType = request.restrictionType,
      effectiveDate = request.effectiveDate,
      expiryDate = request.expiryDate,
      commentText = request.commentText,
      authorisedUsername = request.authorisedUsername,
      updatedBy = request.updatedBy,
      updatedTime = request.updatedTime,
    )

    return prisonerRestrictionsRepository.saveAndFlush(changed).mapEntityToSyncResponse()
  }

  private fun validateReferenceDataExists(code: String) = referenceCodeRepository
    .findByGroupCodeAndCode(ReferenceCodeGroup.RESTRICTION, code)
    ?: throw EntityNotFoundException("No reference data found for groupCode: ReferenceCodeGroup.RESTRICTION and code: $code")
}
