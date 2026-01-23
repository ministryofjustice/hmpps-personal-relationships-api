package uk.gov.justice.digital.hmpps.personalrelationships.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.personalrelationships.entity.PrisonerDomesticStatus

@Repository
interface PrisonerDomesticStatusRepository : JpaRepository<PrisonerDomesticStatus, Long> {
  fun findByPrisonerNumberAndActiveTrue(prisonerNumber: String): PrisonerDomesticStatus?
  fun findByPrisonerNumberAndActiveFalse(prisonerNumber: String): List<PrisonerDomesticStatus>
  fun deleteByPrisonerNumber(prisonerNumber: String)
}
