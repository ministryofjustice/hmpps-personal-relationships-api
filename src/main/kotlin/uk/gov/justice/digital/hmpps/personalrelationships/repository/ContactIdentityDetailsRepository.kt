package uk.gov.justice.digital.hmpps.personalrelationships.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.personalrelationships.entity.ContactIdentityDetailsEntity

@Repository
interface ContactIdentityDetailsRepository : JpaRepository<ContactIdentityDetailsEntity, Long> {

  fun findByContactId(contactId: Long): List<ContactIdentityDetailsEntity>
  fun findByContactIdAndContactIdentityId(contactId: Long, contactIdentityId: Long): ContactIdentityDetailsEntity?
}
