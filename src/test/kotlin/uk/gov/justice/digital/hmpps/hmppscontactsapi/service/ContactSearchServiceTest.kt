package uk.gov.justice.digital.hmpps.hmppscontactsapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.ContactWithAddressEntity
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.PrisonerContactSummaryEntity
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.ContactSearchRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.ContactSearchResultItem
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.ExistingRelationshipToPrisoner
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.ContactSearchRepository
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.ContactSearchRepositoryV2
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.ContactWithAddressRepository
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.PrisonerContactSummaryRepository
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
      val request = ContactSearchRequest("last", "first", "middle", "123456", LocalDate.of(1980, 1, 1), false, "A1234BC")
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
}
