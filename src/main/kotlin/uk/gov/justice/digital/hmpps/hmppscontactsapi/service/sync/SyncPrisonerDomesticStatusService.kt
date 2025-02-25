package uk.gov.justice.digital.hmpps.hmppscontactsapi.service.sync

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.PrisonerDomesticStatus
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.ReferenceCodeGroup
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync.SyncUpdatePrisonerDomesticStatusRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.sync.SyncPrisonerDomesticStatusResponse
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.PrisonerDomesticStatusRepository
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.ReferenceCodeRepository
import java.time.LocalDateTime

@Service
@Transactional
class SyncPrisonerDomesticStatusService(
  private val domesticStatusRepository: PrisonerDomesticStatusRepository,
  private val referenceCodeRepository: ReferenceCodeRepository,
) {
  companion object {
    const val NOT_FOUND_MESSAGE = "Domestic status not found for prisoner: %s"
    fun from(numberOfChildren: PrisonerDomesticStatus) = SyncPrisonerDomesticStatusResponse(
      id = numberOfChildren.prisonerDomesticStatusId,
      domesticStatusCode = numberOfChildren.domesticStatusCode,
      createdBy = numberOfChildren.createdBy,
      createdTime = numberOfChildren.createdTime,
      active = numberOfChildren.active,
    )
  }

  fun getDomesticStatusByPrisonerNumber(prisonerNumber: String): SyncPrisonerDomesticStatusResponse = getPrisonerDomesticStatusActive(prisonerNumber)
    ?.let { from(it) }
    ?: throw EntityNotFoundException(String.format(NOT_FOUND_MESSAGE, prisonerNumber))

  @Transactional
  fun createOrUpdateDomesticStatus(
    prisonerNumber: String,
    request: SyncUpdatePrisonerDomesticStatusRequest,
  ): SyncPrisonerDomesticStatusResponse {
    // Find existing status

    request.domesticStatusCode?.let { validateReferenceDataExists(it) }

    val existingStatus = getPrisonerDomesticStatusActive(prisonerNumber)

    // If exists, deactivate it
    existingStatus?.let {
      val deactivatedStatus = it.copy(
        active = false,
      )
      domesticStatusRepository.save(deactivatedStatus)
    }

    // Create new active status
    val newDomesticStatus = request.domesticStatusCode?.let {
      PrisonerDomesticStatus(
        prisonerNumber = prisonerNumber,
        domesticStatusCode = request.domesticStatusCode,
        createdBy = request.createdBy,
        createdTime = LocalDateTime.now(),
        active = true,
      )
    }

    val saved = newDomesticStatus?.let { domesticStatusRepository.save(it) }
      ?: throw IllegalArgumentException("Cannot save number of children for prisoner")
    return SyncPrisonerDomesticStatusResponse(
      id = saved.prisonerDomesticStatusId,
      domesticStatusCode = saved.domesticStatusCode,
      createdBy = saved.createdBy,
      createdTime = saved.createdTime,
      active = saved.active,
    )
  }

  fun getPrisonerDomesticStatusActive(prisonerNumber: String) = domesticStatusRepository.findByPrisonerNumberAndActive(prisonerNumber, true)

  private fun validateReferenceDataExists(code: String) = referenceCodeRepository
    .findByGroupCodeAndCode(ReferenceCodeGroup.DOMESTIC_STS, code)
    ?: throw EntityNotFoundException("No reference data found for groupCode: ReferenceCodeGroup.DOMESTIC_STS and code: $code")
}
