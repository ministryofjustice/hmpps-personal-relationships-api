package uk.gov.justice.digital.hmpps.personalrelationships.repository

import org.springframework.data.jpa.repository.JpaRepository
import uk.gov.justice.digital.hmpps.personalrelationships.entity.ContactRestrictionDetailsEntity

interface ContactRestrictionDetailsRepository : JpaRepository<ContactRestrictionDetailsEntity, Long> {

  fun findAllByContactId(contactId: Long): List<ContactRestrictionDetailsEntity>
}
