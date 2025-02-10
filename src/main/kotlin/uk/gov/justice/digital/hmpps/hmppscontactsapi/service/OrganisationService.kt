package uk.gov.justice.digital.hmpps.hmppscontactsapi.service

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppscontactsapi.client.organisations.OrganisationsApiClient
import uk.gov.justice.digital.hmpps.hmppscontactsapi.client.organisationsapi.model.OrganisationSummary

@Service
class OrganisationService(
  private val organisationsApiClient: OrganisationsApiClient,
) {

  fun getOrganisationSummaryById(id: Long): OrganisationSummary {
    val entity = organisationsApiClient.getOrganisationSummary(id)
      ?: throw EntityNotFoundException("Organisation with id $id not found")
    return entity
  }
}
