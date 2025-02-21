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
    var current: Long = 1L

    // Save historical records and map them to details
    val history = request.history.map {
      val savedEntity = prisonerNumberOfChildrenRepository.save(
        PrisonerNumberOfChildren(
          numberOfChildren = it.numberOfChildren,
          prisonerNumber = request.prisonerNumber,
          createdBy = it.createdBy,
          createdTime = it.createdTime,
          active = false,
        ),
      )
      savedEntity.prisonerNumberOfChildrenId.also { details -> current = details }
    }

    // Save current numberOfChildren if provided
    request.current?.let {
      val savedEntity = prisonerNumberOfChildrenRepository.save(
        PrisonerNumberOfChildren(
          prisonerNumber = request.prisonerNumber,
          numberOfChildren = it.numberOfChildren,
          createdBy = it.createdBy,
          createdTime = it.createdTime,
          active = true,
        ),
      )
      current = savedEntity.prisonerNumberOfChildrenId
    }

    return PrisonerNumberOfChildrenMigrationResponse(
      prisonerNumber = request.prisonerNumber,
      current = current,
      history = history,
    )
  }
}
