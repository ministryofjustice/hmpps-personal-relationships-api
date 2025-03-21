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
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.PrisonerContactSummaryEntity
import uk.gov.justice.digital.hmpps.hmppscontactsapi.helpers.prisoner
import uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.helper.hasSize
import uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.helper.isEqualTo
import uk.gov.justice.digital.hmpps.hmppscontactsapi.mapping.toModel
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.internal.PrisonerContactSearchParams
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.PrisonerContactSearchRepository
import java.time.LocalDate

@ExtendWith(MockitoExtension::class)
class PrisonerContactServiceTest {

  @Mock
  private lateinit var prisonerContactSearchRepository: PrisonerContactSearchRepository

  @Mock
  private lateinit var prisonerService: PrisonerService

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
    val result = prisonerContactService.getAllContacts(request)

    result.content hasSize 2
    assertThat(result).containsAll(listOf(c1.toModel(), c2.toModel()))

    verify(prisonerContactSearchRepository).searchPrisonerContacts(request)
  }

  @Test
  fun `should throw exception`() {
    whenever(prisonerService.getPrisoner(prisonerNumber)).thenReturn(null)
    val exception = assertThrows<EntityNotFoundException> {
      prisonerContactService.getAllContacts(PrisonerContactSearchParams(prisonerNumber, null, null, null, null, null, Pageable.unpaged()))
    }
    exception.message isEqualTo "Prisoner number $prisonerNumber - not found"
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
