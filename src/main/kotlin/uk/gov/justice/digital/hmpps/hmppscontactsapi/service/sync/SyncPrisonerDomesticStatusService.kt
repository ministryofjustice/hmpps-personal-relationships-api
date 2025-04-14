package uk.gov.justice.digital.hmpps.hmppscontactsapi.service.sync

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.PrisonerDomesticStatus
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.ReferenceCodeGroup
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync.SyncUpdatePrisonerDomesticStatusRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.sync.Status
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.sync.SyncPrisonerDomesticStatusResponse
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.sync.SyncPrisonerDomesticStatusResponseData
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
    const val NOT_FOUND_MESSAGE = "No active domestic status found for prisoner: %s"
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
  ): SyncPrisonerDomesticStatusResponseData {
    request.domesticStatusCode?.let { validateReferenceDataExists(it) }

    val existingStatus = getPrisonerDomesticStatusActive(prisonerNumber)

    // If an existing domestic status is found and differs from the request:
    //   - Deactivate the existing status and create a new one
    // If no existing domestic status is found:
    //   - Create a new domestic status
    // If existing domestic status matches the request:
    //   - Return the existing status unchanged

    if (existingStatus != null) {
      if (existingStatus.domesticStatusCode == request.domesticStatusCode) {
        return SyncPrisonerDomesticStatusResponseData(from(existingStatus), Status.UNCHANGED)
      }

      val deactivatedStatus = existingStatus.copy(
        active = false,
      )
      domesticStatusRepository.save(deactivatedStatus)
    }

    // Create new active status
    val newDomesticStatus = PrisonerDomesticStatus(
      prisonerNumber = prisonerNumber,
      domesticStatusCode = request.domesticStatusCode,
      createdBy = request.createdBy,
      createdTime = LocalDateTime.now(),
      active = true,
    )

    val saved = domesticStatusRepository.save(newDomesticStatus)
      ?: throw IllegalArgumentException("Cannot save domestic status for prisoner")

    // set UPDATED status if existingStatus is not null and request domestic status value is not same as existing value , return created if existing status is null
    if (existingStatus != null) {
      return SyncPrisonerDomesticStatusResponseData(from(saved), Status.UPDATED, updatedId = existingStatus.prisonerDomesticStatusId)
    }
    return SyncPrisonerDomesticStatusResponseData(from(saved), Status.CREATED)
  }

  fun getPrisonerDomesticStatusActive(prisonerNumber: String) = domesticStatusRepository.findByPrisonerNumberAndActiveTrue(
    prisonerNumber,
  )

  private fun validateReferenceDataExists(code: String) = referenceCodeRepository
    .findByGroupCodeAndCode(ReferenceCodeGroup.DOMESTIC_STS, code)
    ?: throw EntityNotFoundException("No reference data found for groupCode: ReferenceCodeGroup.DOMESTIC_STS and code: $code")
}
