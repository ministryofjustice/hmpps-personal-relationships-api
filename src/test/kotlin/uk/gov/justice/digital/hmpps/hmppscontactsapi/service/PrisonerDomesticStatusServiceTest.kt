package uk.gov.justice.digital.hmpps.hmppscontactsapi.service

import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.PrisonerDomesticStatus
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.ReferenceCodeEntity
import uk.gov.justice.digital.hmpps.hmppscontactsapi.helpers.aUser
import uk.gov.justice.digital.hmpps.hmppscontactsapi.helpers.isEqualTo
import uk.gov.justice.digital.hmpps.hmppscontactsapi.helpers.prisoner
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.ReferenceCodeGroup
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.CreateOrUpdatePrisonerDomesticStatusRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.PrisonerDomesticStatusRepository
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.ReferenceCodeRepository
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
class PrisonerDomesticStatusServiceTest {

  @Mock
  private lateinit var prisonerDomesticStatusRepository: PrisonerDomesticStatusRepository

  @Mock
  private lateinit var referenceCodeRepository: ReferenceCodeRepository

  @Mock
  private lateinit var prisonerService: PrisonerService

  @InjectMocks
  private lateinit var prisonerDomesticStatusService: PrisonerDomesticStatusService

  private val prisonerNumber = "A1234BC"

  private val user = aUser("test-user")

  @Nested
  inner class GetDomesticStatusByPrisonerNumber {

    @Test
    fun `should returns correct response when status exists`() {
      // Given
      val domesticStatus = PrisonerDomesticStatus(
        prisonerDomesticStatusId = 1,
        prisonerNumber = prisonerNumber,
        domesticStatusCode = "S",
        active = true,
        createdBy = "USER1",
        createdTime = LocalDateTime.now(),
      )

      whenever(prisonerDomesticStatusRepository.findByPrisonerNumberAndActiveTrue(prisonerNumber))
        .thenReturn(domesticStatus)

      whenever(
        referenceCodeRepository.findByGroupCodeAndCode(
          ReferenceCodeGroup.DOMESTIC_STS,
          "S",
        ),
      ).thenReturn(ReferenceCodeEntity(1L, ReferenceCodeGroup.DOMESTIC_STS, "S", "Single", 0, true, "name"))

      // When
      val result = prisonerDomesticStatusService.getDomesticStatus(prisonerNumber)

      // Then
      with(result) {
        assertThat(prisonerNumber).isEqualTo(prisonerNumber)
        assertThat(domesticStatusCode).isEqualTo("S")
        assertThat(domesticStatusDescription).isEqualTo("Single")
        assertThat(active).isTrue
      }
    }

    @Test
    fun `should throws EntityNotFoundException when status does not exist`() {
      // Given
      whenever(prisonerDomesticStatusRepository.findByPrisonerNumberAndActiveTrue(prisonerNumber))
        .thenReturn(null)

      // When/Then
      assertThrows<EntityNotFoundException> {
        prisonerDomesticStatusService.getDomesticStatus(prisonerNumber)
      }.message isEqualTo ("No domestic status found for prisoner number: $prisonerNumber")

      verify(referenceCodeRepository, times(0)).findByGroupCodeAndCode(any(), any())
    }
  }

  @Nested
  inner class CreateOrUpdateDomesticStatusByPrisonerNumber {

    @Test
    fun `should creates new status when none exists `() {
      // Given
      val request = CreateOrUpdatePrisonerDomesticStatusRequest(
        domesticStatusCode = "S",
      )

      whenever(prisonerDomesticStatusRepository.findByPrisonerNumberAndActiveTrue(prisonerNumber))
        .thenReturn(null)
      whenever(prisonerService.getPrisoner(any())).thenReturn(prisoner("A1234BC", prisonId = "MDI"))
      whenever(
        referenceCodeRepository.findByGroupCodeAndCode(
          ReferenceCodeGroup.DOMESTIC_STS,
          "S",
        ),
      ).thenReturn(ReferenceCodeEntity(1L, ReferenceCodeGroup.DOMESTIC_STS, "S", "Single", 0, true, "name"))

      val newStatus = PrisonerDomesticStatus(
        prisonerDomesticStatusId = 1,
        prisonerNumber = prisonerNumber,
        domesticStatusCode = "S",
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
        user,
      )

      // Then
      with(result) {
        assertThat(prisonerNumber).isEqualTo(prisonerNumber)
        assertThat(domesticStatusCode).isEqualTo("S")
        assertThat(domesticStatusDescription).isEqualTo("Single")
        assertThat(active).isTrue
      }
      verify(prisonerDomesticStatusRepository, times(1)).save(any())
    }

    @Test
    fun `should creates new status with null domestic status `() {
      // Given
      val request = CreateOrUpdatePrisonerDomesticStatusRequest(
        domesticStatusCode = null,
      )
      whenever(prisonerService.getPrisoner(any())).thenReturn(prisoner("A1234BC", prisonId = "MDI"))
      whenever(prisonerDomesticStatusRepository.findByPrisonerNumberAndActiveTrue(prisonerNumber))
        .thenReturn(null)

      val newStatus = PrisonerDomesticStatus(
        prisonerDomesticStatusId = 1,
        prisonerNumber = prisonerNumber,
        domesticStatusCode = null,
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
        user,
      )

      // Then
      with(result) {
        assertThat(prisonerNumber).isEqualTo(prisonerNumber)
        assertThat(domesticStatusCode).isEqualTo(null)
        assertThat(domesticStatusDescription).isEqualTo(null)
        assertThat(active).isTrue
      }
      verify(prisonerDomesticStatusRepository, times(1)).save(any())
    }

    @Test
    fun `should deactivates existing status and creates new one`() {
      // Given

      val existingStatus = PrisonerDomesticStatus(
        prisonerDomesticStatusId = 1,
        prisonerNumber = prisonerNumber,
        domesticStatusCode = "M",
        active = true,
        createdBy = "USER1",
        createdTime = LocalDateTime.now(),
      )

      val request = CreateOrUpdatePrisonerDomesticStatusRequest(
        domesticStatusCode = "D",
      )
      whenever(prisonerService.getPrisoner(any())).thenReturn(prisoner("A1234BC", prisonId = "MDI"))
      whenever(prisonerDomesticStatusRepository.findByPrisonerNumberAndActiveTrue(prisonerNumber))
        .thenReturn(existingStatus)

      whenever(
        referenceCodeRepository.findByGroupCodeAndCode(
          ReferenceCodeGroup.DOMESTIC_STS,
          "D",
        ),
      ).thenReturn(ReferenceCodeEntity(1L, ReferenceCodeGroup.DOMESTIC_STS, "D", "Divorced", 0, true, "name"))

      whenever(prisonerDomesticStatusRepository.save(any()))
        .thenReturn(existingStatus)
        .thenReturn(existingStatus.copy(domesticStatusCode = "D", active = true))

      // When
      val result = prisonerDomesticStatusService.createOrUpdateDomesticStatus(
        prisonerNumber,
        request,
        user,
      )

      val statusCaptor = argumentCaptor<PrisonerDomesticStatus>()
      verify(prisonerDomesticStatusRepository, times(2)).save(statusCaptor.capture())
      assertThat(statusCaptor.firstValue.active).isFalse()
      assertThat(statusCaptor.firstValue.domesticStatusCode).isEqualTo("M")
      assertThat(statusCaptor.secondValue.active).isTrue()
      assertThat(statusCaptor.secondValue.domesticStatusCode).isEqualTo("D")

      // new code is returned
      with(result) {
        assertThat(prisonerNumber).isEqualTo(prisonerNumber)
        assertThat(domesticStatusCode).isEqualTo("D")
        assertThat(domesticStatusDescription).isEqualTo("Divorced")
        assertThat(createdBy).isEqualTo("USER1")
        assertThat(createdTime).isInThePast()
        assertThat(active).isTrue
      }
    }

    @Test
    fun `should throws exception when reference code doesn't exist`() {
      // Given
      val request = CreateOrUpdatePrisonerDomesticStatusRequest(
        domesticStatusCode = "M",
      )
      whenever(prisonerService.getPrisoner(any())).thenReturn(prisoner("A1234BC", prisonId = "MDI"))
      whenever(
        referenceCodeRepository.findByGroupCodeAndCode(
          ReferenceCodeGroup.DOMESTIC_STS,
          "M",
        ),
      ).thenReturn(null)

      // When/Then
      assertThrows<EntityNotFoundException> {
        prisonerDomesticStatusService.createOrUpdateDomesticStatus(
          prisonerNumber,
          request,
          user,
        )
      }.message isEqualTo "No reference data found for groupCode: DOMESTIC_STS and code: M"
    }

    @Test
    fun `should throws exception when prisoner doesn't exist`() {
      // Given
      val request = CreateOrUpdatePrisonerDomesticStatusRequest(
        domesticStatusCode = "M",
      )
      whenever(prisonerService.getPrisoner(any())).thenReturn(null)

      // When/Then
      assertThrows<EntityNotFoundException> {
        prisonerDomesticStatusService.createOrUpdateDomesticStatus(
          prisonerNumber,
          request,
          user,
        )
      }.message isEqualTo "Prisoner number $prisonerNumber - not found"
    }
  }
}
