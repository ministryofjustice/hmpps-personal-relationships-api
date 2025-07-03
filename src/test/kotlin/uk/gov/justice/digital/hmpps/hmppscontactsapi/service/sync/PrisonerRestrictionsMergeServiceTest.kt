package uk.gov.justice.digital.hmpps.hmppscontactsapi.service.sync

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.PrisonerRestriction
import uk.gov.justice.digital.hmpps.hmppscontactsapi.helpers.isEqualTo
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.PrisonerRestrictionsRepository
import java.time.LocalDate
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
class PrisonerRestrictionsMergeServiceTest {
  @Mock
  private lateinit var prisonerRestrictionsRepository: PrisonerRestrictionsRepository

  @InjectMocks
  private lateinit var prisonerMergeService: PrisonerRestrictionsMergeService

  @Nested
  inner class PrisonerMergeRestrictions {

    private val retainingPrisonerNumber = "A1234BC"
    private val removingPrisonerNumber = "B2345CD"

    @Test
    fun `should move restrictions from removing to retaining prisoner and delete old restrictions`() {
      // Given
      val prisonerRestrictionId0 = 0L
      val prisonerRestrictionId1 = 1L
      val databaseNextIndex = 5L
      val removingRestriction1 = restriction(prisonerRestrictionId0)
      val removingRestriction2 = restriction(prisonerRestrictionId1)
      val removingRestrictions = listOf(removingRestriction1, removingRestriction2)

      whenever(prisonerRestrictionsRepository.findByPrisonerNumber(removingPrisonerNumber))
        .thenReturn(removingRestrictions)
      whenever(prisonerRestrictionsRepository.saveAllAndFlush(any<List<PrisonerRestriction>>()))
        .thenAnswer { invocation ->
          val restrictions = invocation.getArgument<List<PrisonerRestriction>>(0)
          // Simulate saving by assigning new IDs based on the next index
          restrictions.mapIndexed { idx, restriction -> restriction.copy(prisonerRestrictionId = (idx + databaseNextIndex)) }
        }
      // When
      val result = prisonerMergeService.mergePrisonerRestrictions(retainingPrisonerNumber, removingPrisonerNumber)

      // Then
      verify(prisonerRestrictionsRepository).findByPrisonerNumber(removingPrisonerNumber)
      verify(prisonerRestrictionsRepository).deleteByPrisonerNumber(removingPrisonerNumber)
      verify(prisonerRestrictionsRepository).saveAllAndFlush(any<List<PrisonerRestriction>>())
      assertThat(result.wasUpdated).isTrue
    }

    @Test
    fun `should return default response when no restrictions to move`() {
      // Given
      whenever(prisonerRestrictionsRepository.findByPrisonerNumber(removingPrisonerNumber))
        .thenReturn(emptyList())

      // When
      val result = prisonerMergeService.mergePrisonerRestrictions(retainingPrisonerNumber, removingPrisonerNumber)

      // Then
      verify(prisonerRestrictionsRepository).findByPrisonerNumber(removingPrisonerNumber)
      verify(prisonerRestrictionsRepository, never()).saveAllAndFlush(any<List<PrisonerRestriction>>())
      verify(prisonerRestrictionsRepository, never()).deleteByPrisonerNumber(any())
      assertThat(result.wasUpdated).isFalse
    }

    @Test
    fun `should handle exception thrown by saveAllAndFlush and return default response`() {
      val removingRestriction = restriction(1L)
      whenever(prisonerRestrictionsRepository.findByPrisonerNumber(removingPrisonerNumber))
        .thenReturn(listOf(removingRestriction))
      whenever(prisonerRestrictionsRepository.saveAllAndFlush(any<List<PrisonerRestriction>>()))
        .thenThrow(RuntimeException("DB error"))

      assertThrows<RuntimeException> {
        prisonerMergeService.mergePrisonerRestrictions(retainingPrisonerNumber, removingPrisonerNumber)
      }.message isEqualTo "DB error"
    }

    private fun restriction(
      prisonerRestrictionId: Long = 1L,
      prisonerNumber: String = removingPrisonerNumber,
    ) = PrisonerRestriction(
      prisonerRestrictionId,
      prisonerNumber = prisonerNumber,
      restrictionType = "NO_VISIT",
      effectiveDate = LocalDate.of(2024, 1, 1),
      expiryDate = LocalDate.of(2024, 12, 31),
      commentText = "No visits allowed",
      authorisedUsername = "JSMITH",
      currentTerm = true,
      createdBy = "user1",
      createdTime = LocalDateTime.of(2024, 6, 1, 12, 0),
      updatedBy = "user2",
      updatedTime = LocalDateTime.of(2024, 6, 1, 12, 0).plusDays(1),
    )
  }
}
