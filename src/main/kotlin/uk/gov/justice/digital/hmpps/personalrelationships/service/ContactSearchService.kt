package uk.gov.justice.digital.hmpps.personalrelationships.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.personalrelationships.mapping.toModel
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.ContactSearchRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.UserSearchType
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.ContactSearchResultItem
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.ExistingRelationshipToPrisoner
import uk.gov.justice.digital.hmpps.personalrelationships.repository.ContactSearchRepository
import uk.gov.justice.digital.hmpps.personalrelationships.repository.ContactWithAddressRepository
import uk.gov.justice.digital.hmpps.personalrelationships.repository.PrisonerContactSummaryRepository
import uk.gov.justice.digital.hmpps.personalrelationships.service.ContactSearchType.CONTACT_ID_ONLY
import uk.gov.justice.digital.hmpps.personalrelationships.service.ContactSearchType.DATE_OF_BIRTH_AND_NAMES_EXACT
import uk.gov.justice.digital.hmpps.personalrelationships.service.ContactSearchType.DATE_OF_BIRTH_AND_NAMES_EXACT_AND_HISTORY
import uk.gov.justice.digital.hmpps.personalrelationships.service.ContactSearchType.DATE_OF_BIRTH_AND_NAMES_MATCH
import uk.gov.justice.digital.hmpps.personalrelationships.service.ContactSearchType.DATE_OF_BIRTH_AND_NAMES_MATCH_AND_HISTORY
import uk.gov.justice.digital.hmpps.personalrelationships.service.ContactSearchType.DATE_OF_BIRTH_AND_NAMES_SOUND_LIKE
import uk.gov.justice.digital.hmpps.personalrelationships.service.ContactSearchType.DATE_OF_BIRTH_AND_NAMES_SOUND_LIKE_AND_HISTORY
import uk.gov.justice.digital.hmpps.personalrelationships.service.ContactSearchType.DATE_OF_BIRTH_ONLY
import uk.gov.justice.digital.hmpps.personalrelationships.service.ContactSearchType.NAMES_EXACT
import uk.gov.justice.digital.hmpps.personalrelationships.service.ContactSearchType.NAMES_EXACT_AND_HISTORY
import uk.gov.justice.digital.hmpps.personalrelationships.service.ContactSearchType.NAMES_MATCH
import uk.gov.justice.digital.hmpps.personalrelationships.service.ContactSearchType.NAMES_MATCH_AND_HISTORY
import uk.gov.justice.digital.hmpps.personalrelationships.service.ContactSearchType.NAMES_SOUND_LIKE
import uk.gov.justice.digital.hmpps.personalrelationships.service.ContactSearchType.NAMES_SOUND_LIKE_AND_HISTORY

@Service
@Transactional(readOnly = true)
class ContactSearchService(
  private val prisonerContactSummaryRepository: PrisonerContactSummaryRepository,
  private val contactSearchRepository: ContactSearchRepository,
  private val contactWithAddressRepository: ContactWithAddressRepository,
  @Value("\${contact-search.slow-query.row-limit}") val rowLimiter: Int = 2000,
) {
  fun searchContacts(request: ContactSearchRequest, pageable: Pageable): Page<ContactSearchResultItem> {
    validateRequest(request)
    val pageOfContactIds = getPageOfContactIds(request, pageable)

    logger.info("PageOfContacts is (elementsInThisPage) ${pageOfContactIds.content.size} and (totalElements) ${pageOfContactIds.totalElements}")

    return if (!pageOfContactIds.isEmpty) {
      enrichOnePage(pageOfContactIds, request, pageable, pageOfContactIds.totalElements)
    } else {
      Page.empty()
    }
  }

  /**
   * Determine the most appropriate repository method, calls it and return at most one page of contactIds
   */
  private fun getPageOfContactIds(request: ContactSearchRequest, pageable: Pageable): Page<Long> {
    val searchType = determineSearchType(request)

    val pageOfContactIds = when (searchType) {
      CONTACT_ID_ONLY -> {
        logger.info("CONTACT_ID_ONLY search for ${request.contactId}")
        contactSearchRepository.findAllByContactIdEquals(request.contactId!!, pageable)
      }

      DATE_OF_BIRTH_ONLY -> {
        logger.info("DATE_OF_BIRTH_ONLY search for ${request.dateOfBirth}")
        contactSearchRepository.findAllByDateOfBirthEquals(request.dateOfBirth!!, pageable)
      }

      DATE_OF_BIRTH_AND_NAMES_EXACT -> {
        logger.info("DATE_OF_BIRTH_AND_NAMES_EXACT search for ${request.dateOfBirth} last ${request.lastName}, first ${request.firstName},middle ${request.middleNames}")
        contactSearchRepository.findAllByDateOfBirthAndNamesExact(
          request.dateOfBirth!!,
          request.firstName?.trim(),
          request.middleNames?.trim(),
          request.lastName?.trim(),
          pageable,
        )
      }

      DATE_OF_BIRTH_AND_NAMES_EXACT_AND_HISTORY -> {
        logger.info("DATE_OF_BIRTH_AND_NAMES_EXACT_AND_HISTORY search for ${request.dateOfBirth} last ${request.lastName}, first ${request.firstName},middle ${request.middleNames}")
        contactSearchRepository.findAllByDateOfBirthAndNamesExactAndHistory(
          request.dateOfBirth!!,
          request.firstName?.trim(),
          request.middleNames?.trim(),
          request.lastName?.trim(),
          pageable,
        )
      }

      DATE_OF_BIRTH_AND_NAMES_MATCH -> {
        logger.info("DATE_OF_BIRTH_AND_NAMES_MATCH search for ${request.dateOfBirth} last ${request.lastName}, first ${request.firstName},middle ${request.middleNames}")
        contactSearchRepository.findAllByDateOfBirthAndNamesMatch(
          request.dateOfBirth!!,
          request.firstName?.trim(),
          request.middleNames?.trim(),
          request.lastName?.trim(),
          pageable,
        )
      }

      DATE_OF_BIRTH_AND_NAMES_MATCH_AND_HISTORY -> {
        logger.info("DATE_OF_BIRTH_AND_NAMES_MATCH_AND_HISTORY search for dob ${request.dateOfBirth}, last ${request.lastName}, first ${request.firstName},middle ${request.middleNames}")
        contactSearchRepository.findAllByDateOfBirthAndNamesMatchAndHistory(
          request.dateOfBirth!!,
          request.firstName?.trim(),
          request.middleNames?.trim(),
          request.lastName?.trim(),
          pageable,
        )
      }

      DATE_OF_BIRTH_AND_NAMES_SOUND_LIKE -> {
        logger.info("DATE_OF_BIRTH_AND_NAMES_SOUND_LIKE search for ${request.dateOfBirth} last ${request.lastName}, first ${request.firstName},middle ${request.middleNames}")
        contactSearchRepository.findAllByDateOfBirthAndNamesSoundLike(
          request.dateOfBirth!!,
          request.firstName?.trim(),
          request.middleNames?.trim(),
          request.lastName?.trim(),
          pageable,
        )
      }

      DATE_OF_BIRTH_AND_NAMES_SOUND_LIKE_AND_HISTORY -> {
        logger.info("DATE_OF_BIRTH_AND_NAMES_SOUND_LIKE_AND_HISTORY search for dob ${request.dateOfBirth}, last ${request.lastName}, first ${request.firstName},middle ${request.middleNames}")
        contactSearchRepository.findAllByDateOfBirthAndNamesSoundLikeAndHistory(
          request.dateOfBirth!!,
          request.firstName?.trim(),
          request.middleNames?.trim(),
          request.lastName?.trim(),
          pageable,
        )
      }

      NAMES_EXACT -> {
        logger.info("NAMES_EXACT search for last ${request.lastName}, first ${request.firstName},middle ${request.middleNames}")
        contactSearchRepository.findAllByNamesExact(
          request.firstName?.trim(),
          request.middleNames?.trim(),
          request.lastName?.trim(),
          pageable,
        )
      }

      NAMES_EXACT_AND_HISTORY -> {
        logger.info("NAMES_EXACT_AND_HISTORY search for last ${request.lastName}, first ${request.firstName},middle ${request.middleNames}")
        contactSearchRepository.findAllByNamesExactAndHistory(
          request.firstName?.trim(),
          request.middleNames?.trim(),
          request.lastName?.trim(),
          pageable,
        )
      }

      NAMES_MATCH -> {
        logger.info("NAMES_MATCH search for last ${request.lastName}, first ${request.firstName},middle ${request.middleNames}")
        contactSearchRepository.findAllByNamesMatch(
          request.firstName?.trim(),
          request.middleNames?.trim(),
          request.lastName?.trim(),
          pageable,
        )
      }

      NAMES_MATCH_AND_HISTORY -> {
        logger.info("NAMES_MATCH_AND_HISTORY search for last ${request.lastName}, first ${request.firstName},middle ${request.middleNames}")
        // This search option involves a native query so sort parameters need to be manipulated to native column name format
        val newPageable = PageRequest.of(pageable.pageNumber, pageable.pageSize, manipulateSortToNative(pageable.sort))

        contactSearchRepository.findAllByNamesMatchAndHistory(
          request.firstName?.trim(),
          request.middleNames?.trim(),
          request.lastName?.trim(),
          rowLimiter,
          newPageable,
        )
      }

      NAMES_SOUND_LIKE -> {
        logger.info("NAMES_SOUND_LIKE search for last ${request.lastName}, first ${request.firstName},middle ${request.middleNames}")
        contactSearchRepository.findAllByNamesSoundLike(
          request.firstName?.trim(),
          request.middleNames?.trim(),
          request.lastName?.trim(),
          pageable,
        )
      }

      NAMES_SOUND_LIKE_AND_HISTORY -> {
        logger.info("NAMES_SOUND_LIKE_AND_HISTORY search for last ${request.lastName}, first ${request.firstName},middle ${request.middleNames}")
        // native query - adjust sort to native column names
        val nativePageable = PageRequest.of(pageable.pageNumber, pageable.pageSize, manipulateSortToNative(pageable.sort))

        contactSearchRepository.findAllByNamesSoundLikeAndHistory(
          request.firstName?.trim(),
          request.middleNames?.trim(),
          request.lastName?.trim(),
          rowLimiter,
          nativePageable,
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
  private fun enrichOnePage(
    pageOfContactIds: Page<Long>,
    request: ContactSearchRequest,
    pageable: Pageable,
    totalCount: Long,
  ): Page<ContactSearchResultItem> {
    val contactIds = pageOfContactIds.content.map { it }

    // Take the sort criteria from the original user query
    val sort = pageable.sort

    // Retrieve the contact details and primary addresses for this one page of contact IDs only - not paginated
    val contactsWithAddresses = contactWithAddressRepository.findAllWhereContactIdUnpaginated(contactIds, sort)

    val checkForRelationships = request.includePrisonerRelationships != null

    // Add the relationships for a prisoner, if requested
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
    val contactsModelWithAddresses = contactsWithAddresses.map {
      val existingRelationships: List<ExistingRelationshipToPrisoner>? = if (checkForRelationships) {
        contactRelationships[it.contactId] ?: emptyList()
      } else {
        null
      }
      it.toModel(existingRelationships)
    }

    // Use the original pageable and total count
    return PageImpl(contactsModelWithAddresses, pageable, totalCount)
  }

  private fun validateRequest(request: ContactSearchRequest) {
    val nameProvided = request.lastName != null || request.firstName != null || request.middleNames != null
    val isNamedSearchType = request.searchType in setOf(UserSearchType.SOUNDS_LIKE, UserSearchType.PARTIAL, UserSearchType.EXACT)

    if (isNamedSearchType && request.contactId == null && request.dateOfBirth == null) {
      val typeLabel = request.searchType.name.lowercase().replace("_", "-")
      require(nameProvided) { "A name must be provided for $typeLabel searches" }

      request.lastName?.let {
        require(it.length >= 2) { "Last name must be 2 or more characters" }
      }
    }

    require(request.contactId != null || request.dateOfBirth != null || nameProvided) {
      "Either contact ID, date of birth or a full or partial name must be provided for contact searches"
    }
  }

  fun manipulateSortToNative(originalSort: Sort = Sort.unsorted()): Sort = if (originalSort.isUnsorted) {
    originalSort
  } else {
    Sort.by(originalSort.map { Sort.Order(it.direction, convertCamelToSnake(it.property)) }.toList())
  }

  fun convertCamelToSnake(property: String): String {
    val regex = "(?<=.)([A-Z])".toRegex()
    return property.trim().replace(regex) { "_${it.value.lowercase()}" }.lowercase()
  }

  fun determineSearchType(request: ContactSearchRequest): ContactSearchType {
    val contactIdPresent = request.contactId != null
    val dobPresent = request.dateOfBirth != null
    val namesEntered = listOf(request.firstName, request.lastName, request.middleNames).any { it != null }
    val previousNames = namesEntered && request.previousNames == true

    return when {
      contactIdPresent -> CONTACT_ID_ONLY
      dobPresent && !namesEntered -> DATE_OF_BIRTH_ONLY
      namesEntered -> mapNameSearch(request.searchType, dobPresent, previousNames)
      else -> NAMES_MATCH
    }
  }

  private fun mapNameSearch(type: UserSearchType, withDob: Boolean, withHistory: Boolean): ContactSearchType = when (type to withDob to withHistory) {
    (UserSearchType.SOUNDS_LIKE to true to false) -> DATE_OF_BIRTH_AND_NAMES_SOUND_LIKE
    (UserSearchType.SOUNDS_LIKE to true to true) -> DATE_OF_BIRTH_AND_NAMES_SOUND_LIKE_AND_HISTORY
    (UserSearchType.EXACT to true to false) -> DATE_OF_BIRTH_AND_NAMES_EXACT
    (UserSearchType.EXACT to true to true) -> DATE_OF_BIRTH_AND_NAMES_EXACT_AND_HISTORY
    (UserSearchType.PARTIAL to true to false) -> DATE_OF_BIRTH_AND_NAMES_MATCH
    (UserSearchType.PARTIAL to true to true) -> DATE_OF_BIRTH_AND_NAMES_MATCH_AND_HISTORY
    (UserSearchType.SOUNDS_LIKE to false to false) -> NAMES_SOUND_LIKE
    (UserSearchType.SOUNDS_LIKE to false to true) -> NAMES_SOUND_LIKE_AND_HISTORY
    (UserSearchType.EXACT to false to false) -> NAMES_EXACT
    (UserSearchType.EXACT to false to true) -> NAMES_EXACT_AND_HISTORY
    (UserSearchType.PARTIAL to false to false) -> NAMES_MATCH
    (UserSearchType.PARTIAL to false to true) -> NAMES_MATCH_AND_HISTORY
    else -> NAMES_MATCH
  }

  companion object {
    private val logger = LoggerFactory.getLogger(this::class.java)
  }
}

enum class ContactSearchType {
  CONTACT_ID_ONLY,
  DATE_OF_BIRTH_ONLY,
  DATE_OF_BIRTH_AND_NAMES_MATCH,
  DATE_OF_BIRTH_AND_NAMES_MATCH_AND_HISTORY,
  DATE_OF_BIRTH_AND_NAMES_SOUND_LIKE,
  DATE_OF_BIRTH_AND_NAMES_SOUND_LIKE_AND_HISTORY,
  DATE_OF_BIRTH_AND_NAMES_EXACT,
  DATE_OF_BIRTH_AND_NAMES_EXACT_AND_HISTORY,
  NAMES_EXACT,
  NAMES_EXACT_AND_HISTORY,
  NAMES_MATCH,
  NAMES_MATCH_AND_HISTORY,
  NAMES_SOUND_LIKE,
  NAMES_SOUND_LIKE_AND_HISTORY,
}
