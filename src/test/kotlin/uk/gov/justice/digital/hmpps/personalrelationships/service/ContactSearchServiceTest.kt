package uk.gov.justice.digital.hmpps.personalrelationships.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import uk.gov.justice.digital.hmpps.personalrelationships.entity.ContactWithAddressEntity
import uk.gov.justice.digital.hmpps.personalrelationships.entity.PrisonerContactSummaryEntity
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.ContactSearchRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.ContactSearchRequestV2
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.UserSearchType
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.ContactSearchResultItem
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.ExistingRelationshipToPrisoner
import uk.gov.justice.digital.hmpps.personalrelationships.repository.ContactSearchRepository
import uk.gov.justice.digital.hmpps.personalrelationships.repository.ContactSearchRepositoryV2
import uk.gov.justice.digital.hmpps.personalrelationships.repository.ContactWithAddressRepository
import uk.gov.justice.digital.hmpps.personalrelationships.repository.PrisonerContactSummaryRepository
import java.time.LocalDate
import java.time.LocalDateTime

class ContactSearchServiceTest {

  private val contactSearchRepository: ContactSearchRepository = mock()
  private val prisonerContactSummaryRepository: PrisonerContactSummaryRepository = mock()
  private val contactSearchRepositoryV2: ContactSearchRepositoryV2 = mock()
  private val contactWithAddressRepository: ContactWithAddressRepository = mock()

  private val service = ContactSearchService(
    contactSearchRepository,
    prisonerContactSummaryRepository,
    contactSearchRepositoryV2,
    contactWithAddressRepository,
  )

  @Nested
  inner class SearchContact {

    @Test
    fun `test searchContacts with lastName , firstName , middleName and date of birth`() {
      // Given
      val pageable = PageRequest.of(0, 10)
      val contactWithAddressEntity = entity(1L)

      val results = listOf(contactWithAddressEntity)

      val pageContacts = PageImpl(results, pageable, results.size.toLong())

      // When
      val request = ContactSearchRequest("last", "first", "middle", "", LocalDate.of(1980, 1, 1), false, null)
      whenever(contactSearchRepository.searchContacts(request, pageable)).thenReturn(pageContacts)

      // Act
      val result: Page<ContactSearchResultItem> = service.searchContacts(pageable, request)

      // Then
      assertNotNull(result)
      assertThat(result.totalElements).isEqualTo(1)
      assertThat(result.content[0].lastName).isEqualTo("last")
      assertThat(result.content[0].firstName).isEqualTo("first")
      verify(prisonerContactSummaryRepository, never()).findByPrisonerNumberAndContactIdIn(any(), any())
    }

    @Test
    fun `should add existing relationships if requested`() {
      // Given
      val pageable = PageRequest.of(0, 10)
      val contactOne = entity(1L)
      val contactTwo = entity(2L)

      val results = listOf(contactOne, contactTwo)

      val pageContacts = PageImpl(results, pageable, results.size.toLong())

      // When
      val request =
        ContactSearchRequest("last", "first", "middle", "123456", LocalDate.of(1980, 1, 1), false, "A1234BC")
      whenever(contactSearchRepository.searchContacts(request, pageable)).thenReturn(pageContacts)
      whenever(
        prisonerContactSummaryRepository.findByPrisonerNumberAndContactIdIn(
          "A1234BC",
          listOf(1L, 2L),
        ),
      ).thenReturn(
        listOf(
          PrisonerContactSummaryEntity(
            prisonerContactId = 99L,
            contactId = 1L,
            title = "MR",
            titleDescription = "Mr",
            firstName = "first",
            middleNames = "Any",
            lastName = "last",
            dateOfBirth = null,
            deceasedDate = null,
            contactAddressId = 3L,
            flat = "2B",
            property = "123",
            street = "Baker Street",
            area = "Westminster",
            cityCode = "SHEF",
            cityDescription = "Sheffield",
            countyCode = "SYORKS",
            countyDescription = "South Yorkshire",
            postCode = "NW1 6XE",
            countryCode = "UK",
            countryDescription = "United Kingdom",
            noFixedAddress = false,
            primaryAddress = false,
            mailFlag = false,
            contactPhoneId = 4L,
            phoneType = "Mobile",
            phoneTypeDescription = "Mobile Phone",
            phoneNumber = "07123456789",
            extNumber = "0123",
            contactEmailId = 5L,
            emailAddress = "john.doe@example.com",
            prisonerNumber = "A1234BC",
            relationshipToPrisoner = "FRI",
            relationshipToPrisonerDescription = "Friend",
            active = true,
            approvedVisitor = true,
            nextOfKin = false,
            emergencyContact = false,
            currentTerm = true,
            comments = "No comments",
            relationshipType = "S",
            relationshipTypeDescription = "Social",
            staffFlag = false,
            approvedBy = "A_USER",
          ),
        ),
      )

      // Act
      val result: Page<ContactSearchResultItem> = service.searchContacts(pageable, request)

      // Then
      assertNotNull(result)
      assertThat(result.totalElements).isEqualTo(2)
      assertThat(result.content.find { it.id == 1L }!!.existingRelationships).isEqualTo(
        listOf(
          ExistingRelationshipToPrisoner(
            prisonerContactId = 99L,
            relationshipTypeCode = "S",
            relationshipTypeDescription = "Social",
            relationshipToPrisonerCode = "FRI",
            relationshipToPrisonerDescription = "Friend",
            isRelationshipActive = true,
          ),
        ),
      )
      assertThat(result.content.find { it.id == 2L }!!.existingRelationships).isEqualTo(emptyList<ExistingRelationshipToPrisoner>())

      verify(prisonerContactSummaryRepository).findByPrisonerNumberAndContactIdIn("A1234BC", listOf(1L, 2L))
    }

    private fun entity(contactId: Long) = ContactWithAddressEntity(
      contactId = contactId,
      title = "Mr",
      lastName = "last",
      middleNames = "middle",
      firstName = "first",
      dateOfBirth = LocalDate.of(1980, 2, 1),
      deceasedDate = null,
      contactAddressId = 1L,
      primaryAddress = true,
      verified = false,
      addressType = "HOME",
      flat = "Mr",
      property = "last",
      street = "middle",
      area = "first",
      cityCode = "",
      countyCode = "null",
      postCode = "user",
      countryCode = "user",
      createdBy = "TEST",
      createdTime = LocalDateTime.now(),
    )
  }

  @Nested
  inner class SearchContactV2 {
    val request = ContactSearchRequestV2(
      lastName = "last",
      firstName = "first",
      middleNames = "middle",
      dateOfBirth = null,
      searchType = UserSearchType.PARTIAL,
      previousNames = false,
      contactId = null,
      includePrisonerRelationships = null,
    )

    @Test
    fun `Should default to names match (but won't get here with an invalid request`() {
      assertThat(service.determineSearchType(request.copy(lastName = null, firstName = null, middleNames = null))).isEqualTo(ContactSearchType.NAMES_MATCH)
    }

    @Test
    fun `Should do a names match where only first name is supplied`() {
      assertThat(service.determineSearchType(request.copy(lastName = null, middleNames = null))).isEqualTo(ContactSearchType.NAMES_MATCH)
    }

    @Test
    fun `Should do a names match where only middle name is supplied`() {
      assertThat(service.determineSearchType(request.copy(lastName = null, firstName = null))).isEqualTo(ContactSearchType.NAMES_MATCH)
    }

    @Test
    fun `Should choose search by names match`() {
      assertThat(service.determineSearchType(request)).isEqualTo(ContactSearchType.NAMES_MATCH)
    }

    @Test
    fun `Should choose search by names exact`() {
      assertThat(service.determineSearchType(request.copy(searchType = UserSearchType.EXACT))).isEqualTo(ContactSearchType.NAMES_EXACT)
    }

    @Test
    fun `Should choose search by names exact and history`() {
      assertThat(service.determineSearchType(request.copy(searchType = UserSearchType.EXACT, previousNames = true))).isEqualTo(ContactSearchType.NAMES_EXACT_AND_HISTORY)
    }

    @Test
    fun `Should choose search by contact ID`() {
      assertThat(service.determineSearchType(request.copy(contactId = 111L))).isEqualTo(ContactSearchType.CONTACT_ID_ONLY)
    }

    @Test
    fun `Should still choose search by contact ID with other parameters set`() {
      assertThat(service.determineSearchType(request.copy(contactId = 111L, searchType = UserSearchType.SOUNDS_LIKE, previousNames = true))).isEqualTo(ContactSearchType.CONTACT_ID_ONLY)
    }

    @Test
    fun `Should choose search by date of birth only`() {
      val request2 = request.copy(dateOfBirth = LocalDate.of(1980, 1, 1), lastName = null, firstName = null, middleNames = null)
      assertThat(service.determineSearchType(request2)).isEqualTo(ContactSearchType.DATE_OF_BIRTH_ONLY)
    }

    @Test
    fun `Should choose search by date of birth and names match`() {
      val request2 = request.copy(dateOfBirth = LocalDate.of(1980, 1, 1))
      assertThat(service.determineSearchType(request2)).isEqualTo(ContactSearchType.DATE_OF_BIRTH_AND_NAMES_MATCH)
    }

    @Test
    fun `Should choose search by names sound like`() {
      assertThat(service.determineSearchType(request.copy(searchType = UserSearchType.SOUNDS_LIKE))).isEqualTo(ContactSearchType.NAMES_SOUND_LIKE)
    }

    @Test
    fun `Should choose search by date of birth and names sound like`() {
      val request2 = request.copy(dateOfBirth = LocalDate.of(1980, 1, 1), searchType = UserSearchType.SOUNDS_LIKE)
      assertThat(service.determineSearchType(request2)).isEqualTo(ContactSearchType.DATE_OF_BIRTH_AND_NAMES_SOUND_LIKE)
    }

    @Test
    fun `Should choose search by names sound like and history`() {
      assertThat(service.determineSearchType(request.copy(searchType = UserSearchType.SOUNDS_LIKE, previousNames = true))).isEqualTo(ContactSearchType.NAMES_SOUND_LIKE_AND_HISTORY)
    }

    @Test
    fun `Should choose search by names match and history`() {
      assertThat(service.determineSearchType(request.copy(previousNames = true))).isEqualTo(ContactSearchType.NAMES_MATCH_AND_HISTORY)
    }

    @Test
    fun `Should choose search by date of birth and names exact and history`() {
      val request2 = request.copy(dateOfBirth = LocalDate.of(1980, 1, 1), previousNames = true, searchType = UserSearchType.EXACT)
      assertThat(service.determineSearchType(request2)).isEqualTo(ContactSearchType.DATE_OF_BIRTH_AND_NAMES_EXACT_AND_HISTORY)
    }

    @Test
    fun `Should choose search by date of birth and names match and history`() {
      val request2 = request.copy(dateOfBirth = LocalDate.of(1980, 1, 1), previousNames = true)
      assertThat(service.determineSearchType(request2)).isEqualTo(ContactSearchType.DATE_OF_BIRTH_AND_NAMES_MATCH_AND_HISTORY)
    }

    @Test
    fun `Should choose search by date of birth and names sound like and history`() {
      val request2 = request.copy(dateOfBirth = LocalDate.of(1980, 1, 1), searchType = UserSearchType.SOUNDS_LIKE, previousNames = true)
      assertThat(service.determineSearchType(request2)).isEqualTo(ContactSearchType.DATE_OF_BIRTH_AND_NAMES_SOUND_LIKE_AND_HISTORY)
    }
  }

  @Nested
  inner class ContactSearchV2Utilities {
    @ParameterizedTest
    @CsvSource(
      "lastName, last_name",
      "firstName, first_name",
      "dateOfBirth, date_of_birth",
      "aVeryVeryVeryLongName, a_very_very_very_long_name",
      "contactId, contact_id",
    )
    fun `Should manipulate camel case sort order names to snake case column names for native queries`(camelName: String, snakeName: String) {
      assertThat(service.convertCamelToSnake(camelName)).isEqualTo(snakeName)
    }
  }

  @Nested
  inner class ContactSearchV2SortOrder {
    @Test
    fun `Should manipulate sort order to native SQL sort order with default direction`() {
      val oldSort = Sort.by("lastName", "firstName", "dateOfBirth")

      val newSort = service.manipulateSortToNative(oldSort)

      assertThat(newSort.isSorted).isTrue()
      assertThat(newSort.get().toList()).containsExactly(
        Sort.Order(Sort.Direction.ASC, "last_name"),
        Sort.Order(Sort.Direction.ASC, "first_name"),
        Sort.Order(Sort.Direction.ASC, "date_of_birth"),
      )
    }

    @Test
    fun `Should not try to sort an unsorted query`() {
      val oldSort = Sort.unsorted()
      val newSort = service.manipulateSortToNative(oldSort)
      assertThat(newSort.isUnsorted).isTrue()
    }

    @Test
    fun `Should retain the sort order including ASC or DESC supplied for columns`() {
      val oldSort = Sort.by(
        listOf(
          Sort.Order(Sort.Direction.ASC, "lastName"),
          Sort.Order(Sort.Direction.DESC, "firstName"),
          Sort.Order(Sort.Direction.ASC, "dateOfBirth"),
        ),
      )

      val newSort = service.manipulateSortToNative(oldSort)

      assertThat(newSort.isSorted).isTrue()
      assertThat(newSort.get().toList()).containsExactly(
        Sort.Order(Sort.Direction.ASC, "last_name"),
        Sort.Order(Sort.Direction.DESC, "first_name"),
        Sort.Order(Sort.Direction.ASC, "date_of_birth"),
      )
    }
  }
}
