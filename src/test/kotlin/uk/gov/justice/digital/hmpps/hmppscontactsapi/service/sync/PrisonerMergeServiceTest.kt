package uk.gov.justice.digital.hmpps.hmppscontactsapi.service.sync

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.PrisonerDomesticStatus
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.PrisonerNumberOfChildren
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.PrisonerDomesticStatusRepository
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.PrisonerNumberOfChildrenRepository
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
class PrisonerMergeServiceTest {
  @Mock
  private lateinit var numberOfChildrenRepository: PrisonerNumberOfChildrenRepository

  @Mock
  private lateinit var prisonerDomesticStatusRepository: PrisonerDomesticStatusRepository

  @InjectMocks
  private lateinit var prisonerMergeService: PrisonerMergeService

  @Nested
  inner class PrisonerMergeNumberOfChildren {

    @Test
    fun `should move removing active record to history when both prisoners have active records`() {
      // Given
      val currentTime = LocalDateTime.now()
      val retainingPrisonerNumber = "A1234BC"
      val removingPrisonerNumber = "B2345CD"

      val retainingActiveRecord = PrisonerNumberOfChildren(
        prisonerNumber = retainingPrisonerNumber,
        numberOfChildren = "2",
        createdBy = "user1",
        createdTime = currentTime.minusDays(1),
        active = true,
      )

      val removingActiveRecord = PrisonerNumberOfChildren(
        prisonerNumber = removingPrisonerNumber,
        numberOfChildren = "3",
        createdBy = "user2",
        createdTime = currentTime,
        active = true,
      )

      whenever(numberOfChildrenRepository.findByPrisonerNumberAndActiveTrue(retainingPrisonerNumber))
        .thenReturn(retainingActiveRecord)
      whenever(numberOfChildrenRepository.findByPrisonerNumberAndActiveTrue(removingPrisonerNumber))
        .thenReturn(removingActiveRecord)
      whenever(numberOfChildrenRepository.save(any())).thenAnswer { it.arguments[0] }

      // When
      prisonerMergeService.mergeNumberOfChildren(retainingPrisonerNumber, removingPrisonerNumber)

      // Then
      verify(numberOfChildrenRepository).save(retainingActiveRecord.copy(active = false))
      verify(numberOfChildrenRepository).save(removingActiveRecord.copy(prisonerNumber = retainingPrisonerNumber))
    }

    @Test
    fun `should not save any records when only retaining prisoner has active record`() {
      // Given
      val retainingPrisonerNumber = "A1234BC"
      val removingPrisonerNumber = "B2345CD"
      val retainingActiveRecord = PrisonerNumberOfChildren(
        prisonerNumber = retainingPrisonerNumber,
        numberOfChildren = "2",
        createdBy = "user1",
        createdTime = LocalDateTime.now(),
        active = true,
      )

      whenever(numberOfChildrenRepository.findByPrisonerNumberAndActiveTrue(retainingPrisonerNumber))
        .thenReturn(retainingActiveRecord)
      whenever(numberOfChildrenRepository.findByPrisonerNumberAndActiveTrue(removingPrisonerNumber))
        .thenReturn(null)

      // When
      prisonerMergeService.mergeNumberOfChildren(retainingPrisonerNumber, removingPrisonerNumber)

      // Then
      verify(numberOfChildrenRepository, never()).save(any())
    }

    @Test
    fun `should not save any records when only removing prisoner has active record`() {
      // Given
      val retainingPrisonerNumber = "A1234BC"
      val removingPrisonerNumber = "B2345CD"
      val removingActiveRecord = PrisonerNumberOfChildren(
        prisonerNumber = removingPrisonerNumber,
        numberOfChildren = "3",
        createdBy = "user2",
        createdTime = LocalDateTime.now(),
        active = true,
      )

      whenever(numberOfChildrenRepository.findByPrisonerNumberAndActiveTrue(retainingPrisonerNumber))
        .thenReturn(null)
      whenever(numberOfChildrenRepository.findByPrisonerNumberAndActiveTrue(removingPrisonerNumber))
        .thenReturn(removingActiveRecord)

      // When
      prisonerMergeService.mergeNumberOfChildren(retainingPrisonerNumber, removingPrisonerNumber)

      // Then
      verify(numberOfChildrenRepository, never()).save(any())
    }

    @Test
    fun `should not save any records when neither prisoner has active records`() {
      // Given
      val retainingPrisonerNumber = "A1234BC"
      val removingPrisonerNumber = "B2345CD"

      whenever(numberOfChildrenRepository.findByPrisonerNumberAndActiveTrue(retainingPrisonerNumber))
        .thenReturn(null)
      whenever(numberOfChildrenRepository.findByPrisonerNumberAndActiveTrue(removingPrisonerNumber))
        .thenReturn(null)

      // When
      prisonerMergeService.mergeNumberOfChildren(retainingPrisonerNumber, removingPrisonerNumber)

      // Then
      verify(numberOfChildrenRepository, never()).save(any())
    }

    @Test
    fun `should move both active and inactive records when removing prisoner has both types of records`() {
      // Given
      val retainingPrisonerNumber = "A1234BC"
      val removingPrisonerNumber = "B2345CD"

      val retainingActiveRecord = PrisonerNumberOfChildren(
        prisonerNumber = retainingPrisonerNumber,
        numberOfChildren = "3",
        createdBy = "user1",
        createdTime = LocalDateTime.now(),
        active = true,
      )

      val removingActiveRecord = PrisonerNumberOfChildren(
        prisonerNumber = retainingPrisonerNumber,
        numberOfChildren = "2",
        createdBy = "user1",
        createdTime = LocalDateTime.now().minusDays(1),
        active = true,
      )

      val removingInactiveRecord = PrisonerNumberOfChildren(
        prisonerNumber = removingPrisonerNumber,
        numberOfChildren = "1",
        createdBy = "user2",
        createdTime = LocalDateTime.now().minusDays(2),
        active = false,
      )

      whenever(numberOfChildrenRepository.findByPrisonerNumberAndActiveTrue(retainingPrisonerNumber))
        .thenReturn(retainingActiveRecord)
      whenever(numberOfChildrenRepository.findByPrisonerNumberAndActiveTrue(removingPrisonerNumber))
        .thenReturn(removingActiveRecord)
      whenever(numberOfChildrenRepository.findByPrisonerNumberAndActiveFalse(removingPrisonerNumber))
        .thenReturn(listOf(removingInactiveRecord))

      whenever(numberOfChildrenRepository.save(any())).thenAnswer { it.arguments[0] }

      // When
      prisonerMergeService.mergeNumberOfChildren(retainingPrisonerNumber, removingPrisonerNumber)

      // Then
      verify(numberOfChildrenRepository, times(1)).save(removingInactiveRecord.copy(prisonerNumber = retainingPrisonerNumber))
      verify(numberOfChildrenRepository, times(1)).save(removingActiveRecord.copy(prisonerNumber = retainingPrisonerNumber, active = false))
    }

    @Test
    fun `should update active record when removing prisoner active record is newer than keeping record`() {
      // Given
      val retainingPrisonerNumber = "A1234BC"
      val removingPrisonerNumber = "B2345CD"

      val olderDate = LocalDateTime.now().minusDays(1)
      val newerDate = LocalDateTime.now()
      val retainingActiveRecord = PrisonerNumberOfChildren(
        prisonerNumber = retainingPrisonerNumber,
        numberOfChildren = "3",
        createdBy = "user1",
        createdTime = olderDate,
        active = true,
      )

      val removingActiveRecord = PrisonerNumberOfChildren(
        prisonerNumber = retainingPrisonerNumber,
        numberOfChildren = "2",
        createdBy = "user1",
        createdTime = newerDate,
        active = true,
      )

      whenever(numberOfChildrenRepository.findByPrisonerNumberAndActiveTrue(retainingPrisonerNumber))
        .thenReturn(retainingActiveRecord)
      whenever(numberOfChildrenRepository.findByPrisonerNumberAndActiveTrue(removingPrisonerNumber))
        .thenReturn(removingActiveRecord)

      whenever(numberOfChildrenRepository.save(any())).thenAnswer { it.arguments[0] }

      // When
      prisonerMergeService.mergeNumberOfChildren(retainingPrisonerNumber, removingPrisonerNumber)

      // Then
      verify(numberOfChildrenRepository, times(1)).save(retainingActiveRecord.copy(active = false))
      verify(numberOfChildrenRepository, times(1)).save(removingActiveRecord.copy(prisonerNumber = retainingPrisonerNumber, active = true))
    }

    @Test
    fun `should move removing record as inactive when removing active record is older than retaining active record`() {
      // Given
      val retainingPrisonerNumber = "A1234BC"
      val removingPrisonerNumber = "B5678CD"
      val olderDate = LocalDateTime.now().minusDays(2)
      val newerDate = LocalDateTime.now().minusDays(1)

      val retainingActiveRecord = PrisonerNumberOfChildren(
        prisonerNumber = retainingPrisonerNumber,
        numberOfChildren = "3",
        createdBy = "user1",
        active = true,
        createdTime = newerDate,
      )

      val removingActiveRecord = PrisonerNumberOfChildren(
        prisonerNumber = removingPrisonerNumber,
        numberOfChildren = "2",
        createdBy = "user1",
        active = true,
        createdTime = olderDate,
      )

      whenever(numberOfChildrenRepository.findByPrisonerNumberAndActiveTrue(retainingPrisonerNumber))
        .thenReturn(retainingActiveRecord)
      whenever(numberOfChildrenRepository.findByPrisonerNumberAndActiveTrue(removingPrisonerNumber))
        .thenReturn(removingActiveRecord)
      whenever(numberOfChildrenRepository.findByPrisonerNumberAndActiveFalse(removingPrisonerNumber))
        .thenReturn(emptyList())

      val expectedSavedRecord = removingActiveRecord.copy(
        prisonerNumber = retainingPrisonerNumber,
        active = false,
      )
      whenever(numberOfChildrenRepository.save(expectedSavedRecord))
        .thenReturn(expectedSavedRecord)

      // When
      val result = prisonerMergeService.mergeNumberOfChildren(retainingPrisonerNumber, removingPrisonerNumber)

      // Then
      verify(numberOfChildrenRepository).save(expectedSavedRecord)
      assertThat(result.wasCreated).isFalse
      assertThat(result.id).isEqualTo(expectedSavedRecord.prisonerNumberOfChildrenId)
    }
  }

  @Nested
  inner class PrisonerMergeDomesticStatus {

    @Test
    fun `should move removing active record to history when both prisoners have active records`() {
      // Given
      val currentTime = LocalDateTime.now()
      val retainingPrisonerNumber = "A1234BC"
      val removingPrisonerNumber = "B2345CD"

      val retainingActiveRecord = PrisonerDomesticStatus(
        prisonerNumber = retainingPrisonerNumber,
        domesticStatusCode = "Married",
        createdBy = "user1",
        createdTime = currentTime.minusDays(1),
        active = true,
      )

      val removingActiveRecord = PrisonerDomesticStatus(
        prisonerNumber = removingPrisonerNumber,
        domesticStatusCode = "Single",
        createdBy = "user2",
        createdTime = currentTime,
        active = true,
      )

      whenever(prisonerDomesticStatusRepository.findByPrisonerNumberAndActiveTrue(retainingPrisonerNumber))
        .thenReturn(retainingActiveRecord)
      whenever(prisonerDomesticStatusRepository.findByPrisonerNumberAndActiveTrue(removingPrisonerNumber))
        .thenReturn(removingActiveRecord)
      whenever(prisonerDomesticStatusRepository.save(any())).thenAnswer { it.arguments[0] }

      // When
      prisonerMergeService.mergeDomesticStatus(retainingPrisonerNumber, removingPrisonerNumber)

      // Then
      verify(prisonerDomesticStatusRepository).save(retainingActiveRecord.copy(active = false))
      verify(prisonerDomesticStatusRepository).save(removingActiveRecord.copy(prisonerNumber = retainingPrisonerNumber))
    }

    @Test
    fun `should not save any records when only retaining prisoner has active record`() {
      // Given
      val retainingPrisonerNumber = "A1234BC"
      val removingPrisonerNumber = "B2345CD"
      val retainingActiveRecord = PrisonerDomesticStatus(
        prisonerNumber = retainingPrisonerNumber,
        domesticStatusCode = "Married",
        createdBy = "user1",
        createdTime = LocalDateTime.now(),
        active = true,
      )

      whenever(prisonerDomesticStatusRepository.findByPrisonerNumberAndActiveTrue(retainingPrisonerNumber))
        .thenReturn(retainingActiveRecord)
      whenever(prisonerDomesticStatusRepository.findByPrisonerNumberAndActiveTrue(removingPrisonerNumber))
        .thenReturn(null)

      // When
      prisonerMergeService.mergeDomesticStatus(retainingPrisonerNumber, removingPrisonerNumber)

      // Then
      verify(prisonerDomesticStatusRepository, never()).save(any())
    }

    @Test
    fun `should not save any records when only removing prisoner has active record`() {
      val retainingPrisonerNumber = "A1234BC"
      val removingPrisonerNumber = "B2345CD"
      val removingActiveRecord = PrisonerDomesticStatus(
        prisonerNumber = removingPrisonerNumber,
        domesticStatusCode = "Single",
        createdBy = "user2",
        createdTime = LocalDateTime.now(),
        active = true,
      )

      whenever(prisonerDomesticStatusRepository.findByPrisonerNumberAndActiveTrue(retainingPrisonerNumber))
        .thenReturn(null)
      whenever(prisonerDomesticStatusRepository.findByPrisonerNumberAndActiveTrue(removingPrisonerNumber))
        .thenReturn(removingActiveRecord)

      // When
      prisonerMergeService.mergeDomesticStatus(retainingPrisonerNumber, removingPrisonerNumber)

      // Then
      verify(prisonerDomesticStatusRepository, never()).save(any())
    }

    @Test
    fun `should not save any records when neither prisoner has active records`() {
      val retainingPrisonerNumber = "A1234BC"
      val removingPrisonerNumber = "B2345CD"

      whenever(prisonerDomesticStatusRepository.findByPrisonerNumberAndActiveTrue(retainingPrisonerNumber))
        .thenReturn(null)
      whenever(prisonerDomesticStatusRepository.findByPrisonerNumberAndActiveTrue(removingPrisonerNumber))
        .thenReturn(null)

      // When
      prisonerMergeService.mergeDomesticStatus(retainingPrisonerNumber, removingPrisonerNumber)

      // Then
      verify(prisonerDomesticStatusRepository, never()).save(any())
    }

    @Test
    fun `should move both active and inactive records when removing prisoner has both types of records`() {
      // Given
      val retainingPrisonerNumber = "A1234BC"
      val removingPrisonerNumber = "B2345CD"

      val newerDate = LocalDateTime.now()
      val retainingActiveRecord = PrisonerDomesticStatus(
        prisonerNumber = retainingPrisonerNumber,
        domesticStatusCode = "Married",
        createdBy = "user1",
        createdTime = newerDate,
        active = true,
      )

      val olderDate = LocalDateTime.now().minusDays(1)
      val removingActiveRecord = PrisonerDomesticStatus(
        prisonerNumber = retainingPrisonerNumber,
        domesticStatusCode = "Single",
        createdBy = "user1",
        createdTime = olderDate,
        active = true,
      )

      val oldestDate = LocalDateTime.now().minusDays(2)
      val removingInactiveRecord = PrisonerDomesticStatus(
        prisonerNumber = removingPrisonerNumber,
        domesticStatusCode = "Divorced",
        createdBy = "user2",
        createdTime = oldestDate,
        active = false,
      )

      whenever(prisonerDomesticStatusRepository.findByPrisonerNumberAndActiveTrue(retainingPrisonerNumber))
        .thenReturn(retainingActiveRecord)
      whenever(prisonerDomesticStatusRepository.findByPrisonerNumberAndActiveTrue(removingPrisonerNumber))
        .thenReturn(removingActiveRecord)
      whenever(prisonerDomesticStatusRepository.findByPrisonerNumberAndActiveFalse(removingPrisonerNumber))
        .thenReturn(listOf(removingInactiveRecord))

      whenever(prisonerDomesticStatusRepository.save(any())).thenAnswer { it.arguments[0] }

      // When
      prisonerMergeService.mergeDomesticStatus(retainingPrisonerNumber, removingPrisonerNumber)

      // Then
      verify(prisonerDomesticStatusRepository, times(1)).save(removingInactiveRecord.copy(prisonerNumber = retainingPrisonerNumber))
      verify(prisonerDomesticStatusRepository, times(1)).save(removingActiveRecord.copy(prisonerNumber = retainingPrisonerNumber, active = false))
    }

    @Test
    fun `should update active record when removing prisoner active record is newer than keeping record`() {
      // Given
      val retainingPrisonerNumber = "A1234BC"
      val removingPrisonerNumber = "B2345CD"

      val olderDate = LocalDateTime.now().minusDays(1)
      val retainingActiveRecord = PrisonerDomesticStatus(
        prisonerNumber = retainingPrisonerNumber,
        domesticStatusCode = "Married",
        createdBy = "user1",
        createdTime = olderDate,
        active = true,
      )

      val newDate = LocalDateTime.now()
      val removingActiveRecord = PrisonerDomesticStatus(
        prisonerNumber = retainingPrisonerNumber,
        domesticStatusCode = "Single",
        createdBy = "user1",
        createdTime = newDate,
        active = true,
      )

      whenever(prisonerDomesticStatusRepository.findByPrisonerNumberAndActiveTrue(retainingPrisonerNumber))
        .thenReturn(retainingActiveRecord)
      whenever(prisonerDomesticStatusRepository.findByPrisonerNumberAndActiveTrue(removingPrisonerNumber))
        .thenReturn(removingActiveRecord)

      whenever(prisonerDomesticStatusRepository.save(any())).thenAnswer { it.arguments[0] }

      // When
      prisonerMergeService.mergeDomesticStatus(retainingPrisonerNumber, removingPrisonerNumber)

      // Then
      verify(prisonerDomesticStatusRepository, times(1)).save(retainingActiveRecord.copy(active = false))
      verify(prisonerDomesticStatusRepository, times(1)).save(removingActiveRecord.copy(prisonerNumber = retainingPrisonerNumber, active = true))
    }

    @Test
    fun `should move removing record as inactive when removing active record is older than retaining active record`() {
      // Given
      val retainingPrisonerNumber = "A1234BC"
      val removingPrisonerNumber = "B5678CD"
      val olderDate = LocalDateTime.now().minusDays(2)
      val newerDate = LocalDateTime.now().minusDays(1)

      val retainingActiveRecord = PrisonerDomesticStatus(
        prisonerNumber = retainingPrisonerNumber,
        domesticStatusCode = "Married",
        createdBy = "user1",
        active = true,
        createdTime = newerDate,
      )

      val removingActiveRecord = PrisonerDomesticStatus(
        prisonerNumber = removingPrisonerNumber,
        domesticStatusCode = "Single",
        createdBy = "user1",
        active = true,
        createdTime = olderDate,
      )

      whenever(prisonerDomesticStatusRepository.findByPrisonerNumberAndActiveTrue(retainingPrisonerNumber))
        .thenReturn(retainingActiveRecord)
      whenever(prisonerDomesticStatusRepository.findByPrisonerNumberAndActiveTrue(removingPrisonerNumber))
        .thenReturn(removingActiveRecord)
      whenever(prisonerDomesticStatusRepository.findByPrisonerNumberAndActiveFalse(removingPrisonerNumber))
        .thenReturn(emptyList())

      val expectedSavedRecord = removingActiveRecord.copy(
        prisonerNumber = retainingPrisonerNumber,
        active = false,
      )
      whenever(prisonerDomesticStatusRepository.save(expectedSavedRecord))
        .thenReturn(expectedSavedRecord)

      // When
      val result = prisonerMergeService.mergeDomesticStatus(retainingPrisonerNumber, removingPrisonerNumber)

      // Then
      verify(prisonerDomesticStatusRepository).save(expectedSavedRecord)
      assertThat(result.wasCreated).isFalse
      assertThat(result.id).isEqualTo(expectedSavedRecord.prisonerDomesticStatusId)
    }
  }
}
