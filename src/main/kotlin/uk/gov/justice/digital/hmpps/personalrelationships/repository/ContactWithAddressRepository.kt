package uk.gov.justice.digital.hmpps.personalrelationships.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.personalrelationships.entity.ContactWithAddressEntity

@Repository
interface ContactWithAddressRepository : JpaRepository<ContactWithAddressEntity, Long> {
  @Query(
    """
   select c 
   from ContactWithAddressEntity c
   where c.contactId in (:contactIds) 
   """,
  )
  fun findAllWhereContactIdIn(contactIds: Collection<Long>, pageable: Pageable): Page<ContactWithAddressEntity>

  @Query(
    """
   select c 
   from ContactWithAddressEntity c
   where c.contactId in (:contactIds) 
   """,
  )
  fun findAllWhereContactIdUnpaginated(contactIds: Collection<Long>, sort: Sort): List<ContactWithAddressEntity>
}
