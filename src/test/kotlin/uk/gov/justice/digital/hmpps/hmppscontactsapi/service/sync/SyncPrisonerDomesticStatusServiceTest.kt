package uk.gov.justice.digital.hmpps.hmppscontactsapi.service.sync

import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.PrisonerDomesticStatus
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.ReferenceCodeEntity
import uk.gov.justice.digital.hmpps.hmppscontactsapi.helpers.isEqualTo
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.ReferenceCodeGroup
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync.SyncUpdatePrisonerDomesticStatusRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.PrisonerDomesticStatusRepository
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.ReferenceCodeRepository
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
class SyncPrisonerDomesticStatusServiceTest {

  @Mock
  private lateinit var domesticStatusRepository: PrisonerDomesticStatusRepository

  @Mock
  private lateinit var referenceCodeRepository: ReferenceCodeRepository

  @InjectMocks
  private lateinit var syncDomesticStatusService: SyncPrisonerDomesticStatusService

  private val referenceData =
    ReferenceCodeEntity(1L, ReferenceCodeGroup.DOMESTIC_STS, "M", "Married", 0, true, "name")

  @Test
  fun `getDomesticStatusByPrisonerNumber returns status when found`() {
    // Given
    val prisonerNumber = "A1234BC"
    val domesticStatus = PrisonerDomesticStatus(
      prisonerDomesticStatusId = 1L,
      prisonerNumber = prisonerNumber,
      domesticStatusCode = "D",
      createdBy = "user",
      createdTime = LocalDateTime.now(),
      active = true,
    )

    whenever(domesticStatusRepository.findByPrisonerNumberAndActiveTrue(prisonerNumber))
      .thenReturn(domesticStatus)

    // When
    val result = syncDomesticStatusService.getDomesticStatusByPrisonerNumber(prisonerNumber)

    // Then
    assertNotNull(result)
    assertThat(result.id).isEqualTo(domesticStatus.prisonerDomesticStatusId)
    assertThat(result.domesticStatusCode).isEqualTo(domesticStatus.domesticStatusCode)
    assertThat(result.createdBy).isEqualTo(domesticStatus.createdBy)
    assertThat(result.active).isEqualTo(domesticStatus.active)
    assertThat(result.createdTime).isInThePast()

    verify(domesticStatusRepository).findByPrisonerNumberAndActiveTrue(prisonerNumber)
  }

  @Test
  fun `getDomesticStatusByPrisonerNumber throws EntityNotFoundException when not found`() {
    // Given
    val prisonerNumber = "A1234BC"
    whenever(domesticStatusRepository.findByPrisonerNumberAndActiveTrue(prisonerNumber))
      .thenReturn(null)

    // When/Then
    assertThrows<EntityNotFoundException> {
      syncDomesticStatusService.getDomesticStatusByPrisonerNumber(prisonerNumber)
    }.also {
      assertThat(it).hasMessage(
        String.format(SyncPrisonerDomesticStatusService.NOT_FOUND_MESSAGE, prisonerNumber),
      )
    }
  }

  @Test
  fun `createOrUpdateDomesticStatus deactivates existing status and creates new one when existing value is different`() {
    // Given
    val prisonerNumber = "A1234BC"
    val existingStatus = PrisonerDomesticStatus(
      prisonerDomesticStatusId = 1L,
      prisonerNumber = prisonerNumber,
      domesticStatusCode = "M",
      createdBy = "user",
      createdTime = LocalDateTime.now(),
      active = true,
    )

    val updateRequest = SyncUpdatePrisonerDomesticStatusRequest(
      domesticStatusCode = "D",
      createdBy = "user",
      createdTime = LocalDateTime.now(),
    )
    whenever(referenceCodeRepository.findByGroupCodeAndCode(any(), any())).thenReturn(referenceData)
    whenever(domesticStatusRepository.findByPrisonerNumberAndActiveTrue(prisonerNumber))
      .thenReturn(existingStatus)

    val deactivatedStatus = existingStatus.copy(active = false)
    whenever(domesticStatusRepository.save(any())).thenReturn(deactivatedStatus)

    // When
    syncDomesticStatusService.createOrUpdateDomesticStatus(prisonerNumber, updateRequest)

    // Then
    verify(domesticStatusRepository).findByPrisonerNumberAndActiveTrue(prisonerNumber)
  }

  @Test
  fun `createOrUpdateDomesticStatus unchanged existing status when existing value is same`() {
    // Given
    val prisonerNumber = "A1234BC"
    val existingStatus = PrisonerDomesticStatus(
      prisonerDomesticStatusId = 1L,
      prisonerNumber = prisonerNumber,
      domesticStatusCode = "D",
      createdBy = "user",
      createdTime = LocalDateTime.now(),
      active = true,
    )

    val updateRequest = SyncUpdatePrisonerDomesticStatusRequest(
      domesticStatusCode = "D",
      createdBy = "user",
      createdTime = LocalDateTime.now(),
    )
    whenever(referenceCodeRepository.findByGroupCodeAndCode(any(), any())).thenReturn(referenceData)
    whenever(domesticStatusRepository.findByPrisonerNumberAndActiveTrue(prisonerNumber))
      .thenReturn(existingStatus)

    // When
    syncDomesticStatusService.createOrUpdateDomesticStatus(prisonerNumber, updateRequest)

    // Then
    verify(domesticStatusRepository).findByPrisonerNumberAndActiveTrue(prisonerNumber)
    verify(domesticStatusRepository, never()).save(any())
  }

  @Test
  fun `createOrUpdateDomesticStatus creates new status when no existing status found`() {
    // Given
    val prisonerNumber = "A1234BC"
    val updateRequest = SyncUpdatePrisonerDomesticStatusRequest(
      domesticStatusCode = "D",
      createdBy = "user",
      createdTime = LocalDateTime.now(),
    )
    whenever(referenceCodeRepository.findByGroupCodeAndCode(any(), any())).thenReturn(referenceData)
    whenever(domesticStatusRepository.findByPrisonerNumberAndActiveTrue(prisonerNumber))
      .thenReturn(null)

    whenever(domesticStatusRepository.save(any())).thenReturn(
      PrisonerDomesticStatus(
        prisonerNumber = prisonerNumber,
        domesticStatusCode = "D",
        createdBy = "user",
        createdTime = LocalDateTime.now(),
        active = true,
      ),
    )

    // When
    syncDomesticStatusService.createOrUpdateDomesticStatus(prisonerNumber, updateRequest)

    // Then
    verify(domesticStatusRepository).findByPrisonerNumberAndActiveTrue(prisonerNumber)
    val domesticStatusCaptor = argumentCaptor<PrisonerDomesticStatus>()
    verify(domesticStatusRepository, times(1)).save(domesticStatusCaptor.capture())
    val savedDomesticStatus = domesticStatusCaptor.firstValue
    assertThat(savedDomesticStatus.prisonerNumber).isEqualTo(prisonerNumber)
    assertThat(savedDomesticStatus.domesticStatusCode).isEqualTo("D")
    assertThat(savedDomesticStatus.createdBy).isEqualTo("user")
    assertThat(savedDomesticStatus.createdTime).isNotNull()
  }

  @Test
  fun `createOrUpdateDomesticStatus throws exception when invalid status code provided`() {
    // Given
    val prisonerNumber = "A1234BC"
    val updateRequest = SyncUpdatePrisonerDomesticStatusRequest(
      domesticStatusCode = "INVALID",
      createdBy = "user",
      createdTime = LocalDateTime.now(),
    )
    whenever(referenceCodeRepository.findByGroupCodeAndCode(any(), any())).thenReturn(null)
    // When/Then
    val error = assertThrows<EntityNotFoundException> {
      syncDomesticStatusService.createOrUpdateDomesticStatus(prisonerNumber, updateRequest)
    }
    error.message isEqualTo "No reference data found for groupCode: ReferenceCodeGroup.DOMESTIC_STS and code: INVALID"

    verify(referenceCodeRepository).findByGroupCodeAndCode(any(), any())
    verify(domesticStatusRepository, never()).save(any())
  }

  @Test
  fun `should return active existing status`() {
    // Given
    val prisonerNumber = "A1234BC"
    val domesticStatus = PrisonerDomesticStatus(
      prisonerDomesticStatusId = 1L,
      prisonerNumber = prisonerNumber,
      domesticStatusCode = "D",
      createdBy = "user",
      createdTime = LocalDateTime.now(),
      active = true,
    )

    whenever(domesticStatusRepository.findByPrisonerNumberAndActiveTrue(prisonerNumber))
      .thenReturn(domesticStatus)

    // When
    val result = syncDomesticStatusService.getPrisonerDomesticStatusActive(prisonerNumber)

    // Then
    assertThat(result?.prisonerDomesticStatusId).isEqualTo(domesticStatus.prisonerDomesticStatusId)
    verify(domesticStatusRepository).findByPrisonerNumberAndActiveTrue(prisonerNumber)
  }

  @Test
  fun `should not return active existing status`() {
    // Given
    val prisonerNumber = "A1234BC"
    whenever(domesticStatusRepository.findByPrisonerNumberAndActiveTrue(prisonerNumber))
      .thenReturn(null)

    // When
    val result = syncDomesticStatusService.getPrisonerDomesticStatusActive(prisonerNumber)

    // Then
    assertThat(result).isNull()
    verify(domesticStatusRepository).findByPrisonerNumberAndActiveTrue(prisonerNumber)
  }
}
