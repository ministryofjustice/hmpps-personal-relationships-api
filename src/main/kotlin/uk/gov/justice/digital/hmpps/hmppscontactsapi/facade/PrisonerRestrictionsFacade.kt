package uk.gov.justice.digital.hmpps.hmppscontactsapi.facade

import org.springframework.data.domain.Pageable
import org.springframework.data.web.PagedModel
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.PrisonerRestrictionDetails
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.PrisonerRestrictionsService

@Service
class PrisonerRestrictionsFacade(
  private val prisonerRestrictionsService: PrisonerRestrictionsService,
) {
  fun getPrisonerRestrictions(
    prisonerNumber: String,
    currentTermOnly: Boolean,
    pageable: Pageable,
    paged: Boolean,
  ): PagedModel<PrisonerRestrictionDetails> = prisonerRestrictionsService.getPrisonerRestrictions(prisonerNumber, currentTermOnly, pageable, paged)
}
