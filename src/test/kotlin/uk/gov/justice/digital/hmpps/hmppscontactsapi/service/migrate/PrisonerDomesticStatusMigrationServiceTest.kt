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
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.PrisonerDomesticStatus
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.migrate.DomesticStatusDetailsRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.migrate.MigratePrisonerDomesticStatusRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.PrisonerDomesticStatusRepository
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
class PrisonerDomesticStatusMigrationServiceTest {

  @Mock
  private lateinit var prisonerDomesticStatusRepository: PrisonerDomesticStatusRepository

  @InjectMocks
  private lateinit var domesticStatusMigrationService: PrisonerDomesticStatusMigrationService

  @Test
  fun `migrateDomesticStatus should save historical records and return correct response`() {
    // Given
    val prisonerNumber = "A1234BC"
    val historyItem1 = DomesticStatusDetailsRequest(
      domesticStatusCode = "MARRIED",
      createdBy = "USER1",
      createdTime = LocalDateTime.now().minusDays(2),
    )
    val historyItem2 = DomesticStatusDetailsRequest(
      domesticStatusCode = "SINGLE",
      createdBy = "USER2",
      createdTime = LocalDateTime.now().minusDays(1),
    )

    val request = MigratePrisonerDomesticStatusRequest(
      prisonerNumber = prisonerNumber,
      history = listOf(historyItem1, historyItem2),
    )

    val savedEntity1 = PrisonerDomesticStatus(
      prisonerDomesticStatusId = 1L,
      prisonerNumber = prisonerNumber,
      domesticStatusCode = historyItem1.domesticStatusCode,
      createdBy = historyItem1.createdBy,
      createdTime = historyItem1.createdTime,
      active = false,
    )

    val savedEntity2 = PrisonerDomesticStatus(
      prisonerDomesticStatusId = 2L,
      prisonerNumber = prisonerNumber,
      domesticStatusCode = historyItem2.domesticStatusCode,
      createdBy = historyItem2.createdBy,
      createdTime = historyItem2.createdTime,
      active = false,
    )

    // When
    whenever(prisonerDomesticStatusRepository.save(any())).thenReturn(savedEntity1, savedEntity2)

    val result = domesticStatusMigrationService.migrateDomesticStatus(request)

    // Then
    verify(prisonerDomesticStatusRepository, times(2)).save(any())

    assertThat(result.history).hasSize(2)
    assertThat(result.history[0].id).isEqualTo(1L)

    assertThat(result.current.id).isEqualTo(2L)
  }

  @Test
  fun `migrateDomesticStatus should handle empty history`() {
    // Given
    val request = MigratePrisonerDomesticStatusRequest(
      prisonerNumber = "A1234BC",
      history = emptyList(),
      current = DomesticStatusDetailsRequest(
        domesticStatusCode = "D",
        createdBy = "USER1",
        createdTime = LocalDateTime.now(),
      ),
    )

    val savedEntity = PrisonerDomesticStatus(
      prisonerDomesticStatusId = 2L,
      prisonerNumber = request.prisonerNumber,
      domesticStatusCode = "D",
      createdBy = "USER1",
      createdTime = LocalDateTime.now(),
      active = false,
    )

    // When
    whenever(prisonerDomesticStatusRepository.save(any())).thenReturn(savedEntity).thenReturn(savedEntity)

    // When
    val result = domesticStatusMigrationService.migrateDomesticStatus(request)

    // Then
    verify(prisonerDomesticStatusRepository, times(1)).save(any())
    assertThat(result.history).isEmpty()
  }

  @Test
  fun `migrateDomesticStatus should handle single history item`() {
    // Given
    val prisonerNumber = "A1234BC"
    val historyItem = DomesticStatusDetailsRequest(
      domesticStatusCode = "MARRIED",
      createdBy = "USER1",
      createdTime = LocalDateTime.now(),
    )

    val request = MigratePrisonerDomesticStatusRequest(
      prisonerNumber = prisonerNumber,
      history = listOf(historyItem),
    )

    val savedEntity = PrisonerDomesticStatus(
      prisonerDomesticStatusId = 1L,
      prisonerNumber = prisonerNumber,
      domesticStatusCode = historyItem.domesticStatusCode,
      createdBy = historyItem.createdBy,
      createdTime = historyItem.createdTime,
      active = false,
    )

    // When
    whenever(prisonerDomesticStatusRepository.save(any())).thenReturn(savedEntity)

    val result = domesticStatusMigrationService.migrateDomesticStatus(request)

    // Then
    verify(prisonerDomesticStatusRepository, times(1)).save(any())
    assertThat(result.history).hasSize(1)
    assertThat(result.current.id).isEqualTo(1L)
  }
}
