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
  fun `migrateNumberOfChildren should save current and historical records and return correct response`() {
    // Given
    val prisonerNumber = "A1234BC"
    val current = NumberOfChildrenDetailsRequest(
      numberOfChildren = "1",
      createdBy = "USER1",
      createdTime = LocalDateTime.now().minusDays(2),
    )
    val historical = NumberOfChildrenDetailsRequest(
      numberOfChildren = "1",
      createdBy = "USER2",
      createdTime = LocalDateTime.now().minusDays(1),
    )

    val request = MigratePrisonerNumberOfChildrenRequest(
      prisonerNumber = prisonerNumber,
      current = current,
      history = listOf(historical),
    )

    val savedEntity1 = PrisonerNumberOfChildren(
      prisonerNumberOfChildrenId = 1L,
      prisonerNumber = prisonerNumber,
      numberOfChildren = current.numberOfChildren,
      createdBy = current.createdBy,
      createdTime = current.createdTime,
      active = true,
    )

    val savedEntity2 = PrisonerNumberOfChildren(
      prisonerNumberOfChildrenId = 2L,
      prisonerNumber = prisonerNumber,
      numberOfChildren = historical.numberOfChildren,
      createdBy = historical.createdBy,
      createdTime = historical.createdTime,
      active = false,
    )

    // When
    whenever(prisonerNumberOfChildrenRepository.saveAll<PrisonerNumberOfChildren>(any())).thenReturn(listOf(savedEntity1, savedEntity2))

    val result = numberOfChildrenMigrationService.migrateNumberOfChildren(request)

    // Then
    verify(prisonerNumberOfChildrenRepository).saveAll(
      org.mockito.kotlin.check<List<PrisonerNumberOfChildren>> { items ->
        assertThat(items).hasSize(2)
        assertThat(items[0].prisonerNumber).isEqualTo(prisonerNumber)
        assertThat(items[0].numberOfChildren).isEqualTo(historical.numberOfChildren)
        assertThat(items[0].createdBy).isEqualTo(historical.createdBy)
        assertThat(items[0].createdTime).isEqualTo(historical.createdTime)
        assertThat(items[0].active).isFalse()
        assertThat(items[1].prisonerNumber).isEqualTo(prisonerNumber)
        assertThat(items[1].numberOfChildren).isEqualTo(current.numberOfChildren)
        assertThat(items[1].createdBy).isEqualTo(current.createdBy)
        assertThat(items[1].createdTime).isEqualTo(current.createdTime)
        assertThat(items[1].active).isTrue()
      },
    )

    assertThat(result.history).hasSize(1)
    assertThat(result.history[0]).isEqualTo(2L)

    assertThat(result.current).isEqualTo(1L)
  }

  @Test
  fun `migrateNumberOfChildren should handle empty history`() {
    // Given
    val prisonerNumber = "A1234BC"
    val createdTime = LocalDateTime.now()
    val request = MigratePrisonerNumberOfChildrenRequest(
      prisonerNumber = prisonerNumber,
      history = emptyList(),
      current = NumberOfChildrenDetailsRequest(
        numberOfChildren = "1",
        createdBy = "USER1",
        createdTime = createdTime,
      ),
    )

    val savedEntity = PrisonerNumberOfChildren(
      prisonerNumberOfChildrenId = 2L,
      prisonerNumber = prisonerNumber,
      numberOfChildren = "1",
      createdBy = "USER1",
      createdTime = createdTime,
      active = true,
    )

    // When
    whenever(prisonerNumberOfChildrenRepository.saveAll<PrisonerNumberOfChildren>(any())).thenReturn(listOf(savedEntity))

    // When
    val result = numberOfChildrenMigrationService.migrateNumberOfChildren(request)

    // Then
    verify(prisonerNumberOfChildrenRepository).saveAll(
      org.mockito.kotlin.check<List<PrisonerNumberOfChildren>> { items ->
        assertThat(items).hasSize(1)
        assertThat(items[0].prisonerNumber).isEqualTo(prisonerNumber)
        assertThat(items[0].numberOfChildren).isEqualTo(savedEntity.numberOfChildren)
        assertThat(items[0].createdBy).isEqualTo(savedEntity.createdBy)
        assertThat(items[0].createdTime).isEqualTo(createdTime)
        assertThat(items[0].active).isTrue()
      },
    )
    assertThat(result.history).isEmpty()
    assertThat(result.current).isEqualTo(2L)
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
    whenever(prisonerNumberOfChildrenRepository.saveAll<PrisonerNumberOfChildren>(any()))
      .thenReturn(listOf(savedEntity))

    val result = numberOfChildrenMigrationService.migrateNumberOfChildren(request)

    // Then
    verify(prisonerNumberOfChildrenRepository).saveAll<PrisonerNumberOfChildren>(any())
    assertThat(result.history).hasSize(1)
    assertThat(result.current).isNull()
  }
}
