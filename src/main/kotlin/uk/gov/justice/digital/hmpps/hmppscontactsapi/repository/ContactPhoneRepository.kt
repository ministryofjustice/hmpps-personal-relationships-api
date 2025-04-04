package uk.gov.justice.digital.hmpps.hmppscontactsapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.ContactPhoneEntity

interface ContactPhoneRepository : JpaRepository<ContactPhoneEntity, Long> {
  fun findByContactId(contactId: Long): List<ContactPhoneEntity>

  @Modifying
  @Query("delete from ContactPhoneEntity cp where cp.contactId = :contactId")
  fun deleteAllByContactId(contactId: Long): Int
}
