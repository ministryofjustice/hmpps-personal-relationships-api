package uk.gov.justice.digital.hmpps.hmppscontactsapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.PrisonerDomesticStatus

@Repository
interface PrisonerDomesticStatusRepository : JpaRepository<PrisonerDomesticStatus, Long> {
  fun findByPrisonerNumberAndActiveTrue(prisonerNumber: String): PrisonerDomesticStatus?
  fun findByPrisonerNumberAndActiveFalse(prisonerNumber: String): List<PrisonerDomesticStatus>
  fun deleteByPrisonerNumber(prisonerNumber: String)

  @Modifying
  @Query("UPDATE PrisonerDomesticStatus p SET p.active = false WHERE p.prisonerNumber = :prisonerNumber AND p.active = true")
  fun deactivateExistingActiveRecord(prisonerNumber: String)
}
