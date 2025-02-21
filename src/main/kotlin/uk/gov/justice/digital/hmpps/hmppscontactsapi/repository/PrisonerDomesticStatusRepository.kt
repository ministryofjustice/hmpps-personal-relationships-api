package uk.gov.justice.digital.hmpps.hmppscontactsapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.PrisonerDomesticStatus

@Repository
interface PrisonerDomesticStatusRepository : JpaRepository<PrisonerDomesticStatus, Long> {
  fun findByPrisonerNumber(prisonerNumber: String): PrisonerDomesticStatus?
  fun findByPrisonerNumberAndActive(prisonerNumber: String, active: Boolean): PrisonerDomesticStatus?
}
