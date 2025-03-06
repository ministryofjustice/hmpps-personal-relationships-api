package uk.gov.justice.digital.hmpps.hmppscontactsapi.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.PrisonerContactSummaryEntity

@Repository
interface PrisonerContactSummaryRepository : ReadOnlyRepository<PrisonerContactSummaryEntity, Long> {
  fun findByPrisonerNumberAndActive(prisonerNumber: String, active: Boolean, pageable: Pageable): Page<PrisonerContactSummaryEntity>
  fun findByContactId(contactId: Long): List<PrisonerContactSummaryEntity>
}
