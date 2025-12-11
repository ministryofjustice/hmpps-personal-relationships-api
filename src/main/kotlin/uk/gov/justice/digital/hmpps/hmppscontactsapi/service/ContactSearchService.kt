package uk.gov.justice.digital.hmpps.hmppscontactsapi.service

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.ContactEntity
import uk.gov.justice.digital.hmpps.hmppscontactsapi.mapping.toModel
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.AdvancedContactSearchRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.ContactSearchRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.AdvancedContactSearchResultItem
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.ContactSearchResultItem
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.ExistingRelationshipToPrisoner
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.ContactAdvancedSearchRepository
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.ContactIdentitySearchRepository
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.ContactSearchRepository
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.PrisonerContactSummaryRepository
import java.time.LocalDate
import kotlin.collections.get

@Service
class ContactSearchService(
  private val contactSearchRepository: ContactSearchRepository,
  private val contactAdvancedSearchRepository: ContactAdvancedSearchRepository,
  private val contactIdentitySearchRepository: ContactIdentitySearchRepository,
  private val prisonerContactSummaryRepository: PrisonerContactSummaryRepository,
) {

  @Transactional(readOnly = true)
  fun searchContacts(pageable: Pageable, request: ContactSearchRequest): Page<ContactSearchResultItem> {
    val prisonerNumber = request.includeAnyExistingRelationshipsToPrisoner
    val matchingContactsPage = contactSearchRepository.searchContacts(request, pageable)

    val contactIdList = matchingContactsPage.content.map { it.contactId!! }
    val contactExistingRelationships = if (!prisonerNumber.isNullOrBlank()) {
      mapExistingRelationshipToPrisoner(prisonerNumber, contactIdList)
    } else {
      emptyMap()
    }
    return matchingContactsPage.map { contactWithAddressEntity ->
      val existingRelationships: List<ExistingRelationshipToPrisoner>? =
        if (!prisonerNumber.isNullOrBlank()) {
          contactExistingRelationships[contactWithAddressEntity.contactId]
            ?: emptyList()
        } else {
          null
        }

      contactWithAddressEntity.toModel(existingRelationships)
    }
  }

  @Transactional(readOnly = true)
  fun advancedContactSearch(
    pageable: Pageable,
    request: AdvancedContactSearchRequest,
  ): Page<AdvancedContactSearchResultItem> {
    val prisonerNumber = request.includeAnyExistingRelationshipsToPrisoner
    val matchingContactsPage = performAdvancedSearch(request, pageable)

    val contactIdList = matchingContactsPage.content.map { it.contactId!! }
    val contactExistingRelationships = if (!prisonerNumber.isNullOrBlank()) {
      mapExistingRelationshipToPrisoner(prisonerNumber, contactIdList)
    } else {
      emptyMap()
    }

    return matchingContactsPage.map { contactEntity ->
      val existingRelationships: List<ExistingRelationshipToPrisoner>? =
        if (!prisonerNumber.isNullOrBlank()) {
          contactExistingRelationships[contactEntity.contactId]
            ?: emptyList()
        } else {
          null
        }

      contactEntity.toModel(existingRelationships)
    }
  }

  @Transactional(readOnly = true)
  fun searchContactsByIdPartialMatch(
    contactId: String,
    dateOfBirth: LocalDate?,
    includeAnyExistingRelationshipsToPrisoner: String?,
    pageable: Pageable,
  ): Page<AdvancedContactSearchResultItem> {
    val prisonerNumber = includeAnyExistingRelationshipsToPrisoner
    val matchingContactsPage = contactIdentitySearchRepository.searchContactsByIdPartialMatch(
      contactId,
      dateOfBirth,
      pageable,
    )

    val contactIdList = matchingContactsPage.content.map { it.contactId!! }
    val contactExistingRelationships = if (!prisonerNumber.isNullOrBlank()) {
      mapExistingRelationshipToPrisoner(prisonerNumber, contactIdList)
    } else {
      emptyMap()
    }

    return matchingContactsPage.map { contactEntity ->
      val existingRelationships: List<ExistingRelationshipToPrisoner>? =
        if (!prisonerNumber.isNullOrBlank()) {
          contactExistingRelationships[contactEntity.contactId]
            ?: emptyList()
        } else {
          null
        }

      contactEntity.toModel(existingRelationships)
    }
  }

  private fun performAdvancedSearch(
    request: AdvancedContactSearchRequest,
    pageable: Pageable,
  ): Page<ContactEntity> = when {
    request.soundsLike -> contactAdvancedSearchRepository.phoneticSearchContacts(request, pageable)
    else -> contactAdvancedSearchRepository.likeSearchContacts(request, pageable)
  }

  private fun mapExistingRelationshipToPrisoner(
    prisonerNumber: String,
    contactIdList: List<Long>,
  ): Map<Long, List<ExistingRelationshipToPrisoner>> {
    if (contactIdList.isEmpty()) return emptyMap()

    return prisonerContactSummaryRepository
      .findByPrisonerNumberAndContactIdIn(prisonerNumber, contactIdList)
      .groupBy { it.contactId }
      .mapValues { entry ->
        entry.value.map { summary ->
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
  }
}
