package uk.gov.justice.digital.hmpps.hmppscontactsapi.service.migrate

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.PrisonerNumberOfChildren
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.migrate.MigratePrisonerNumberOfChildrenRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.migrate.NumberOfChildrenDetailsRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.PrisonerNumberOfChildrenRepository
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
class PrisonerNumberOfChildrenMigrationServiceTest {

  @Mock
  private lateinit var prisonerNumberOfChildrenRepository: PrisonerNumberOfChildrenRepository

  @InjectMocks
  private lateinit var numberOfChildrenMigrationService: PrisonerNumberOfChildrenMigrationService

  @Test
  fun `migrateNumberOfChildren should save historical records and return correct response`() {
    // Given
    val prisonerNumber = "A1234BC"
    val historyItem1 = NumberOfChildrenDetailsRequest(
      numberOfChildren = "1",
      createdBy = "USER1",
      createdTime = LocalDateTime.now().minusDays(2),
    )
    val historyItem2 = NumberOfChildrenDetailsRequest(
      numberOfChildren = "1",
      createdBy = "USER2",
      createdTime = LocalDateTime.now().minusDays(1),
    )

    val request = MigratePrisonerNumberOfChildrenRequest(
      prisonerNumber = prisonerNumber,
      history = listOf(historyItem1, historyItem2),
    )

    val savedEntity1 = PrisonerNumberOfChildren(
      prisonerNumberOfChildrenId = 1L,
      prisonerNumber = prisonerNumber,
      numberOfChildren = historyItem1.numberOfChildren,
      createdBy = historyItem1.createdBy,
      createdTime = historyItem1.createdTime,
      active = false,
    )

    val savedEntity2 = PrisonerNumberOfChildren(
      prisonerNumberOfChildrenId = 2L,
      prisonerNumber = prisonerNumber,
      numberOfChildren = historyItem2.numberOfChildren,
      createdBy = historyItem2.createdBy,
      createdTime = historyItem2.createdTime,
      active = false,
    )

    // When
    whenever(prisonerNumberOfChildrenRepository.save(any())).thenReturn(savedEntity1, savedEntity2)

    val result = numberOfChildrenMigrationService.migrateNumberOfChildren(request)

    // Then
    verify(prisonerNumberOfChildrenRepository, times(2)).save(any())

    assertThat(result.history).hasSize(2)
    assertThat(result.history[0]).isEqualTo(1L)

    assertThat(result.current).isEqualTo(2L)
  }

  @Test
  fun `migrateNumberOfChildren should handle empty history`() {
    // Given
    val request = MigratePrisonerNumberOfChildrenRequest(
      prisonerNumber = "A1234BC",
      history = emptyList(),
      current = NumberOfChildrenDetailsRequest(
        numberOfChildren = "1",
        createdBy = "USER1",
        createdTime = LocalDateTime.now(),
      ),
    )

    val savedEntity = PrisonerNumberOfChildren(
      prisonerNumberOfChildrenId = 2L,
      prisonerNumber = request.prisonerNumber,
      numberOfChildren = "1",
      createdBy = "USER1",
      createdTime = LocalDateTime.now(),
      active = false,
    )

    // When
    whenever(prisonerNumberOfChildrenRepository.save(any())).thenReturn(savedEntity).thenReturn(savedEntity)

    // When
    val result = numberOfChildrenMigrationService.migrateNumberOfChildren(request)

    // Then
    verify(prisonerNumberOfChildrenRepository, times(1)).save(any())
    assertThat(result.history).isEmpty()
  }

  @Test
  fun `migrateNumberOfChildren should handle single history item`() {
    // Given
    val prisonerNumber = "A1234BC"
    val historyItem = NumberOfChildrenDetailsRequest(
      numberOfChildren = "1",
      createdBy = "USER1",
      createdTime = LocalDateTime.now(),
    )

    val request = MigratePrisonerNumberOfChildrenRequest(
      prisonerNumber = prisonerNumber,
      history = listOf(historyItem),
    )

    val savedEntity = PrisonerNumberOfChildren(
      prisonerNumberOfChildrenId = 1L,
      prisonerNumber = prisonerNumber,
      numberOfChildren = historyItem.numberOfChildren,
      createdBy = historyItem.createdBy,
      createdTime = historyItem.createdTime,
      active = false,
    )

    // When
    whenever(prisonerNumberOfChildrenRepository.save(any())).thenReturn(savedEntity)

    val result = numberOfChildrenMigrationService.migrateNumberOfChildren(request)

    // Then
    verify(prisonerNumberOfChildrenRepository, times(1)).save(any())
    assertThat(result.history).hasSize(1)
    assertThat(result.current).isEqualTo(1L)
  }
}
