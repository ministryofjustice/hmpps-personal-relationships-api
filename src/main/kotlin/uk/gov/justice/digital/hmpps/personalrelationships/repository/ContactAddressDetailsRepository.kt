package uk.gov.justice.digital.hmpps.personalrelationships.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.personalrelationships.entity.ContactAddressDetailsEntity

@Repository
interface ContactAddressDetailsRepository : JpaRepository<ContactAddressDetailsEntity, Long> {

  fun findByContactId(contactId: Long): List<ContactAddressDetailsEntity>
}
