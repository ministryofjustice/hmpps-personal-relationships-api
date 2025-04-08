package uk.gov.justice.digital.hmpps.hmppscontactsapi.service

import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import uk.gov.justice.digital.hmpps.hmppscontactsapi.client.prisonersearch.Prisoner
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.PrisonerContactRelationshipCountEntity
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.PrisonerContactRestrictionCountsEntity
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.PrisonerContactSummaryEntity
import uk.gov.justice.digital.hmpps.hmppscontactsapi.helpers.prisoner
import uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.helper.hasSize
import uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.helper.isEqualTo
import uk.gov.justice.digital.hmpps.hmppscontactsapi.mapping.toModel
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.internal.PrisonerContactSearchParams
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.PrisonerContactRelationshipCount
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.RestrictionTypeDetails
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.RestrictionsSummary
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.PrisonerContactRelationshipCountRepository
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.PrisonerContactRestrictionCountsRepository
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.PrisonerContactSearchRepository
import java.time.LocalDate
import java.util.*

@ExtendWith(MockitoExtension::class)
class PrisonerContactServiceTest {

  @Mock
  private lateinit var prisonerContactSearchRepository: PrisonerContactSearchRepository

  @Mock
  private lateinit var prisonerContactRestrictionCountsRepository: PrisonerContactRestrictionCountsRepository

  @Mock
  private lateinit var prisonerService: PrisonerService

  @Mock
  private lateinit var prisonerContactRelationshipCountRepository: PrisonerContactRelationshipCountRepository

  @InjectMocks
  private lateinit var prisonerContactService: PrisonerContactService

  private lateinit var prisoner: Prisoner

  private val prisonerNumber = "A1111AA"

  @BeforeEach
  fun before() {
    prisoner = prisoner(prisonerNumber, prisonId = "MDI")
  }

  private val pageable = Pageable.ofSize(10)

  @Test
  fun `should fetch all contacts for a prisoner`() {
    val dateOfBirth = LocalDate.of(1980, 5, 10)
    val c1 = makePrisonerContact(
      prisonerContactId = 1L,
      contactId = 2L,
      dateOfBirth,
      firstName = "John",
      lastName = "Doe",
    )
    val c2 = makePrisonerContact(
      prisonerContactId = 2L,
      contactId = 2L,
      dateOfBirth,
      firstName = "David",
      lastName = "Doe",
    )
    val contacts = listOf(c1, c2)
    val page = PageImpl(contacts, pageable, contacts.size.toLong())
    val request = PrisonerContactSearchParams(prisonerNumber, null, null, null, null, null, Pageable.unpaged())

    whenever(prisonerService.getPrisoner(prisonerNumber)).thenReturn(prisoner)
    whenever(prisonerContactSearchRepository.searchPrisonerContacts(any())).thenReturn(page)
    whenever(prisonerContactRestrictionCountsRepository.findAllByPrisonerContactIdIn(any())).thenReturn(
      listOf(
        PrisonerContactRestrictionCountsEntity(1L, "BAN", "Banned", false, 1),
        PrisonerContactRestrictionCountsEntity(1L, "NONCON", "Non-contact visit", true, 3),
        PrisonerContactRestrictionCountsEntity(1L, "CCTV", "CCTV", false, 1),
        PrisonerContactRestrictionCountsEntity(2L, "BAN", "Banned", true, 2),
        PrisonerContactRestrictionCountsEntity(2L, "CCTV", "CCTV", false, 1),
        PrisonerContactRestrictionCountsEntity(2L, "NONCON", "Non-contact visit", true, 3),
      ),
    )
    val result = prisonerContactService.getAllContacts(request)

    result.content hasSize 2
    assertThat(result.content).containsAll(
      listOf(
        c1.toModel(
          RestrictionsSummary(
            setOf(
              RestrictionTypeDetails("BAN", "Banned"),
              RestrictionTypeDetails("CCTV", "CCTV"),
            ),
            2,
            3,
          ),
        ),
        c2.toModel(
          RestrictionsSummary(
            setOf((RestrictionTypeDetails("CCTV", "CCTV"))),
            1,
            5,
          ),
        ),
      ),
    )

    verify(prisonerContactSearchRepository).searchPrisonerContacts(request)
    verify(prisonerContactRestrictionCountsRepository).findAllByPrisonerContactIdIn(setOf(1L, 2L))
  }

  @Test
  fun `should fetch all contacts for a prisoner handle no restrictions`() {
    val dateOfBirth = LocalDate.of(1980, 5, 10)
    val c1 = makePrisonerContact(
      prisonerContactId = 1L,
      contactId = 2L,
      dateOfBirth,
      firstName = "John",
      lastName = "Doe",
    )
    val c2 = makePrisonerContact(
      prisonerContactId = 2L,
      contactId = 2L,
      dateOfBirth,
      firstName = "David",
      lastName = "Doe",
    )
    val contacts = listOf(c1, c2)
    val page = PageImpl(contacts, pageable, contacts.size.toLong())
    val request = PrisonerContactSearchParams(prisonerNumber, null, null, null, null, null, Pageable.unpaged())

    whenever(prisonerService.getPrisoner(prisonerNumber)).thenReturn(prisoner)
    whenever(prisonerContactSearchRepository.searchPrisonerContacts(any())).thenReturn(page)
    whenever(prisonerContactRestrictionCountsRepository.findAllByPrisonerContactIdIn(any())).thenReturn(
      listOf(
        PrisonerContactRestrictionCountsEntity(1L, "BAN", "Banned", false, 1),
        PrisonerContactRestrictionCountsEntity(1L, "NONCON", "Non-contact visit", true, 3),
        PrisonerContactRestrictionCountsEntity(1L, "CCTV", "CCTV", false, 1),
      ),
    )
    val result = prisonerContactService.getAllContacts(request)

    result.content hasSize 2
    assertThat(result.content).containsAll(
      listOf(
        c1.toModel(
          RestrictionsSummary(
            setOf(
              RestrictionTypeDetails("BAN", "Banned"),
              RestrictionTypeDetails("CCTV", "CCTV"),
            ),
            2,
            3,
          ),
        ),
        c2.toModel(RestrictionsSummary.NO_RESTRICTIONS),
      ),
    )

    verify(prisonerContactSearchRepository).searchPrisonerContacts(request)
    verify(prisonerContactRestrictionCountsRepository).findAllByPrisonerContactIdIn(setOf(1L, 2L))
  }

  @Test
  fun `should throw exception`() {
    whenever(prisonerService.getPrisoner(prisonerNumber)).thenReturn(null)
    val exception = assertThrows<EntityNotFoundException> {
      prisonerContactService.getAllContacts(
        PrisonerContactSearchParams(
          prisonerNumber,
          null,
          null,
          null,
          null,
          null,
          Pageable.unpaged(),
        ),
      )
    }
    exception.message isEqualTo "Prisoner number $prisonerNumber - not found"
  }

  @Test
  fun `should return count if found`() {
    val prisonerNumber = "A1234BC"
    whenever(prisonerContactRelationshipCountRepository.findById(prisonerNumber))
      .thenReturn(Optional.of(PrisonerContactRelationshipCountEntity(prisonerNumber, 99, 1000)))
    val count = prisonerContactService.countContactRelationships(prisonerNumber)
    assertThat(count).isEqualTo(PrisonerContactRelationshipCount(99, 1000))
  }

  @Test
  fun `should return 0 if count is not found`() {
    val prisonerNumber = "A1234BC"
    whenever(prisonerContactRelationshipCountRepository.findById(prisonerNumber)).thenReturn(Optional.empty())
    val count = prisonerContactService.countContactRelationships(prisonerNumber)
    assertThat(count).isEqualTo(PrisonerContactRelationshipCount(0, 0))
  }

  private fun makePrisonerContact(
    prisonerContactId: Long,
    contactId: Long,
    dateOfBirth: LocalDate?,
    firstName: String,
    lastName: String,
    active: Boolean = true,
  ): PrisonerContactSummaryEntity = PrisonerContactSummaryEntity(
    prisonerContactId,
    contactId = contactId,
    title = "Mr.",
    firstName = firstName,
    middleNames = "Any",
    lastName = lastName,
    dateOfBirth = dateOfBirth,
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
    relationshipToPrisoner = "FRIEND",
    relationshipToPrisonerDescription = "Friend",
    active = active,
    approvedVisitor = true,
    nextOfKin = false,
    emergencyContact = false,
    currentTerm = true,
    comments = "No comments",
    relationshipType = "S",
    relationshipTypeDescription = "Social",
  )
}
