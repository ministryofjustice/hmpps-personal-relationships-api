package uk.gov.justice.digital.hmpps.hmppscontactsapi.service

import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.check
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.PrisonerDomesticStatus
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.ReferenceCodeEntity
import uk.gov.justice.digital.hmpps.hmppscontactsapi.helpers.isEqualTo
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.ReferenceCodeGroup
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.UpdatePrisonerDomesticStatusRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.PrisonerDomesticStatusRepository
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.ReferenceCodeRepository
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
class PrisonerDomesticStatusServiceTest {

  @Mock
  private lateinit var prisonerDomesticStatusRepository: PrisonerDomesticStatusRepository

  @Mock
  private lateinit var referenceCodeRepository: ReferenceCodeRepository

  @InjectMocks
  private lateinit var prisonerDomesticStatusService: PrisonerDomesticStatusService

  private val prisonerNumber = "A1234BC"

  @Test
  fun `getDomesticStatus returns correct response when status exists`() {
    // Given
    val domesticStatus = PrisonerDomesticStatus(
      prisonerDomesticStatusId = 1,
      prisonerNumber = prisonerNumber,
      domesticStatusCode = "CODE1",
      active = true,
      createdBy = "USER1",
      createdTime = LocalDateTime.now(),
    )

    whenever(prisonerDomesticStatusRepository.findByPrisonerNumberAndActive(prisonerNumber, true))
      .thenReturn(domesticStatus)

    // When
    val result = prisonerDomesticStatusService.getDomesticStatus(prisonerNumber)

    // Then
    with(result) {
      assertThat(prisonerNumber).isEqualTo(prisonerNumber)
      assertThat(domesticStatusValue).isEqualTo("CODE1")
      assertThat(active).isTrue
    }
  }

  @Test
  fun `getDomesticStatus throws EntityNotFoundException when status does not exist`() {
    // Given
    whenever(prisonerDomesticStatusRepository.findByPrisonerNumberAndActive(prisonerNumber, true))
      .thenReturn(null)

    // When/Then
    assertThrows<EntityNotFoundException> {
      prisonerDomesticStatusService.getDomesticStatus(prisonerNumber)
    }.message isEqualTo ("No domestic status found for prisoner number: $prisonerNumber")
  }

  @Test
  fun `createOrUpdateDomesticStatus creates new status when none exists`() {
    // Given
    val request = UpdatePrisonerDomesticStatusRequest(
      prisonerNumber = prisonerNumber,
      domesticStatusCode = "CODE1",
      updatedBy = "USER1",
    )

    whenever(prisonerDomesticStatusRepository.findByPrisonerNumberAndActive(prisonerNumber, true))
      .thenReturn(null)

    whenever(
      referenceCodeRepository.findByGroupCodeAndCode(
        ReferenceCodeGroup.DOMESTIC_STS,
        request.domesticStatusCode,
      ),
    ).thenReturn(ReferenceCodeEntity(1L, ReferenceCodeGroup.DOMESTIC_STS, "S", "Single", 0, true, "name"))

    val newStatus = PrisonerDomesticStatus(
      prisonerDomesticStatusId = 1,
      prisonerNumber = prisonerNumber,
      domesticStatusCode = "CODE1",
      active = true,
      createdBy = "USER1",
      createdTime = LocalDateTime.now(),
    )

    whenever(prisonerDomesticStatusRepository.save(any()))
      .thenReturn(newStatus)

    // When
    val result = prisonerDomesticStatusService.createOrUpdateDomesticStatus(
      prisonerNumber,
      request,
    )

    // Then
    with(result) {
      assertThat(prisonerNumber).isEqualTo(prisonerNumber)
      assertThat(domesticStatusValue).isEqualTo("CODE1")
      assertThat(active).isTrue
    }
    verify(prisonerDomesticStatusRepository, times(1)).save(any())
  }

  @Test
  fun `createOrUpdateDomesticStatus deactivates existing status and creates new one`() {
    // Given
    val existingStatus = PrisonerDomesticStatus(
      prisonerDomesticStatusId = 1,
      prisonerNumber = prisonerNumber,
      domesticStatusCode = "OLD_CODE",
      active = true,
      createdBy = "USER1",
      createdTime = LocalDateTime.now(),
    )

    val request = UpdatePrisonerDomesticStatusRequest(
      prisonerNumber = prisonerNumber,
      domesticStatusCode = "NEW_CODE",
      updatedBy = "USER1",
    )

    whenever(prisonerDomesticStatusRepository.findByPrisonerNumberAndActive(prisonerNumber, true))
      .thenReturn(existingStatus)

    whenever(
      referenceCodeRepository.findByGroupCodeAndCode(
        ReferenceCodeGroup.DOMESTIC_STS,
        request.domesticStatusCode,
      ),
    ).thenReturn(ReferenceCodeEntity(1L, ReferenceCodeGroup.DOMESTIC_STS, "FRIEND", "Friend", 0, true, "name"))

    val newStatus = PrisonerDomesticStatus(
      prisonerDomesticStatusId = 1,
      prisonerNumber = prisonerNumber,
      domesticStatusCode = "NEW_CODE",
      active = true,
      createdBy = "USER1",
      createdTime = LocalDateTime.now(),
    )

    whenever(prisonerDomesticStatusRepository.save(any()))
      .thenReturn(existingStatus).thenReturn(newStatus)

    // When
    val result = prisonerDomesticStatusService.createOrUpdateDomesticStatus(
      prisonerNumber,
      request,
    )

    verify(prisonerDomesticStatusRepository, times(1)).save(
      check { savedStatus ->
        assertThat(savedStatus.active).isFalse()
        assertThat(savedStatus.domesticStatusCode).isEqualTo("OLD_CODE")
      },
    )

    verify(prisonerDomesticStatusRepository, times(1)).save(
      check { savedStatus ->
        assertThat(savedStatus.active).isTrue()
        assertThat(savedStatus.domesticStatusCode).isEqualTo("NEW_CODE")
      },
    )
    // new code is returned
    with(result) {
      assertThat(prisonerNumber).isEqualTo(prisonerNumber)
      assertThat(domesticStatusValue).isEqualTo("NEW_CODE")
      assertThat(active).isTrue
    }
  }

  @Test
  fun `createOrUpdateDomesticStatus throws exception when reference code doesn't exist`() {
    // Given
    val request = UpdatePrisonerDomesticStatusRequest(
      domesticStatusCode = "MARRIED",
      prisonerNumber = prisonerNumber,
      updatedBy = "test-user",
    )

    whenever(
      referenceCodeRepository.findByGroupCodeAndCode(
        ReferenceCodeGroup.DOMESTIC_STS,
        request.domesticStatusCode,
      ),
    ).thenReturn(null)

    // When/Then
    assertThrows<EntityNotFoundException> {
      prisonerDomesticStatusService.createOrUpdateDomesticStatus(
        prisonerNumber,
        request,
      )
    }.message isEqualTo "No reference data found for groupCode: DOMESTIC_STS and code: MARRIED"
  }
}
