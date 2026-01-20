package uk.gov.justice.digital.hmpps.personalrelationships.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.personalrelationships.entity.ContactPhoneDetailsEntity

@Repository
interface ContactPhoneDetailsRepository : JpaRepository<ContactPhoneDetailsEntity, Long> {

  fun findByContactId(contactId: Long): List<ContactPhoneDetailsEntity>

  fun findByContactIdAndContactPhoneId(contactId: Long, contactPhoneId: Long): ContactPhoneDetailsEntity?
}
