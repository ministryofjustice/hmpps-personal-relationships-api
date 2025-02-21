package uk.gov.justice.digital.hmpps.hmppscontactsapi.service.sync

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.PrisonerDomesticStatus
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync.SyncUpdatePrisonerDomesticStatusRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.sync.SyncPrisonerDomesticStatusResponse
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.PrisonerDomesticStatusRepository
import java.time.LocalDateTime

@Service
@Transactional
class SyncPrisonerDomesticStatusService(
  private val domesticStatusRepository: PrisonerDomesticStatusRepository,
) {
  companion object {
    const val NOT_FOUND_MESSAGE = "Domestic status not found for prisoner: %s"
    fun from(numberOfChildren: PrisonerDomesticStatus) = SyncPrisonerDomesticStatusResponse(
      id = numberOfChildren.prisonerDomesticStatusId,
    )
  }

  fun getDomesticStatusByPrisonerNumber(prisonerNumber: String): SyncPrisonerDomesticStatusResponse = domesticStatusRepository.findByPrisonerNumberAndActive(prisonerNumber, true)
    ?.let { from(it) }
    ?: throw EntityNotFoundException(String.format(NOT_FOUND_MESSAGE, prisonerNumber))

  @Transactional
  fun createOrUpdateDomesticStatus(
    prisonerNumber: String,
    request: SyncUpdatePrisonerDomesticStatusRequest,
  ): SyncPrisonerDomesticStatusResponse {
    // Find existing status
    val existingStatus = domesticStatusRepository.findByPrisonerNumberAndActive(prisonerNumber, true)

    // If exists, deactivate it
    existingStatus?.let {
      val deactivatedStatus = it.copy(
        active = false,
      )
      domesticStatusRepository.save(deactivatedStatus)
    }

    // Create new active status
    // todo check if this need further validation
    val newDomesticStatus = request.domesticStatusCode?.let {
      PrisonerDomesticStatus(
        prisonerNumber = prisonerNumber,
        domesticStatusCode = request.domesticStatusCode,
        createdBy = request.createdBy,
        createdTime = LocalDateTime.now(),
        active = true,
      )
    }

    // todo check if this is right
    val saved = newDomesticStatus?.let { domesticStatusRepository.save(it) }
      ?: throw IllegalArgumentException("Cannot save number of children for prisoner")
    return SyncPrisonerDomesticStatusResponse(saved.prisonerDomesticStatusId)
  }

  @Transactional
  fun deactivateDomesticStatus(prisonerNumber: String): SyncPrisonerDomesticStatusResponse {
    val rowToDeactivate = domesticStatusRepository.findByPrisonerNumberAndActive(prisonerNumber, true)
      ?: throw EntityNotFoundException(String.format(NOT_FOUND_MESSAGE, prisonerNumber))

    // If exists, deactivate it
    rowToDeactivate.let {
      val deactivatedStatus = it.copy(
        active = false,
      )
      domesticStatusRepository.save(deactivatedStatus)
    }
    return SyncPrisonerDomesticStatusResponse(rowToDeactivate.prisonerDomesticStatusId)
  }
}
