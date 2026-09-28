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
import uk.gov.justice.digital.hmpps.personalrelationships.helpers.makePrisonerContact
import uk.gov.justice.digital.hmpps.personalrelationships.helpers.prisoner
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.PrisonerContactRelationshipDetails
import java.time.LocalDate

@ExtendWith(MockitoExtension::class)
class PrisonerContactRelationshipServiceTest {

  @Mock
  private lateinit var manageUsersService: ManageUsersService

  @Mock
  private lateinit var transactionalPrisonerContactRelationshipService: TransactionalPrisonerContactRelationshipService

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

    whenever(transactionalPrisonerContactRelationshipService.getById(prisonerContactId)).thenReturn(prisonerContactSummaryEntity)

    val actualPrisonerContactRelationship = prisonerContactRelationshipService.getById(prisonerContactId)

    assertThat(actualPrisonerContactRelationship).isEqualTo(expectedPrisonerContactRelationship)
    verify(transactionalPrisonerContactRelationshipService).getById(prisonerContactId)
  }

  @Test
  fun `should throw EntityNotFoundException when prisoner contact relationship does not exist`() {
    val prisonerContactId = 1L
    whenever(transactionalPrisonerContactRelationshipService.getById(prisonerContactId)).thenThrow(EntityNotFoundException("prisoner contact relationship with id $prisonerContactId not found"))

    val exception = assertThrows<EntityNotFoundException> {
      prisonerContactRelationshipService.getById(prisonerContactId)
    }

    assertThat(exception.message).isEqualTo("prisoner contact relationship with id $prisonerContactId not found")
    verify(transactionalPrisonerContactRelationshipService).getById(prisonerContactId)
  }
}
