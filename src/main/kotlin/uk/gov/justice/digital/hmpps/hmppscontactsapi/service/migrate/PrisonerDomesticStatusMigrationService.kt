package uk.gov.justice.digital.hmpps.hmppscontactsapi.service.migrate

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.PrisonerDomesticStatus
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.migrate.MigratePrisonerDomesticStatusRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.migrate.PrisonerDomesticStatusMigrationResponse
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.PrisonerDomesticStatusRepository

@Service
class PrisonerDomesticStatusMigrationService(
  private val prisonerDomesticStatusRepository: PrisonerDomesticStatusRepository,
) {

  fun migrateDomesticStatus(request: MigratePrisonerDomesticStatusRequest): PrisonerDomesticStatusMigrationResponse {
    // Create list of entities to save
    val entitiesToSave = buildList {
      addAll(
        request.history.map {
          PrisonerDomesticStatus(
            prisonerNumber = request.prisonerNumber,
            domesticStatusCode = it.domesticStatusCode,
            createdBy = it.createdBy,
            createdTime = it.createdTime,
            active = false,
          )
        },
      )

      // Add current status if provided
      request.current?.let {
        add(
          PrisonerDomesticStatus(
            prisonerNumber = request.prisonerNumber,
            domesticStatusCode = it.domesticStatusCode,
            createdBy = it.createdBy,
            createdTime = it.createdTime,
            active = true,
          ),
        )
      }
    }

    // Batch save all entities
    val savedEntities = prisonerDomesticStatusRepository.saveAll(entitiesToSave)

    return PrisonerDomesticStatusMigrationResponse(
      prisonerNumber = request.prisonerNumber,
      current = savedEntities.find { it.active }?.prisonerDomesticStatusId,
      history = savedEntities.filter { !it.active }.map { it.prisonerDomesticStatusId },
    )
  }
}
