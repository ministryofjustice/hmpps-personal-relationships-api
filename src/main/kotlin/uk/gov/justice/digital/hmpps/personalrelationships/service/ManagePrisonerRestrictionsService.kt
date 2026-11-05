package uk.gov.justice.digital.hmpps.personalrelationships.service

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.personalrelationships.config.User
import uk.gov.justice.digital.hmpps.personalrelationships.entity.PrisonerRestriction
import uk.gov.justice.digital.hmpps.personalrelationships.model.ReferenceCodeGroup
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.restrictions.CreatePrisonerRestrictionRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.restrictions.UpdatePrisonerRestrictionRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.PrisonerRestrictionDetails
import uk.gov.justice.digital.hmpps.personalrelationships.repository.PrisonerRestrictionsRepository
import java.time.LocalDateTime

/**
 * Maintains prisoner restrictions when changed in DPS. Changes made here are published as DPS-sourced domain events
 * which the NOMIS sync service uses to update NOMIS.
 */
@Service
@Transactional
class ManagePrisonerRestrictionsService(
  private val prisonerRestrictionsRepository: PrisonerRestrictionsRepository,
  private val referenceCodeService: ReferenceCodeService,
  private val prisonerService: PrisonerService,
  private val prisonerRestrictionsService: PrisonerRestrictionsService,
) {

  fun createPrisonerRestriction(
    prisonerNumber: String,
    request: CreatePrisonerRestrictionRequest,
    user: User,
  ): PrisonerRestrictionDetails {
    prisonerService.checkPrisonerExists(prisonerNumber)
    validateRequest(request.restrictionType, request.effectiveDate, request.expiryDate, allowInactiveType = false)
    val saved = prisonerRestrictionsRepository.saveAndFlush(
      PrisonerRestriction(
        prisonerNumber = prisonerNumber,
        restrictionType = request.restrictionType,
        effectiveDate = request.effectiveDate,
        expiryDate = request.expiryDate,
        commentText = request.commentText,
        authorisedUsername = user.username,
        // restrictions created in DPS apply to the current booking
        currentTerm = true,
        createdBy = user.username,
        createdTime = LocalDateTime.now(),
      ),
    )
    return getDetails(prisonerNumber, saved.prisonerRestrictionId)
  }

  fun updatePrisonerRestriction(
    prisonerNumber: String,
    prisonerRestrictionId: Long,
    request: UpdatePrisonerRestrictionRequest,
    user: User,
  ): PrisonerRestrictionDetails {
    val existing = findForPrisoner(prisonerNumber, prisonerRestrictionId)
    validateRequest(request.restrictionType, request.effectiveDate, request.expiryDate, allowInactiveType = true)
    prisonerRestrictionsRepository.saveAndFlush(
      existing.copy(
        restrictionType = request.restrictionType,
        effectiveDate = request.effectiveDate,
        expiryDate = request.expiryDate,
        commentText = request.commentText,
        updatedBy = user.username,
        updatedTime = LocalDateTime.now(),
      ),
    )
    return getDetails(prisonerNumber, prisonerRestrictionId)
  }

  fun deletePrisonerRestriction(prisonerNumber: String, prisonerRestrictionId: Long, user: User) {
    val existing = findForPrisoner(prisonerNumber, prisonerRestrictionId)
    prisonerRestrictionsRepository.saveAndFlush(existing.softDeleted(user.username))
  }

  private fun findForPrisoner(prisonerNumber: String, prisonerRestrictionId: Long): PrisonerRestriction = prisonerRestrictionsRepository.findById(prisonerRestrictionId)
    .filter { it.prisonerNumber == prisonerNumber }
    .orElseThrow { EntityNotFoundException("Prisoner restriction ($prisonerRestrictionId) not found for prisoner $prisonerNumber") }

  private fun getDetails(prisonerNumber: String, prisonerRestrictionId: Long) = prisonerRestrictionsService.getPrisonerRestrictions(prisonerNumber)
    .first { it.prisonerRestrictionId == prisonerRestrictionId }

  private fun validateRequest(restrictionType: String, effectiveDate: java.time.LocalDate, expiryDate: java.time.LocalDate?, allowInactiveType: Boolean) {
    referenceCodeService.validateReferenceCode(ReferenceCodeGroup.RESTRICTION, restrictionType, allowInactiveType)
    if (expiryDate != null && expiryDate.isBefore(effectiveDate)) {
      throw IllegalArgumentException("Expiry date must be on or after the effective date")
    }
  }
}
