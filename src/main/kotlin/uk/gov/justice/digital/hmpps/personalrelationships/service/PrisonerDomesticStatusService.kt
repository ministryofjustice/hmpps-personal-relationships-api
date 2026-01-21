package uk.gov.justice.digital.hmpps.personalrelationships.service

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.personalrelationships.config.User
import uk.gov.justice.digital.hmpps.personalrelationships.entity.PrisonerDomesticStatus
import uk.gov.justice.digital.hmpps.personalrelationships.model.ReferenceCodeGroup
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.CreateOrUpdatePrisonerDomesticStatusRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.PrisonerDomesticStatusResponse
import uk.gov.justice.digital.hmpps.personalrelationships.repository.PrisonerDomesticStatusRepository
import uk.gov.justice.digital.hmpps.personalrelationships.repository.ReferenceCodeRepository
import java.time.LocalDateTime

@Service
class PrisonerDomesticStatusService(
  private val prisonerDomesticStatusRepository: PrisonerDomesticStatusRepository,
  private val prisonerService: PrisonerService,
  private val referenceCodeRepository: ReferenceCodeRepository,
) {
  fun getDomesticStatus(prisonerNumber: String): PrisonerDomesticStatusResponse = getPrisonerDomesticStatusActive(prisonerNumber)?.toModel()
    ?: throw EntityNotFoundException("No domestic status found for prisoner number: $prisonerNumber")

  /**
   * Creates a new domestic status record for a prisoner or updates an existing one.
   * If a record exists:
   * - The existing record is moved to history by setting it as inactive
   * - A new active record is created
   * If no record exists:
   * - A new active record is created
   */
  @Transactional
  fun createOrUpdateDomesticStatus(
    prisonerNumber: String,
    request: CreateOrUpdatePrisonerDomesticStatusRequest,
    user: User,
  ): PrisonerDomesticStatusResponse {
    prisonerService.getPrisoner(prisonerNumber)
      ?: throw EntityNotFoundException("Prisoner number $prisonerNumber - not found")
    request.domesticStatusCode?.let { checkReferenceDataExists(it) }

    // Find existing status, If exists, deactivate it
    getPrisonerDomesticStatusActive(prisonerNumber)?.let {
      val deactivatedStatus = it.copy(
        active = false,
      )
      prisonerDomesticStatusRepository.save(deactivatedStatus)
    }

    // Create new active status
    val newDomesticStatus = PrisonerDomesticStatus(
      prisonerNumber = prisonerNumber,
      domesticStatusCode = request.domesticStatusCode,
      createdBy = user.username,
      createdTime = LocalDateTime.now(),
      active = true,
    )

    // Save and return the new status
    return prisonerDomesticStatusRepository.save(newDomesticStatus).toModel()
  }
  private fun PrisonerDomesticStatus.toModel() = PrisonerDomesticStatusResponse(
    id = prisonerDomesticStatusId,
    domesticStatusCode = domesticStatusCode,
    domesticStatusDescription = domesticStatusCode?.let {
      referenceCodeRepository.findByGroupCodeAndCode(
        ReferenceCodeGroup.DOMESTIC_STS,
        it,
      )?.description
    },
    active = active,
    createdTime = createdTime,
    createdBy = createdBy,
  )

  private fun getPrisonerDomesticStatusActive(prisonerNumber: String) = prisonerDomesticStatusRepository.findByPrisonerNumberAndActiveTrue(
    prisonerNumber,
  )

  private fun checkReferenceDataExists(code: String) = referenceCodeRepository
    .findByGroupCodeAndCode(ReferenceCodeGroup.DOMESTIC_STS, code)
    ?: throw EntityNotFoundException("No reference data found for groupCode: DOMESTIC_STS and code: $code")
}
