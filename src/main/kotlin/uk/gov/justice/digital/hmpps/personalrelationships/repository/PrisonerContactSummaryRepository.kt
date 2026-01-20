package uk.gov.justice.digital.hmpps.personalrelationships.repository

import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.personalrelationships.entity.PrisonerContactSummaryEntity

@Repository
interface PrisonerContactSummaryRepository : ReadOnlyRepository<PrisonerContactSummaryEntity, Long> {
  fun findByContactId(contactId: Long): List<PrisonerContactSummaryEntity>
  fun findByPrisonerNumberAndContactId(prisonerNumber: String, contactId: Long): List<PrisonerContactSummaryEntity>
  fun findByPrisonerNumberAndContactIdIn(prisonerNumber: String, contactIds: List<Long>): List<PrisonerContactSummaryEntity>
}
