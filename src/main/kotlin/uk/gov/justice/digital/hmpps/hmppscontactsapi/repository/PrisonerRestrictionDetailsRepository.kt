package uk.gov.justice.digital.hmpps.hmppscontactsapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.PrisonerRestrictionDetailsEntity

interface PrisonerRestrictionDetailsRepository : JpaRepository<PrisonerRestrictionDetailsEntity, Long> {

  fun findByPrisonerNumber(prisonerNumber: String): List<PrisonerRestrictionDetailsEntity>
}
