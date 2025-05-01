package uk.gov.justice.digital.hmpps.hmppscontactsapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.PrisonerContactRestrictionEntity

@Repository
interface PrisonerContactRestrictionRepository : JpaRepository<PrisonerContactRestrictionEntity, Long> {
  fun findAllByPrisonerContactId(prisonerContactId: Long): List<PrisonerContactRestrictionEntity>

  @Modifying
  @Query("delete from PrisonerContactRestrictionEntity pcr where pcr.prisonerContactId = :prisonerContactId")
  fun deleteAllByPrisonerContactId(prisonerContactId: Long): Int

  @Modifying
  @Query(
    """
    delete from PrisonerContactRestrictionEntity pcr 
    where pcr.prisonerContactId in (
      select pc.prisonerContactId from PrisonerContactEntity pc where pc.contactId = :contactId
    )
    """,
  )
  fun deleteAllByContactId(contactId: Long): Int
}
