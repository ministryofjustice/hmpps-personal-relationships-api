package uk.gov.justice.digital.hmpps.hmppscontactsapi.service

import io.netty.util.internal.StringUtil.isNullOrEmpty
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppscontactsapi.mapping.toModel
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.ContactSearchRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.ContactSearchRequestV2
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.ContactSearchResultItem
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.ExistingRelationshipToPrisoner
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.ContactSearchRepository
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.ContactSearchRepositoryV2
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.ContactWithAddressRepository
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.PrisonerContactSummaryRepository
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.ContactSearchType.CONTACT_ID_ONLY
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.ContactSearchType.DATE_OF_BIRTH_AND_NAMES_MATCH
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.ContactSearchType.DATE_OF_BIRTH_AND_NAMES_MATCH_AND_HISTORY
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.ContactSearchType.DATE_OF_BIRTH_AND_NAMES_SOUND_LIKE
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.ContactSearchType.DATE_OF_BIRTH_AND_NAMES_SOUND_LIKE_AND_HISTORY
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.ContactSearchType.DATE_OF_BIRTH_ONLY
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.ContactSearchType.NAMES_MATCH
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.ContactSearchType.NAMES_MATCH_AND_HISTORY
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.ContactSearchType.NAMES_SOUND_LIKE
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.ContactSearchType.NAMES_SOUND_LIKE_AND_HISTORY

@Service
@Transactional(readOnly = true)
class ContactSearchService(
  private val contactSearchRepository: ContactSearchRepository,
  private val prisonerContactSummaryRepository: PrisonerContactSummaryRepository,
  private val contactSearchRepositoryV2: ContactSearchRepositoryV2,
  private val contactWithAddressRepository: ContactWithAddressRepository,
) {
  /**
   * The original V1 search for contacts
   */
  fun searchContacts(pageable: Pageable, request: ContactSearchRequest): Page<ContactSearchResultItem> {
    val checkForExistingRelationships = request.includeAnyExistingRelationshipsToPrisoner != null
    val matchingContactsPage = contactSearchRepository.searchContacts(request, pageable)
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

  /**
   * The V2 contacts search which attempts to use specific JPQL queries to suit the criteria provided.
   */
  fun searchContactsV2(request: ContactSearchRequestV2, pageable: Pageable): Page<ContactSearchResultItem> {
    validateRequest(request)
    val pageOfContactIds = getPageOfContactIds(request, pageable)

    logger.info("PageOfContacts is (page) ${pageOfContactIds.content.size} and (totalElements) ${pageOfContactIds.totalElements}")

    return if (!pageOfContactIds.isEmpty) {
      enrichOnePage(pageOfContactIds, request, pageable)
    } else {
      Page.empty()
    }
  }

  /**
   * Ths V2 function determines the best search method to find a requested page of contactIds, using
   * the search criteria provided to select the most appropriate repository method.
   */
  private fun getPageOfContactIds(request: ContactSearchRequestV2, pageable: Pageable): Page<Long> {
    val searchType = determineSearchType(request)

    val pageOfContactIds = when (searchType) {
      CONTACT_ID_ONLY -> {
        logger.info("CONTACT_ID_ONLY search for ${request.contactId}")
        contactSearchRepositoryV2.findAllByContactIdEquals(request.contactId!!, pageable)
      }

      DATE_OF_BIRTH_ONLY -> {
        logger.info("DATE_OF_BIRTH_ONLY search for ${request.dateOfBirth}")
        contactSearchRepositoryV2.findAllByDateOfBirthEquals(request.dateOfBirth!!, pageable)
      }

      DATE_OF_BIRTH_AND_NAMES_MATCH -> {
        logger.info("DATE_OF_BIRTH_AND_NAMES_MATCH search for ${request.dateOfBirth} last ${request.lastName}, first ${request.firstName},middle ${request.middleNames}")
        contactSearchRepositoryV2.findAllByDateOfBirthAndNamesMatch(
          request.dateOfBirth!!,
          request.firstName,
          request.middleNames,
          request.lastName,
          pageable,
        )
      }

      DATE_OF_BIRTH_AND_NAMES_SOUND_LIKE -> {
        logger.info("DATE_OF_BIRTH_AND_NAMES_SOUND_LIKE search for ${request.dateOfBirth} last ${request.lastName}, first ${request.firstName},middle ${request.middleNames}")
        contactSearchRepositoryV2.findAllByDateOfBirthAndNamesSoundLike(
          request.dateOfBirth!!,
          request.firstName,
          request.middleNames,
          request.lastName,
          pageable,
        )
      }

      NAMES_MATCH -> {
        logger.info("NAMES_MATCH search for last ${request.lastName}, first ${request.firstName},middle ${request.middleNames}")
        contactSearchRepositoryV2.findAllByNamesMatch(
          request.firstName,
          request.middleNames,
          request.lastName,
          pageable,
        )
      }

      NAMES_SOUND_LIKE -> {
        logger.info("NAMES_SOUND_LIKE search for last ${request.lastName}, first ${request.firstName},middle ${request.middleNames}")
        contactSearchRepositoryV2.findAllByNamesSoundLike(
          request.firstName,
          request.middleNames,
          request.lastName,
          pageable,
        )
      }

      DATE_OF_BIRTH_AND_NAMES_MATCH_AND_HISTORY -> {
        logger.info("DATE_OF_BIRTH_AND_NAMES_MATCH_AND_HISTORY search for dob ${request.dateOfBirth}, last ${request.lastName}, first ${request.firstName},middle ${request.middleNames}")
        contactSearchRepositoryV2.findAllByDateOfBirthAndNamesMatchAndHistory(
          request.dateOfBirth!!,
          request.firstName,
          request.middleNames,
          request.lastName,
          pageable,
        )
      }

      DATE_OF_BIRTH_AND_NAMES_SOUND_LIKE_AND_HISTORY -> {
        logger.info("DATE_OF_BIRTH_AND_NAMES_SOUND_LIKE_AND_HISTORY search for dob ${request.dateOfBirth}, last ${request.lastName}, first ${request.firstName},middle ${request.middleNames}")
        contactSearchRepositoryV2.findAllByDateOfBirthAndNamesSoundLikeAndHistory(
          request.dateOfBirth!!,
          request.firstName,
          request.middleNames,
          request.lastName,
          pageable,
        )
      }

      NAMES_MATCH_AND_HISTORY -> {
        logger.info("NAMES_MATCH_AND_HISTORY search for last ${request.lastName}, first ${request.firstName},middle ${request.middleNames}")
        contactSearchRepositoryV2.findAllByNamesMatchAndHistory(
          request.firstName,
          request.middleNames,
          request.lastName,
          pageable,
        )
      }

      NAMES_SOUND_LIKE_AND_HISTORY -> {
        logger.info("NAMES_SOUND_LIKE_AND_HISTORY search for last ${request.lastName}, first ${request.firstName},middle ${request.middleNames}")
        contactSearchRepositoryV2.findAllByNamesSoundLikeAndHistory(
          request.firstName,
          request.middleNames,
          request.lastName,
          pageable,
        )
      }
    }

    return pageOfContactIds
  }

  /**
   * This V2 function accepts a page of contact ids pre-sorted and enriches them with address details from
   * the view v_contacts_with_primary_address for those specific contact ids on the page. The sort order of
   * lastName, firstName is maintained for all specific queries.
   */
  private fun enrichOnePage(pageOfContactIds: Page<Long>, request: ContactSearchRequestV2, pageable: Pageable): Page<ContactSearchResultItem> {
    val contactIds = pageOfContactIds.content.map { it }

    // Retrieve the contact details and primary addresses for this page of contact IDs only
    val contactsWithAddresses = contactWithAddressRepository.findAllWhereContactIdIn(contactIds, pageable)

    // Add the relationships for a prisoner, if specified in the request
    val checkForRelationships = request.includePrisonerRelationships != null
    val contactRelationships: Map<Long, List<ExistingRelationshipToPrisoner>> = if (checkForRelationships) {
      prisonerContactSummaryRepository
        .findByPrisonerNumberAndContactIdIn(request.includePrisonerRelationships!!, contactIds)
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

    // Return the page of contact search results, including relationships if requested
    return contactsWithAddresses.map {
      val existingRelationships: List<ExistingRelationshipToPrisoner>? = if (checkForRelationships) {
        contactRelationships[it.contactId] ?: emptyList()
      } else {
        null
      }
      it.toModel(existingRelationships)
    }
  }

  /**
   * This V2 function validates a V2 search request and throws an IllegalArgumentException if invalid, including
   * a descriptive message of the problem
   */
  private fun validateRequest(request: ContactSearchRequestV2) {
    if (request.lastNameHistorical == true || request.lastNameSoundex == true) {
      require(request.lastName != null) { "Last name must be provided for historical or sounds-like searches" }
    }

    if (request.firstNameSoundex == true) {
      require(request.firstName != null) { "First name must be provided for sounds-like searches" }
    }

    if (request.middleNamesSoundex == true) {
      require(request.middleNames != null) { "Middle name must be provided for sounds-like searches" }
    }

    if (request.lastName != null) {
      require(request.lastName.length >= 2) { "Last name must be 2 or more characters" }
    }

    require(request.contactId != null || request.lastName != null || request.dateOfBirth != null || request.firstName != null || request.middleNames != null) {
      "Either contact ID, date of birth or a full or partial name must be provided for contact searches"
    }
  }

  /**
   * This V2 function tries to determine then most appropriate repository search method based
   * on the values provided in the request.
   */
  private fun determineSearchType(request: ContactSearchRequestV2): ContactSearchType {
    val contactIdPresent = request.contactId != null
    val dateOfBirthPresent = request.dateOfBirth != null
    val namesEntered = !isNullOrEmpty(request.firstName) || !isNullOrEmpty(request.lastName) || !isNullOrEmpty(request.middleNames)
    val soundsLike = request.lastNameSoundex == true || request.firstNameSoundex == true || request.middleNamesSoundex == true
    val dateOfBirthOnly = request.dateOfBirth != null && !namesEntered && !contactIdPresent
    val historyLastName = request.lastName != null && request.lastNameHistorical == true

    return when {
      contactIdPresent -> CONTACT_ID_ONLY
      dateOfBirthOnly -> DATE_OF_BIRTH_ONLY
      dateOfBirthPresent -> {
        if (soundsLike) {
          DATE_OF_BIRTH_AND_NAMES_SOUND_LIKE
        } else if (historyLastName) {
          DATE_OF_BIRTH_AND_NAMES_MATCH_AND_HISTORY
        } else {
          DATE_OF_BIRTH_AND_NAMES_MATCH
        }
      }
      soundsLike && historyLastName && namesEntered -> NAMES_SOUND_LIKE_AND_HISTORY
      soundsLike && namesEntered -> NAMES_SOUND_LIKE
      historyLastName && !soundsLike -> NAMES_MATCH_AND_HISTORY
      else -> NAMES_MATCH
    }
  }

  companion object {
    private val logger = LoggerFactory.getLogger(this::class.java)
  }
}

/**
 * A V2 enum of the available JPA repository search types
 */
enum class ContactSearchType {
  CONTACT_ID_ONLY,
  DATE_OF_BIRTH_ONLY,
  DATE_OF_BIRTH_AND_NAMES_MATCH,
  DATE_OF_BIRTH_AND_NAMES_SOUND_LIKE,
  DATE_OF_BIRTH_AND_NAMES_MATCH_AND_HISTORY,
  DATE_OF_BIRTH_AND_NAMES_SOUND_LIKE_AND_HISTORY,
  NAMES_MATCH,
  NAMES_SOUND_LIKE,
  NAMES_MATCH_AND_HISTORY,
  NAMES_SOUND_LIKE_AND_HISTORY,
}
