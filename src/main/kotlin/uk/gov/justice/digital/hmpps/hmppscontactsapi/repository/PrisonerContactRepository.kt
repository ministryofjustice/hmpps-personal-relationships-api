package uk.gov.justice.digital.hmpps.hmppscontactsapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.PrisonerContactEntity

@Repository
interface PrisonerContactRepository : JpaRepository<PrisonerContactEntity, Long> {
  fun findAllByContactId(contactId: Long): List<PrisonerContactEntity>

  @Modifying
  @Query("delete from PrisonerContactEntity pc where pc.contactId = :contactId")
  fun deleteAllByContactId(contactId: Long): Int

  fun findAllByPrisonerNumber(prisonerNumber: String): List<PrisonerContactEntity>

  @Modifying
  @Query("delete from PrisonerContactEntity pc where pc.prisonerNumber = :prisonerNumber")
  fun deleteAllByPrisonerNumber(prisonerNumber: String): Int

  @Query(
    """
    SELECT pc FROM PrisonerContactEntity pc 
    WHERE pc.prisonerNumber = :prisonerNumber 
    AND pc.contactId = :contactId
    AND pc.relationshipToPrisoner = :relationshipToPrisoner
    AND pc.currentTerm = true""",
  )
  fun findDuplicateRelationships(prisonerNumber: String, contactId: Long, relationshipToPrisoner: String): List<PrisonerContactEntity>
}
