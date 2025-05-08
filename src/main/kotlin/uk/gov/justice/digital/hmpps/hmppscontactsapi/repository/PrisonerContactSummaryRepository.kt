package uk.gov.justice.digital.hmpps.hmppscontactsapi.repository

import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.PrisonerContactSummaryEntity

@Repository
interface PrisonerContactSummaryRepository : ReadOnlyRepository<PrisonerContactSummaryEntity, Long> {
  fun findByContactId(contactId: Long): List<PrisonerContactSummaryEntity>
  fun findByPrisonerNumberAndContactId(prisonerNumber: String, contactId: Long): List<PrisonerContactSummaryEntity>
}
