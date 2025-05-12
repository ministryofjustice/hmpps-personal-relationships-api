package uk.gov.justice.digital.hmpps.hmppscontactsapi.service.migrate

import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.check
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.PrisonerDomesticStatus
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.ReferenceCodeEntity
import uk.gov.justice.digital.hmpps.hmppscontactsapi.helpers.isEqualTo
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.ReferenceCodeGroup
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.migrate.DomesticStatusDetailsRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.migrate.MigratePrisonerDomesticStatusRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.PrisonerDomesticStatusRepository
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.ReferenceCodeRepository
import java.time.LocalDateTime.now

@ExtendWith(MockitoExtension::class)
class PrisonerDomesticStatusMigrationServiceTest {

  @Mock
  private lateinit var prisonerDomesticStatusRepository: PrisonerDomesticStatusRepository

  @Mock
  private lateinit var referenceCodeRepository: ReferenceCodeRepository

  @InjectMocks
  private lateinit var domesticStatusMigrationService: PrisonerDomesticStatusMigrationService

  private val referenceData =
    ReferenceCodeEntity(1L, ReferenceCodeGroup.DOMESTIC_STS, "M", "Married", 0, true, "name")

  @Test
  fun `should save current and historical records and return correct response`() {
    // Given
    val prisonerNumber = "A1234BC"
    val current = DomesticStatusDetailsRequest(
      domesticStatusCode = "M",
      createdBy = "USER1",
      createdTime = now().minusDays(2),
    )
    val historical = DomesticStatusDetailsRequest(
      domesticStatusCode = "S",
      createdBy = "USER2",
      createdTime = now().minusDays(1),
    )

    val request = MigratePrisonerDomesticStatusRequest(
      prisonerNumber = prisonerNumber,
      current = current,
      history = listOf(historical),
    )

    val savedEntity1 = PrisonerDomesticStatus(
      prisonerDomesticStatusId = 1L,
      prisonerNumber = prisonerNumber,
      domesticStatusCode = current.domesticStatusCode,
      createdBy = current.createdBy,
      createdTime = current.createdTime,
      active = true,
    )

    val savedEntity2 = PrisonerDomesticStatus(
      prisonerDomesticStatusId = 2L,
      prisonerNumber = prisonerNumber,
      domesticStatusCode = historical.domesticStatusCode,
      createdBy = historical.createdBy,
      createdTime = historical.createdTime,
      active = false,
    )

    // When
    whenever(referenceCodeRepository.findByGroupCodeAndCode(any(), any())).thenReturn(referenceData)
    whenever(prisonerDomesticStatusRepository.saveAllAndFlush<PrisonerDomesticStatus>(any())).thenReturn(listOf(savedEntity1, savedEntity2))

    val result = domesticStatusMigrationService.migrateDomesticStatus(request)

    // Then
    verify(prisonerDomesticStatusRepository).saveAllAndFlush(
      check<List<PrisonerDomesticStatus>> { items ->
        assertThat(items).hasSize(2)
        assertThat(items[0].prisonerNumber).isEqualTo(prisonerNumber)
        assertThat(items[0].domesticStatusCode).isEqualTo(historical.domesticStatusCode)
        assertThat(items[0].createdBy).isEqualTo(historical.createdBy)
        assertThat(items[0].createdTime).isEqualTo(historical.createdTime)
        assertThat(items[0].active).isFalse()
        assertThat(items[1].prisonerNumber).isEqualTo(prisonerNumber)
        assertThat(items[1].domesticStatusCode).isEqualTo(current.domesticStatusCode)
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
  fun `should handle empty history`() {
    // Given
    val prisonerNumber = "A1234BC"
    val createdTime = now()
    val request = MigratePrisonerDomesticStatusRequest(
      prisonerNumber = prisonerNumber,
      history = emptyList(),
      current = DomesticStatusDetailsRequest(
        domesticStatusCode = "D",
        createdBy = "USER1",
        createdTime = createdTime,
      ),
    )

    val savedEntity = PrisonerDomesticStatus(
      prisonerDomesticStatusId = 2L,
      prisonerNumber = request.prisonerNumber,
      domesticStatusCode = "D",
      createdBy = "USER1",
      createdTime = createdTime,
      active = true,
    )

    // When
    whenever(referenceCodeRepository.findByGroupCodeAndCode(any(), any())).thenReturn(referenceData)
    whenever(prisonerDomesticStatusRepository.saveAllAndFlush<PrisonerDomesticStatus>(any()))
      .thenReturn(listOf(savedEntity))

    // When
    val result = domesticStatusMigrationService.migrateDomesticStatus(request)

    // Then
    verify(prisonerDomesticStatusRepository).saveAllAndFlush(
      check<List<PrisonerDomesticStatus>> { items ->
        assertThat(items).hasSize(1)
        assertThat(items[0].prisonerNumber).isEqualTo(prisonerNumber)
        assertThat(items[0].domesticStatusCode).isEqualTo(savedEntity.domesticStatusCode)
        assertThat(items[0].createdBy).isEqualTo(savedEntity.createdBy)
        assertThat(items[0].createdTime).isEqualTo(createdTime)
        assertThat(items[0].active).isTrue()
      },
    )
    assertThat(result.history).isEmpty()
    assertThat(result.current).isEqualTo(2L)
  }

  @Test
  fun `should overwrite existing records`() {
    // Given
    val prisonerNumber = "A1234BC"
    val current = DomesticStatusDetailsRequest(
      domesticStatusCode = "M",
      createdBy = "USER1",
      createdTime = now(),
    )
    val historical = DomesticStatusDetailsRequest(
      domesticStatusCode = "S",
      createdBy = "USER2",
      createdTime = now().minusDays(1),
    )

    val request = MigratePrisonerDomesticStatusRequest(
      prisonerNumber = prisonerNumber,
      current = current,
      history = listOf(historical),
    )

    val savedCurrent = PrisonerDomesticStatus(
      prisonerDomesticStatusId = 1L,
      prisonerNumber = prisonerNumber,
      domesticStatusCode = "D",
      createdBy = "Admin",
      createdTime = now().minusDays(5),
      active = true,
    )

    val savedHistory = PrisonerDomesticStatus(
      prisonerDomesticStatusId = 2L,
      prisonerNumber = prisonerNumber,
      domesticStatusCode = "M",
      createdBy = "User1",
      createdTime = now().minusDays(6),
      active = false,
    )

    // When
    whenever(referenceCodeRepository.findByGroupCodeAndCode(any(), any())).thenReturn(referenceData)
    whenever(prisonerDomesticStatusRepository.saveAllAndFlush<PrisonerDomesticStatus>(any())).thenReturn(listOf(savedCurrent, savedHistory))
    whenever(prisonerDomesticStatusRepository.deleteByPrisonerNumber(prisonerNumber)).then {}

    val result = domesticStatusMigrationService.migrateDomesticStatus(request)
    assertThat(result.history).hasSize(1)
    assertThat(result.history[0]).isEqualTo(2L)
    assertThat(result.current).isEqualTo(1L)
    verify(prisonerDomesticStatusRepository).deleteByPrisonerNumber(prisonerNumber)

    // Then
    val argumentCaptor = argumentCaptor<List<PrisonerDomesticStatus>>()
    verify(prisonerDomesticStatusRepository).saveAllAndFlush(argumentCaptor.capture())
    val items = argumentCaptor.firstValue
    assertThat(items).hasSize(2)
    assertThat(items[0].prisonerNumber).isEqualTo(prisonerNumber)
    assertThat(items[0].domesticStatusCode).isEqualTo(historical.domesticStatusCode)
    assertThat(items[0].createdBy).isEqualTo(historical.createdBy)
    assertThat(items[0].createdTime).isEqualTo(historical.createdTime)
    assertThat(items[0].active).isFalse()
    assertThat(items[1].prisonerNumber).isEqualTo(prisonerNumber)
    assertThat(items[1].domesticStatusCode).isEqualTo(current.domesticStatusCode)
    assertThat(items[1].createdBy).isEqualTo(current.createdBy)
    assertThat(items[1].createdTime).isEqualTo(current.createdTime)
    assertThat(items[1].active).isTrue()
  }

  @Test
  fun `should handle single history item`() {
    // Given
    val prisonerNumber = "A1234BC"
    val historyItem = DomesticStatusDetailsRequest(
      domesticStatusCode = "M",
      createdBy = "USER1",
      createdTime = now(),
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
    whenever(referenceCodeRepository.findByGroupCodeAndCode(any(), any())).thenReturn(referenceData)
    whenever(prisonerDomesticStatusRepository.saveAllAndFlush<PrisonerDomesticStatus>(any()))
      .thenReturn(listOf(savedEntity))

    val result = domesticStatusMigrationService.migrateDomesticStatus(request)

    // Then
    verify(prisonerDomesticStatusRepository).saveAllAndFlush<PrisonerDomesticStatus>(any())
    assertThat(result.history).hasSize(1)
    assertThat(result.current).isNull()
  }

  @Test
  fun `should throw exception when current domestic status code is invalid`() {
    // Given
    val prisonerNumber = "A1234BC"
    val invalidStatusCode = "INVALID_CODE"

    val currentItem = DomesticStatusDetailsRequest(
      domesticStatusCode = invalidStatusCode,
      createdBy = "USER1",
      createdTime = now(),
    )

    val request = MigratePrisonerDomesticStatusRequest(
      prisonerNumber = prisonerNumber,
      current = currentItem,
    )

    // When
    whenever(referenceCodeRepository.findByGroupCodeAndCode(any(), eq(invalidStatusCode)))
      .thenReturn(null)

    // Then
    val error = assertThrows<EntityNotFoundException> {
      domesticStatusMigrationService.migrateDomesticStatus(request)
    }
    error.message isEqualTo "No reference data found for groupCode: ReferenceCodeGroup.DOMESTIC_STS and code: INVALID_CODE"

    verify(referenceCodeRepository).findByGroupCodeAndCode(any(), eq(invalidStatusCode))
    verify(prisonerDomesticStatusRepository, never()).saveAll<PrisonerDomesticStatus>(any())
  }

  @Test
  fun `should validate all historical status codes in the request`() {
    // Given
    val prisonerNumber = "A1234BC"
    val validCode = "M"
    val invalidCode = "INVALID"

    val historyItems = listOf(
      DomesticStatusDetailsRequest(
        domesticStatusCode = validCode,
        createdBy = "USER1",
        createdTime = now(),
      ),
      DomesticStatusDetailsRequest(
        domesticStatusCode = invalidCode,
        createdBy = "USER1",
        createdTime = now(),
      ),
    )

    val request = MigratePrisonerDomesticStatusRequest(
      prisonerNumber = prisonerNumber,
      history = historyItems,
    )

    // When
    whenever(referenceCodeRepository.findByGroupCodeAndCode(any(), eq(validCode)))
      .thenReturn(referenceData)
    whenever(referenceCodeRepository.findByGroupCodeAndCode(any(), eq(invalidCode)))
      .thenReturn(null)

    // Then
    val error = assertThrows<EntityNotFoundException> {
      domesticStatusMigrationService.migrateDomesticStatus(request)
    }
    error.message isEqualTo "No reference data found for groupCode: ReferenceCodeGroup.DOMESTIC_STS and code: INVALID"

    verify(prisonerDomesticStatusRepository, never()).saveAll<PrisonerDomesticStatus>(any())
  }
}
