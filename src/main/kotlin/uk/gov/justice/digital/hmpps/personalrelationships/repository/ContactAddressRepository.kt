package uk.gov.justice.digital.hmpps.personalrelationships.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.personalrelationships.entity.ContactAddressEntity

@Repository
interface ContactAddressRepository : JpaRepository<ContactAddressEntity, Long> {
  fun findByContactId(contactId: Long): List<ContactAddressEntity>

  @Modifying
  @Query("delete from ContactAddressEntity ca where ca.contactId = :contactId")
  fun deleteAllByContactId(contactId: Long): Int

  @Modifying
  @Query(
    value = """
      UPDATE contact_address 
      SET primary_address = false 
      WHERE primary_address = true
      AND contact_id = :contactId 
      RETURNING contact_address_id
      """,
    nativeQuery = true,
  )
  fun resetPrimaryAddressFlagForContact(contactId: Long): List<Long>

  @Modifying
  @Query(
    value = """
      UPDATE contact_address 
      SET mail_flag = false 
      WHERE mail_flag = true
      AND contact_id = :contactId 
      RETURNING contact_address_id
      """,
    nativeQuery = true,
  )
  fun resetMailAddressFlagForContact(contactId: Long): List<Long>
}
