package uk.gov.justice.digital.hmpps.personalrelationships.service

import jakarta.persistence.EntityNotFoundException
import jakarta.validation.ValidationException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.groups.Tuple
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.personalrelationships.client.prisonersearch.Prisoner
import uk.gov.justice.digital.hmpps.personalrelationships.helpers.createPrisonerContactEntity
import uk.gov.justice.digital.hmpps.personalrelationships.helpers.makePrisonerContact
import uk.gov.justice.digital.hmpps.personalrelationships.helpers.prisoner
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.PrisonerAndContactId
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.PrisonerContactRelationshipsRequest
import uk.gov.justice.digital.hmpps.personalrelationships.repository.PrisonerContactRepository
import uk.gov.justice.digital.hmpps.personalrelationships.repository.PrisonerContactSummaryRepository
import java.time.LocalDate
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class TransactionalPrisonerContactRelationshipServiceTest {
  @Mock
  private lateinit var prisonerContactSummaryRepository: PrisonerContactSummaryRepository

  @Mock
  private lateinit var prisonerContactRepository: PrisonerContactRepository

  @InjectMocks
  private lateinit var transactionalPrisonerContactRelationshipService: TransactionalPrisonerContactRelationshipService

  private lateinit var prisoner: Prisoner

  private val prisonerNumber = "A1111AA"

  @BeforeEach
  fun before() {
    prisoner = prisoner(prisonerNumber, prisonId = "MDI")
  }

  @Test
  fun `should return when prisoner contact relationship exists`() {
    val prisonerContactId = 1L
    val expectedPrisonerContactRelationship = makePrisonerContact(
      prisonerContactId,
      2,
      LocalDate.of(2000, 11, 21),
      "Jack",
      "Doe",
    )

    val prisonerContactSummaryEntity = makePrisonerContact(
      prisonerContactId = 1L,
      contactId = 2L,
      dateOfBirth = LocalDate.of(2000, 11, 21),
      firstName = "Jack",
      lastName = "Doe",
    )

    whenever(prisonerContactSummaryRepository.findById(prisonerContactId)).thenReturn(Optional.of(prisonerContactSummaryEntity))

    val actualPrisonerContactRelationship = transactionalPrisonerContactRelationshipService.getById(prisonerContactId)

    assertThat(actualPrisonerContactRelationship).isEqualTo(expectedPrisonerContactRelationship)
    verify(prisonerContactSummaryRepository).findById(prisonerContactId)
  }

  @Test
  fun `should throw EntityNotFoundException when prisoner contact relationship does not exist`() {
    val prisonerContactId = 1L
    whenever(prisonerContactSummaryRepository.findById(prisonerContactId)).thenReturn(Optional.empty())

    val exception = assertThrows<EntityNotFoundException> {
      transactionalPrisonerContactRelationshipService.getById(prisonerContactId)
    }

    assertThat(exception.message).isEqualTo("prisoner contact relationship with id $prisonerContactId not found")
    verify(prisonerContactSummaryRepository).findById(prisonerContactId)
  }

  @Nested
  @DisplayName("getSummaryRelationships")
  inner class GetSummaryRelationshipsTests {
    @Test
    fun `should throw ValidationFoundException when an empty list of identifiers is provided`() {
      val request = PrisonerContactRelationshipsRequest(
        identifiers = emptyList(),
      )

      val exception = assertThrows<ValidationException> {
        transactionalPrisonerContactRelationshipService.getSummaryRelationships(request)
      }

      assertThat(exception.message).isEqualTo("No identifiers were provided in the request")

      verifyNoInteractions(prisonerContactRepository)
    }

    @Test
    fun `should return a single prisoner relationship`() {
      val prisonerContact = createPrisonerContactEntity(
        prisonerContactId = 1L,
        contactId = 2L,
        prisonerNumber = "A1234BC",
        relationshipType = "S",
        relationshipToPrisoner = "SIS",
        active = true,
        approvedVisitor = false,
        currentTerm = true,
      )

      val request = PrisonerContactRelationshipsRequest(
        identifiers = listOf(
          PrisonerAndContactId(prisonerNumber = "A1234BC", contactId = 2L),
        ),
      )

      val prisonerList = request.identifiers.map { it.prisonerNumber }

      whenever(
        prisonerContactRepository.getCurrentRelationshipsForPrisoners(prisonerList),
      ).thenReturn(listOf(prisonerContact))

      val response = transactionalPrisonerContactRelationshipService.getSummaryRelationships(request)

      assertThat(response.responses).hasSize(1)
      with(response.responses.first()) {
        assertThat(prisonerNumber).isEqualTo(prisonerContact.prisonerNumber)
        assertThat(contactId).isEqualTo(prisonerContact.contactId)
        assertThat(relationships).hasSize(1)
        assertThat(relationships.first().relationshipTypeCode).isEqualTo(prisonerContact.relationshipType)
        assertThat(relationships.first().relationshipToPrisonerCode).isEqualTo(prisonerContact.relationshipToPrisoner)
        assertThat(relationships.first().isApprovedVisitor).isEqualTo(prisonerContact.approvedVisitor)
        assertThat(relationships.first().currentTerm).isEqualTo(prisonerContact.currentTerm)
      }

      verify(prisonerContactRepository).getCurrentRelationshipsForPrisoners(prisonerList)
    }

    @Test
    fun `should return multiple prisoner relationships`() {
      val prisonerContacts = listOf(
        createPrisonerContactEntity(
          prisonerContactId = 1L,
          contactId = 8L,
          prisonerNumber = "A1234AA",
          relationshipType = "S",
          relationshipToPrisoner = "SIS",
          approvedVisitor = false,
          currentTerm = true,
        ),
        createPrisonerContactEntity(
          prisonerContactId = 2L,
          contactId = 9L,
          prisonerNumber = "A1234BB",
          relationshipType = "O",
          relationshipToPrisoner = "POL",
          approvedVisitor = true,
          currentTerm = true,
        ),
        createPrisonerContactEntity(
          prisonerContactId = 3L,
          contactId = 10L,
          prisonerNumber = "A1234CC",
          relationshipType = "O",
          relationshipToPrisoner = "SOL",
          approvedVisitor = true,
          currentTerm = true,
        ),
      )

      val request = PrisonerContactRelationshipsRequest(
        identifiers = listOf(
          PrisonerAndContactId(prisonerNumber = "A1234AA", contactId = 8L),
          PrisonerAndContactId(prisonerNumber = "A1234BB", contactId = 9L),
          PrisonerAndContactId(prisonerNumber = "A1234CC", contactId = 10L),
        ),
      )

      val prisonerList = request.identifiers.map { it.prisonerNumber }

      whenever(
        prisonerContactRepository.getCurrentRelationshipsForPrisoners(prisonerList),
      ).thenReturn(prisonerContacts)

      val response = transactionalPrisonerContactRelationshipService.getSummaryRelationships(request)

      assertThat(response.responses).hasSize(3)

      // Check that the responses reflect the request structure
      assertThat(response.responses)
        .extracting("prisonerNumber", "contactId")
        .containsAll(
          listOf(
            Tuple("A1234AA", 8L),
            Tuple("A1234BB", 9L),
            Tuple("A1234CC", 10L),
          ),
        )

      // Check the size of the responses
      assertThat(response.responses[0].relationships).hasSize(1)
      assertThat(response.responses[1].relationships).hasSize(1)
      assertThat(response.responses[2].relationships).hasSize(1)

      verify(prisonerContactRepository).getCurrentRelationshipsForPrisoners(prisonerList)
    }

    @Test
    fun `should return no matching relationships for a valid request`() {
      val request = PrisonerContactRelationshipsRequest(
        identifiers = listOf(
          PrisonerAndContactId(prisonerNumber = "A1234BC", contactId = 2L),
        ),
      )

      val prisonerList = request.identifiers.map { it.prisonerNumber }

      // Mock no matches in the prisoner's relationships
      whenever(
        prisonerContactRepository.getCurrentRelationshipsForPrisoners(prisonerList),
      ).thenReturn(emptyList())

      val response = transactionalPrisonerContactRelationshipService.getSummaryRelationships(request)

      assertThat(response.responses).hasSize(1)
      with(response.responses.first()) {
        assertThat(relationships).isEmpty()
      }

      verify(prisonerContactRepository).getCurrentRelationshipsForPrisoners(prisonerList)
    }

    @Test
    fun `should flatten and remove duplicates in the request`() {
      val prisonerContacts = listOf(
        createPrisonerContactEntity(
          prisonerContactId = 1L,
          contactId = 8L,
          prisonerNumber = "A1234AA",
          relationshipType = "S",
          relationshipToPrisoner = "SIS",
          approvedVisitor = false,
          currentTerm = true,
        ),
      )

      val request = PrisonerContactRelationshipsRequest(
        identifiers = listOf(
          PrisonerAndContactId(prisonerNumber = "A1234AA", contactId = 8L),
          PrisonerAndContactId(prisonerNumber = "A1234AA", contactId = 8L),
        ),
      )

      val prisonerList = request.identifiers.map { it.prisonerNumber }.toSet().toList()

      whenever(
        prisonerContactRepository.getCurrentRelationshipsForPrisoners(prisonerList),
      ).thenReturn(prisonerContacts)

      val response = transactionalPrisonerContactRelationshipService.getSummaryRelationships(request)

      assertThat(response.responses).hasSize(1)
      assertThat(response.responses[0].relationships).hasSize(1)

      verify(prisonerContactRepository).getCurrentRelationshipsForPrisoners(prisonerList)
    }
  }
}
