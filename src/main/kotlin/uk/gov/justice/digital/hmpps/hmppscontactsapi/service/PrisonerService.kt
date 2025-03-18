package uk.gov.justice.digital.hmpps.hmppscontactsapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppscontactsapi.client.prisonersearch.Prisoner
import uk.gov.justice.digital.hmpps.hmppscontactsapi.client.prisonersearch.PrisonerSearchClient

@Service
class PrisonerService(private val prisonerSearchClient: PrisonerSearchClient) {
  fun getPrisoner(prisonerNumber: String): Prisoner? = prisonerSearchClient.getPrisoner(prisonerNumber)
  fun getPrisoners(prisonerNumbers: Set<String>): List<Prisoner> = prisonerSearchClient.getPrisoners(prisonerNumbers)
}
