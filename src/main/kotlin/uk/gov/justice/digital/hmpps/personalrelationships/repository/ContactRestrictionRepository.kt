package uk.gov.justice.digital.hmpps.personalrelationships.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import uk.gov.justice.digital.hmpps.personalrelationships.entity.ContactRestrictionEntity

interface ContactRestrictionRepository : JpaRepository<ContactRestrictionEntity, Long> {
  fun findByContactId(contactId: Long): List<ContactRestrictionEntity>

  @Modifying
  @Query("delete from ContactRestrictionEntity cr where cr.contactId = :contactId")
  fun deleteAllByContactId(contactId: Long): Int
}
