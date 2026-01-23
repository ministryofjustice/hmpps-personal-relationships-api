package uk.gov.justice.digital.hmpps.personalrelationships.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.personalrelationships.entity.ContactWithFixedIdEntity

@Repository
interface ContactWithFixedIdRepository : JpaRepository<ContactWithFixedIdEntity, Long> {
  @Modifying
  @Query("delete from ContactWithFixedIdEntity c where c.contactId = :contactId")
  fun deleteAllByContactId(contactId: Long): Int
}
