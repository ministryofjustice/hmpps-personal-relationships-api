package uk.gov.justice.digital.hmpps.hmppscontactsapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.PrisonerRestriction

@Repository
interface PrisonerRestrictionsRepository : JpaRepository<PrisonerRestriction, Long> {
  fun deleteByPrisonerNumber(prisonerNumber: String)
  fun findByPrisonerNumber(prisonerNumber: String): List<PrisonerRestriction>
  fun findAllBy(pageable: org.springframework.data.domain.Pageable): org.springframework.data.domain.Page<PrisonerRestriction>
}
