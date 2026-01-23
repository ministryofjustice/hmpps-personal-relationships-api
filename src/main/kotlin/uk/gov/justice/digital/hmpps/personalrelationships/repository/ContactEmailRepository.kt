package uk.gov.justice.digital.hmpps.personalrelationships.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import uk.gov.justice.digital.hmpps.personalrelationships.entity.ContactEmailEntity

interface ContactEmailRepository : JpaRepository<ContactEmailEntity, Long> {
  fun findByContactId(contactId: Long): List<ContactEmailEntity>
  fun findByContactIdAndContactEmailId(contactId: Long, contactIdentityId: Long): ContactEmailEntity?

  @Modifying
  @Query("delete from ContactEmailEntity ce where ce.contactId = :contactId")
  fun deleteAllByContactId(contactId: Long): Int
}
