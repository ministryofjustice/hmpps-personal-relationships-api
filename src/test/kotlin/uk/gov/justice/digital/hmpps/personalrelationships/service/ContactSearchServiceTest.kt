package uk.gov.justice.digital.hmpps.personalrelationships.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.ContactSearchRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.UserSearchType
import uk.gov.justice.digital.hmpps.personalrelationships.repository.ContactSearchRepositoryV2
import uk.gov.justice.digital.hmpps.personalrelationships.repository.ContactWithAddressRepository
import uk.gov.justice.digital.hmpps.personalrelationships.repository.PrisonerContactSummaryRepository
import java.time.LocalDate

class ContactSearchServiceTest {

  private val prisonerContactSummaryRepository: PrisonerContactSummaryRepository = mock()
  private val contactSearchRepositoryV2: ContactSearchRepositoryV2 = mock()
  private val contactWithAddressRepository: ContactWithAddressRepository = mock()

  private val service = ContactSearchService(
    prisonerContactSummaryRepository,
    contactSearchRepositoryV2,
    contactWithAddressRepository,
  )

  @Nested
  inner class SearchContact {
    val request = ContactSearchRequest(
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
    fun `NAMES_SOUND_LIKE_AND_HISTORY uses rowLimiter and converts sort to snake_case`() {
      val pageable = PageRequest.of(0, 10, Sort.by("lastName", "firstName", "dateOfBirth"))
      val request = request.copy(
        searchType = UserSearchType.SOUNDS_LIKE,
        previousNames = true,
      )

      val captorLimit = argumentCaptor<Int>()
      val captorPageable = argumentCaptor<Pageable>()

      whenever(contactSearchRepositoryV2.findAllByNamesSoundLikeAndHistory(any(), any(), any(), any(), any()))
        .thenReturn(PageImpl(emptyList<Long>(), pageable, 0))

      service.searchContacts(request, pageable)

      verify(contactSearchRepositoryV2).findAllByNamesSoundLikeAndHistory(
        eq(request.firstName?.trim()),
        eq(request.middleNames?.trim()),
        eq(request.lastName?.trim()),
        captorLimit.capture(),
        captorPageable.capture(),
      )

      assertThat(captorLimit.firstValue).isEqualTo(service.rowLimiter)

      val nativeSortOrders = captorPageable.firstValue.sort.get().toList()
      assertThat(nativeSortOrders).containsExactly(
        Sort.Order(Sort.Direction.ASC, "last_name"),
        Sort.Order(Sort.Direction.ASC, "first_name"),
        Sort.Order(Sort.Direction.ASC, "date_of_birth"),
      )
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
    fun `Should choose search by names match and history with only a partial first name`() {
      assertThat(service.determineSearchType(request.copy(lastName = null, middleNames = null, previousNames = true))).isEqualTo(ContactSearchType.NAMES_MATCH_AND_HISTORY)
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
  inner class ContactSearchUtilities {
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
  inner class ContactSearchSortOrder {
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
