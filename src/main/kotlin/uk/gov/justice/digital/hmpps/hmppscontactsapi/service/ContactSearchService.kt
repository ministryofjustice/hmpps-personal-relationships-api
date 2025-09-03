package uk.gov.justice.digital.hmpps.hmppscontactsapi.service

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppscontactsapi.mapping.toModel
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.ContactSearchRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.ContactSearchResultItem
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.ExistingRelationshipToPrisoner
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.ContactSearchRepository
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.PrisonerContactSummaryRepository

@Service
class ContactSearchService(
  private val contactSearchRepository: ContactSearchRepository,
  private val prisonerContactSummaryRepository: PrisonerContactSummaryRepository,
) {

  fun searchContacts(pageable: Pageable, request: ContactSearchRequest): Page<ContactSearchResultItem> {
    val checkForExistingRelationships = request.includeAnyExistingRelationshipsToPrisoner != null
    val matchingContactsPage = contactSearchRepository.searchContactsBySoundex(request, pageable)
    val contactExistingRelationships: Map<Long, List<ExistingRelationshipToPrisoner>> =
      if (checkForExistingRelationships) {
        val contactIds = matchingContactsPage.content.map { it.contactId }
        prisonerContactSummaryRepository
          .findByPrisonerNumberAndContactIdIn(request.includeAnyExistingRelationshipsToPrisoner!!, contactIds)
          .groupBy { it.contactId }
          .mapValues {
            it.value.map { summary ->
              ExistingRelationshipToPrisoner(
                prisonerContactId = summary.prisonerContactId,
                relationshipTypeCode = summary.relationshipType,
                relationshipTypeDescription = summary.relationshipTypeDescription,
                relationshipToPrisonerCode = summary.relationshipToPrisoner,
                relationshipToPrisonerDescription = summary.relationshipToPrisonerDescription,
                isRelationshipActive = summary.active,
              )
            }
          }
      } else {
        emptyMap()
      }
    return matchingContactsPage.map {
      val existingRelationships: List<ExistingRelationshipToPrisoner>? = if (checkForExistingRelationships) {
        contactExistingRelationships[it.contactId] ?: emptyList()
      } else {
        null
      }
      it.toModel(existingRelationships)
    }
  }
}
