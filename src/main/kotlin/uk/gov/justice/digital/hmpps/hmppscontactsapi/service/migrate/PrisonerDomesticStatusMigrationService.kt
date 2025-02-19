package uk.gov.justice.digital.hmpps.hmppscontactsapi.service.migrate

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.PrisonerDomesticStatus
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.migrate.MigratePrisonerDomesticStatusRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.migrate.DomesticStatusDetailsResponse
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.migrate.PrisonerDomesticStatusMigrationResponse
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.PrisonerDomesticStatusRepository

@Service
class PrisonerDomesticStatusMigrationService(
  private val prisonerDomesticStatusRepository: PrisonerDomesticStatusRepository,
) {

  fun migrateDomesticStatus(request: MigratePrisonerDomesticStatusRequest): PrisonerDomesticStatusMigrationResponse {
    lateinit var current: DomesticStatusDetailsResponse

    // Save historical records and map them to details
    val history = request.history.map {
      val savedEntity = prisonerDomesticStatusRepository.save(
        PrisonerDomesticStatus(
          prisonerNumber = request.prisonerNumber,
          domesticStatusCode = it.domesticStatusCode,
          createdBy = it.createdBy,
          createdTime = it.createdTime,
          active = false,
        ),
      )
      DomesticStatusDetailsResponse(
        id = savedEntity.prisonerDomesticStatusId,
        domesticStatusCode = it.domesticStatusCode,
        createdBy = it.createdBy,
        createdTime = it.createdTime,
      ).also { details -> current = details }
    }

    // Save current status if provided
    request.current?.let {
      val savedEntity = prisonerDomesticStatusRepository.save(
        PrisonerDomesticStatus(
          prisonerNumber = request.prisonerNumber,
          domesticStatusCode = it.domesticStatusCode,
          createdBy = it.createdBy,
          createdTime = it.createdTime,
          active = true,
        ),
      )
      current = DomesticStatusDetailsResponse(
        id = savedEntity.prisonerDomesticStatusId,
        domesticStatusCode = it.domesticStatusCode,
        createdBy = it.createdBy,
        createdTime = it.createdTime,
      )
    }

    return PrisonerDomesticStatusMigrationResponse(
      prisonerNumber = request.prisonerNumber,
      current = current,
      history = history,
    )
  }
}
