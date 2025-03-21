package uk.gov.justice.digital.hmpps.hmppscontactsapi.service

import jakarta.persistence.EntityNotFoundException
import org.springframework.data.domain.Page
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppscontactsapi.mapping.toModel
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.internal.PrisonerContactSearchParams
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.PrisonerContactSummary
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.PrisonerContactSearchRepository

@Service
class PrisonerContactService(
  private val prisonerContactSearchRepository: PrisonerContactSearchRepository,
  private val prisonerService: PrisonerService,
) {
  fun getAllContacts(params: PrisonerContactSearchParams): Page<PrisonerContactSummary> {
    prisonerService.getPrisoner(params.prisonerNumber)
      ?: throw EntityNotFoundException("Prisoner number ${params.prisonerNumber} - not found")
    return prisonerContactSearchRepository.searchPrisonerContacts(params).map { it.toModel() }
  }
}
