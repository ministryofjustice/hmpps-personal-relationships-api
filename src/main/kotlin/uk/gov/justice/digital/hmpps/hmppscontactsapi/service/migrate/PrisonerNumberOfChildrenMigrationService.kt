package uk.gov.justice.digital.hmpps.hmppscontactsapi.service.migrate

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.PrisonerNumberOfChildren
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.migrate.MigratePrisonerNumberOfChildrenRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.migrate.PrisonerNumberOfChildrenMigrationResponse
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.PrisonerNumberOfChildrenRepository

@Service
class PrisonerNumberOfChildrenMigrationService(
  private val prisonerNumberOfChildrenRepository: PrisonerNumberOfChildrenRepository,
) {

  fun migrateNumberOfChildren(request: MigratePrisonerNumberOfChildrenRequest): PrisonerNumberOfChildrenMigrationResponse {
    val entitiesToSave = buildList {
      addAll(
        request.history.map {
          PrisonerNumberOfChildren(
            numberOfChildren = it.numberOfChildren,
            prisonerNumber = request.prisonerNumber,
            createdBy = it.createdBy,
            createdTime = it.createdTime,
            active = false,
          )
        },
      )
      // Save current numberOfChildren if provided
      request.current?.let {
        add(
          PrisonerNumberOfChildren(
            prisonerNumber = request.prisonerNumber,
            numberOfChildren = it.numberOfChildren,
            createdBy = it.createdBy,
            createdTime = it.createdTime,
            active = true,
          ),
        )
      }
    }

    // Batch save all entities
    val savedEntities = prisonerNumberOfChildrenRepository.saveAll(entitiesToSave)

    return PrisonerNumberOfChildrenMigrationResponse(
      prisonerNumber = request.prisonerNumber,
      current = savedEntities.find { it.active }?.prisonerNumberOfChildrenId,
      history = savedEntities.filter { !it.active }.map { it.prisonerNumberOfChildrenId },
    )
  }
}
