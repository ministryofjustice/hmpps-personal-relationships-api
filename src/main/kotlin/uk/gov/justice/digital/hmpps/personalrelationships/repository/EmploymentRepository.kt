package uk.gov.justice.digital.hmpps.personalrelationships.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.personalrelationships.entity.EmploymentEntity

@Repository
interface EmploymentRepository : JpaRepository<EmploymentEntity, Long> {
  fun findByContactId(contactId: Long): List<EmploymentEntity>

  @Modifying
  @Query("delete from EmploymentEntity e where e.contactId = :contactId")
  fun deleteAllByContactId(contactId: Long): Int
}
