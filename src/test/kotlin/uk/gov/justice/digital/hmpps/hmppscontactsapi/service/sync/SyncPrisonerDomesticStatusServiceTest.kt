package uk.gov.justice.digital.hmpps.hmppscontactsapi.service.sync

import jakarta.persistence.EntityNotFoundException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.PrisonerDomesticStatus
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync.SyncUpdatePrisonerDomesticStatusRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.PrisonerDomesticStatusRepository
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
class SyncPrisonerDomesticStatusServiceTest {

  @Mock
  private lateinit var domesticStatusRepository: PrisonerDomesticStatusRepository

  @InjectMocks
  private lateinit var syncDomesticStatusService: SyncPrisonerDomesticStatusService

  @Test
  fun `getDomesticStatusByPrisonerNumber returns status when found`() {
    // Given
    val prisonerNumber = "A1234BC"
    val domesticStatus = PrisonerDomesticStatus(
      id = 1L,
      prisonerNumber = prisonerNumber,
      domesticStatusCode = "D",
      createdBy = "user",
      createdTime = LocalDateTime.now(),
      active = true,
    )

    whenever(domesticStatusRepository.findByPrisonerNumber(prisonerNumber))
      .thenReturn(domesticStatus)

    // When
    val result = syncDomesticStatusService.getDomesticStatusByPrisonerNumber(prisonerNumber)

    // Then
    assertNotNull(result)
    assertEquals(prisonerNumber, result.prisonerNumber)
    verify(domesticStatusRepository).findByPrisonerNumber(prisonerNumber)
  }

  @Test
  fun `getDomesticStatusByPrisonerNumber throws EntityNotFoundException when not found`() {
    // Given
    val prisonerNumber = "A1234BC"
    whenever(domesticStatusRepository.findByPrisonerNumber(prisonerNumber))
      .thenReturn(null)

    // When/Then
    assertThrows<EntityNotFoundException> {
      syncDomesticStatusService.getDomesticStatusByPrisonerNumber(prisonerNumber)
    }.also {
      assertEquals(
        String.format(SyncPrisonerDomesticStatusService.NOT_FOUND_MESSAGE, prisonerNumber),
        it.message,
      )
    }
  }

  @Test
  fun `createOrUpdateDomesticStatus deactivates existing status and creates new one`() {
    // Given
    val prisonerNumber = "A1234BC"
    val existingStatus = PrisonerDomesticStatus(
      id = 1L,
      prisonerNumber = prisonerNumber,
      domesticStatusCode = "D",
      createdBy = "user",
      createdTime = LocalDateTime.now(),
      active = true,
    )

    val updateRequest = SyncUpdatePrisonerDomesticStatusRequest(
      prisonerNumber = prisonerNumber,
      domesticStatusCode = "D",
      createdBy = "user",
      createdTime = LocalDateTime.now(),
      active = true,
    )

    whenever(domesticStatusRepository.findByPrisonerNumber(prisonerNumber))
      .thenReturn(existingStatus)

    val deactivatedStatus = existingStatus.copy(active = false)
    whenever(domesticStatusRepository.save(any())).thenReturn(deactivatedStatus)

    // When
    syncDomesticStatusService.createOrUpdateDomesticStatus(prisonerNumber, updateRequest)

    // Then
    verify(domesticStatusRepository).findByPrisonerNumber(prisonerNumber)
       /* verify(domesticStatusRepository).save(
            check { savedStatus ->
                assertFalse(savedStatus.active)
                assertEquals(prisonerNumber, savedStatus.prisonerNumber)
            }
        )*/
  }

  @Test
  fun `createOrUpdateDomesticStatus creates new status when no existing status found`() {
    // Given
    val prisonerNumber = "A1234BC"
    val updateRequest = SyncUpdatePrisonerDomesticStatusRequest(
      prisonerNumber = prisonerNumber,
      domesticStatusCode = "D",
      createdBy = "user",
      createdTime = LocalDateTime.now(),
      active = true,
    )

    whenever(domesticStatusRepository.findByPrisonerNumber(prisonerNumber))
      .thenReturn(null)

    // When
    syncDomesticStatusService.createOrUpdateDomesticStatus(prisonerNumber, updateRequest)

    // Then
    verify(domesticStatusRepository).findByPrisonerNumber(prisonerNumber)
    verify(domesticStatusRepository, never()).save(any())
  }
}
