package uk.gov.justice.digital.hmpps.personalrelationships.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.personalrelationships.entity.PrisonerRestriction

@Repository
interface PrisonerRestrictionsRepository : JpaRepository<PrisonerRestriction, Long> {
  // native so that previously soft deleted rows are removed too (the entity restriction would hide them)
  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query("delete from prisoner_restrictions where prisoner_number = :prisonerNumber", nativeQuery = true)
  fun deleteByPrisonerNumber(@Param("prisonerNumber") prisonerNumber: String)

  fun findByPrisonerNumber(prisonerNumber: String): List<PrisonerRestriction>
  fun findAllBy(pageable: org.springframework.data.domain.Pageable): org.springframework.data.domain.Page<PrisonerRestriction>
}
