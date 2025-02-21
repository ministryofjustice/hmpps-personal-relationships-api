package uk.gov.justice.digital.hmpps.hmppscontactsapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.PrisonerNumberOfChildren

@Repository
interface PrisonerNumberOfChildrenRepository : JpaRepository<PrisonerNumberOfChildren, Long> {
  fun findByPrisonerNumber(prisonerNumber: String): PrisonerNumberOfChildren?
  fun findByPrisonerNumberAndActive(prisonerNumber: String, active: Boolean): PrisonerNumberOfChildren?
}
