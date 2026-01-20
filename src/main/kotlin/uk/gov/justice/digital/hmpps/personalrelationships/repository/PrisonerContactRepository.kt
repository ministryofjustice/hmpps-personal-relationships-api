package uk.gov.justice.digital.hmpps.personalrelationships.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.personalrelationships.entity.PrisonerContactEntity
import java.time.LocalDateTime

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

  data class RelationshipTypeCountProjection(
    val contactId: Long,
    val relationshipType: String,
    val relationshipToPrisoner: String,
    val count: Long,
  )

  @Query(
    """
    SELECT c.contactId, pc.relationshipType, pc.relationshipToPrisoner, count(*) as count
    FROM ContactEntity c, PrisonerContactEntity pc
    WHERE c.dateOfBirth is not null
    AND pc.contactId = c.contactId
    AND c.contactId IN (
      SELECT DISTINCT ac.contactId
      FROM ContactEntity ac, PrisonerContactEntity apc
      WHERE ac.dateOfBirth is not null
      AND apc.contactId = ac.contactId
      AND apc.relationshipType = 'O'
      AND apc.relationshipToPrisoner in :relationshipTypes
    )
    group by c.contactId, pc.relationshipType, pc.relationshipToPrisoner
    order by c.contactId, pc.relationshipType, pc.relationshipToPrisoner
    """,
  )
  fun findAllContactsWithADobInRelationships(relationshipTypes: List<String>): List<RelationshipTypeCountProjection>

  @Query(
    """
        select pc 
        from PrisonerContactEntity pc
        where pc.currentTerm = true
          and pc.approvedVisitor = false
          and pc.createdBy in :createdByList
          and pc.createdTime > :createdAfter
        order by pc.createdTime  
    """,
  )
  fun getRelationshipsToApprove(createdAfter: LocalDateTime, createdByList: List<String>): List<PrisonerContactEntity>
}
