package uk.gov.justice.digital.hmpps.personalrelationships.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.personalrelationships.entity.PrisonerContactRestrictionCountsEntity

@Repository
interface PrisonerContactRestrictionCountsRepository : JpaRepository<PrisonerContactRestrictionCountsEntity, Long> {
  fun findAllByPrisonerContactIdIn(prisonerContactIds: Set<Long>): List<PrisonerContactRestrictionCountsEntity>
}
