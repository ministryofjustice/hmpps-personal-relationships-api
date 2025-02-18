package uk.gov.justice.digital.hmpps.hmppscontactsapi.service

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.PrisonerDomesticStatus
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.ReferenceCodeGroup
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.UpdatePrisonerDomesticStatusRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.PrisonerDomesticStatusResponse
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.PrisonerDomesticStatusRepository
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.ReferenceCodeRepository
import java.time.LocalDateTime

@Service
class PrisonerDomesticStatusService(
    private val prisonerDomesticStatusRepository: PrisonerDomesticStatusRepository,
    private val referenceCodeRepository: ReferenceCodeRepository,
) {
    fun getDomesticStatus(prisonerNumber: String): PrisonerDomesticStatusResponse =
        prisonerDomesticStatusRepository.findByPrisonerNumberAndActive(prisonerNumber, true)
            ?.let { mapToResponse(it) }
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
        request: UpdatePrisonerDomesticStatusRequest,
    ): PrisonerDomesticStatusResponse {
        checkReferenceDataExists(request.domesticStatusCode)

        // Find existing status, If exists, deactivate it
        prisonerDomesticStatusRepository.findByPrisonerNumberAndActive(prisonerNumber, true)?.let {
            val deactivatedStatus = it.copy(
                active = false,
            )
            prisonerDomesticStatusRepository.save(deactivatedStatus)
        }

        // Create new active status
        val newDomesticStatus = PrisonerDomesticStatus(
            prisonerNumber = prisonerNumber,
            domesticStatusCode = request.domesticStatusCode,
            createdBy = request.updatedBy,
            createdTime = LocalDateTime.now(),
            active = true,
        )

        // Save and return the new status
        return mapToResponse(prisonerDomesticStatusRepository.save(newDomesticStatus))
    }

    private fun mapToResponse(entity: PrisonerDomesticStatus): PrisonerDomesticStatusResponse =
        PrisonerDomesticStatusResponse(
            id = entity.id,
            prisonerNumber = entity.prisonerNumber,
            domesticStatusValue = entity.domesticStatusCode,
            domesticStatusDescription = referenceCodeRepository.findByGroupCodeAndCode(
                ReferenceCodeGroup.DOMESTIC_STS,
                entity.domesticStatusCode,
            )?.description,
            active = entity.active,
            createdTime = entity.createdTime,
            createdBy = entity.createdBy,
        )

    private fun checkReferenceDataExists(code: String) = referenceCodeRepository
        .findByGroupCodeAndCode(ReferenceCodeGroup.DOMESTIC_STS, code)
        ?: throw EntityNotFoundException("No reference data found for groupCode: DOMESTIC_STS and code: $code")
}
