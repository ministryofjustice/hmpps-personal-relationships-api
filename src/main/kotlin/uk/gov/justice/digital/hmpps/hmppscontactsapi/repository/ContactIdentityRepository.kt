package uk.gov.justice.digital.hmpps.hmppscontactsapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.ContactIdentityEntity

interface ContactIdentityRepository : JpaRepository<ContactIdentityEntity, Long> {
  fun findByContactId(contactId: Long): List<ContactIdentityEntity>

  @Modifying
  @Query("delete from ContactIdentityEntity ci where ci.contactId = :contactId")
  fun deleteAllByContactId(contactId: Long): Int
}
