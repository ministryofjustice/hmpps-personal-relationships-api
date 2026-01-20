package uk.gov.justice.digital.hmpps.personalrelationships.service

import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.personalrelationships.client.manage.users.UserDetails
import uk.gov.justice.digital.hmpps.personalrelationships.client.prisonersearch.Prisoner
import uk.gov.justice.digital.hmpps.personalrelationships.entity.PrisonerContactSummaryEntity
import uk.gov.justice.digital.hmpps.personalrelationships.helpers.prisoner
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.PrisonerContactRelationshipDetails
import uk.gov.justice.digital.hmpps.personalrelationships.repository.PrisonerContactSummaryRepository
import java.time.LocalDate
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class PrisonerContactRelationshipServiceTest {

  @Mock
  private lateinit var prisonerContactSummaryRepository: PrisonerContactSummaryRepository

  @Mock
  private lateinit var manageUsersService: ManageUsersService

  @InjectMocks
  private lateinit var prisonerContactRelationshipService: PrisonerContactRelationshipService

  private lateinit var prisoner: Prisoner

  private val prisonerNumber = "A1111AA"

  @BeforeEach
  fun before() {
    prisoner = prisoner(prisonerNumber, prisonId = "MDI")
  }

  @Test
  fun `should return when prisoner contact relationship exists`() {
    val user = UserDetails("A_USER", "Foo User")
    whenever(manageUsersService.getUserByUsername("A_USER")).thenReturn(user)
    val prisonerContactId = 1L
    val expectedPrisonerContactRelationship = PrisonerContactRelationshipDetails(
      prisonerContactId = prisonerContactId,
      contactId = 2,
      prisonerNumber = "A1234BC",
      relationshipTypeCode = "S",
      relationshipTypeDescription = "Social",
      relationshipToPrisonerCode = "FRIEND",
      relationshipToPrisonerDescription = "Friend",
      isEmergencyContact = false,
      isNextOfKin = false,
      isApprovedVisitor = true,
      isRelationshipActive = true,
      comments = "No comments",
      approvedBy = "Foo User",
    )

    val prisonerContactSummaryEntity = makePrisonerContact(
      prisonerContactId = 1L,
      contactId = 2L,
      dateOfBirth = LocalDate.of(2000, 11, 21),
      firstName = "Jack",
      lastName = "Doe",
    )

    whenever(prisonerContactSummaryRepository.findById(prisonerContactId)).thenReturn(Optional.of(prisonerContactSummaryEntity))

    val actualPrisonerContactRelationship = prisonerContactRelationshipService.getById(prisonerContactId)

    assertThat(actualPrisonerContactRelationship).isEqualTo(expectedPrisonerContactRelationship)
    verify(prisonerContactSummaryRepository).findById(prisonerContactId)
  }

  @Test
  fun `should throw EntityNotFoundException when prisoner contact relationship does not exist`() {
    val prisonerContactId = 1L
    whenever(prisonerContactSummaryRepository.findById(prisonerContactId)).thenReturn(Optional.empty())

    val exception = assertThrows<EntityNotFoundException> {
      prisonerContactRelationshipService.getById(prisonerContactId)
    }

    assertThat(exception.message).isEqualTo("prisoner contact relationship with id $prisonerContactId not found")
    verify(prisonerContactSummaryRepository).findById(prisonerContactId)
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
    title = "MR",
    titleDescription = "Mr",
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
    staffFlag = false,
    approvedBy = "A_USER",
  )
}
