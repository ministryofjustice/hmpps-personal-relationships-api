package uk.gov.justice.digital.hmpps.personalrelationships.service.sync

import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.never
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.personalrelationships.entity.EmploymentEntity
import uk.gov.justice.digital.hmpps.personalrelationships.mapping.sync.toEntity
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.sync.SyncCreateEmploymentRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.sync.SyncUpdateEmploymentRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.sync.SyncEmployment
import uk.gov.justice.digital.hmpps.personalrelationships.repository.EmploymentRepository
import java.time.LocalDateTime
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class SyncEmploymentServiceTest {

  @Mock
  private lateinit var employmentRepository: EmploymentRepository

  private lateinit var service: SyncEmploymentService

  @BeforeEach
  fun setUp() {
    service = SyncEmploymentService(employmentRepository)
  }

  @Nested
  inner class GetEmploymentById {
    @Test
    fun `should return employment when found`() {
      // Given
      val employmentId = 1L
      val employment = createEmploymentEntity()
      val expectedResponse = createSyncEmployment()

      whenever(employmentRepository.findById(employmentId)).thenReturn(Optional.of(employment))

      // When
      val result = service.getEmploymentById(employmentId)

      // Then
      with(result) {
        assertThat(result.employmentId).isEqualTo(expectedResponse.employmentId)
        assertThat(organisationId).isEqualTo(expectedResponse.organisationId)
        assertThat(contactId).isEqualTo(expectedResponse.contactId)
        assertThat(active).isEqualTo(expectedResponse.active)
        assertThat(createdBy).isEqualTo(expectedResponse.createdBy)
        assertThat(createdTime).isAfter(LocalDateTime.now().minusMinutes(5))
        assertThat(updatedBy).isNull()
        assertThat(updatedTime).isNull()
      }
      verify(employmentRepository).findById(employmentId)
    }

    @Test
    fun `should throw EntityNotFoundException when employment not found`() {
      // Given
      val employmentId = 1L
      whenever(employmentRepository.findById(employmentId)).thenReturn(Optional.empty())

      // When/Then
      val exception = assertThrows<EntityNotFoundException> {
        service.getEmploymentById(employmentId)
      }

      assertThat(exception.message).isEqualTo("Employment with ID $employmentId not found")
      verify(employmentRepository).findById(employmentId)
    }
  }

  @Nested
  inner class CreateEmployment {
    @Test
    fun `should create new employment`() {
      // Given
      val request = createSyncCreateEmploymentRequest()
      val employmentEntity = createEmploymentEntity()
      val createResponse = createSyncEmployment()

      whenever(employmentRepository.saveAndFlush(request.toEntity())).thenReturn(employmentEntity)

      // When
      val result = service.createEmployment(request)

      // Then
      with(result) {
        assertThat(result.employmentId).isEqualTo(createResponse.employmentId)
        assertThat(organisationId).isEqualTo(createResponse.organisationId)
        assertThat(contactId).isEqualTo(createResponse.contactId)
        assertThat(active).isEqualTo(createResponse.active)
        assertThat(createdBy).isEqualTo(createResponse.createdBy)
        assertThat(createdTime).isAfter(LocalDateTime.now().minusMinutes(5))
        assertThat(updatedBy).isNull()
        assertThat(updatedTime).isNull()
      }
      verify(employmentRepository).saveAndFlush(request.toEntity())
    }
  }

  @Nested
  inner class UpdateEmployment {
    @Test
    fun `should update employment when found`() {
      // Given
      val employmentId = 1L
      val existingEmployment = createEmploymentEntity()

      val updateRequest = updateSyncUpdateEmploymentRequest()

      val expectedUpdatedEmployment = existingEmployment.copy(
        employmentId = employmentId,
        organisationId = updateRequest.organisationId,
        contactId = updateRequest.contactId,
        active = updateRequest.active,
        updatedBy = updateRequest.updatedBy,
        updatedTime = updateRequest.updatedTime,
      )

      whenever(employmentRepository.findById(employmentId)).thenReturn(Optional.of(existingEmployment))
      whenever(employmentRepository.saveAndFlush(any())).thenReturn(expectedUpdatedEmployment)

      // When
      val result = service.updateEmployment(employmentId, updateRequest)

      // Then
      verify(employmentRepository).findById(employmentId)

      val employmentEntity = argumentCaptor<EmploymentEntity>()
      verify(employmentRepository).saveAndFlush(employmentEntity.capture())
      val entity = employmentEntity.firstValue

      with(entity) {
        assertThat(entity.employmentId).isEqualTo(employmentId)
        assertThat(organisationId).isEqualTo(expectedUpdatedEmployment.organisationId)
        assertThat(contactId).isEqualTo(expectedUpdatedEmployment.contactId)
        assertThat(active).isEqualTo(expectedUpdatedEmployment.active)
        assertThat(createdBy).isEqualTo(expectedUpdatedEmployment.createdBy)
        assertThat(createdTime).isEqualTo(expectedUpdatedEmployment.createdTime)
        assertThat(updatedBy).isEqualTo(expectedUpdatedEmployment.updatedBy)
        assertThat(updatedTime).isEqualTo(expectedUpdatedEmployment.updatedTime)
      }

      with(result) {
        assertThat(result.employmentId).isEqualTo(employmentId)
        assertThat(organisationId).isEqualTo(updateRequest.organisationId)
        assertThat(contactId).isEqualTo(updateRequest.contactId)
        assertThat(active).isEqualTo(updateRequest.active)
        assertThat(updatedBy).isEqualTo(updateRequest.updatedBy)
        assertThat(updatedTime).isEqualTo(updateRequest.updatedTime)
      }
    }

    @Test
    fun `should throw EntityNotFoundException when employment not found`() {
      // Given
      val employmentId = 1L
      val updateRequest = updateSyncUpdateEmploymentRequest()

      whenever(employmentRepository.findById(employmentId)).thenReturn(Optional.empty())

      // When/Then

      val exception = assertThrows<EntityNotFoundException> {
        service.updateEmployment(employmentId, updateRequest)
      }

      assertThat(exception.message).isEqualTo("Employment with ID $employmentId not found")
      verify(employmentRepository).findById(employmentId)
      verify(employmentRepository, never()).saveAndFlush(any())
    }

    @Test
    fun `should save employment with updated fields`() {
      // Given
      val employmentId = 1L
      val existingEmployment = createEmploymentEntity()

      val updateRequest = updateSyncUpdateEmploymentRequest()

      val expectedUpdatedEmployment = existingEmployment.copy(
        employmentId = employmentId,
        organisationId = updateRequest.organisationId,
        contactId = updateRequest.contactId,
        active = updateRequest.active,
        updatedBy = updateRequest.updatedBy,
        updatedTime = updateRequest.updatedTime,
      )

      whenever(employmentRepository.findById(employmentId)).thenReturn(Optional.of(existingEmployment))
      whenever(employmentRepository.saveAndFlush(any())).thenReturn(expectedUpdatedEmployment)

      // When
      service.updateEmployment(employmentId, updateRequest)

      // Then
      argumentCaptor<EmploymentEntity>().apply {
        verify(employmentRepository).saveAndFlush(capture())
        with(firstValue) {
          assertThat(firstValue.employmentId).isEqualTo(employmentId)
          assertThat(organisationId).isEqualTo(updateRequest.organisationId)
          assertThat(contactId).isEqualTo(updateRequest.contactId)
          assertThat(active).isEqualTo(updateRequest.active)
          assertThat(updatedBy).isEqualTo(updateRequest.updatedBy)
          assertThat(updatedTime).isEqualTo(updateRequest.updatedTime)
        }
      }
    }
  }

  @Nested
  inner class DeleteEmployment {
    @Test
    fun `should delete employment when found`() {
      // Given
      val employmentId = 1L
      val existingEmployment = createEmploymentEntity()

      whenever(employmentRepository.findById(employmentId)).thenReturn(Optional.of(existingEmployment))

      // When
      service.deleteEmployment(employmentId)

      // Then
      verify(employmentRepository).findById(employmentId)
      verify(employmentRepository).delete(existingEmployment)
    }

    @Test
    fun `should throw EntityNotFoundException when employment not found`() {
      // Given
      val employmentId = 1L

      whenever(employmentRepository.findById(employmentId)).thenReturn(Optional.empty())

      // When/Then
      val exception = assertThrows<EntityNotFoundException> {
        service.deleteEmployment(employmentId)
      }

      assertThat(exception.message).isEqualTo("Employment with ID $employmentId not found")

      verify(employmentRepository).findById(employmentId)
      verify(employmentRepository, never()).delete(any())
    }
  }

  private fun createEmploymentEntity() = EmploymentEntity(
    employmentId = 1L,
    organisationId = 2L,
    contactId = 2L,
    active = true,
    createdBy = "CREATOR",
    createdTime = LocalDateTime.now(),
    updatedBy = null,
    updatedTime = null,
  )

  private fun createSyncEmployment() = SyncEmployment(
    employmentId = 1L,
    organisationId = 2L,
    contactId = 2L,
    active = true,
    createdBy = "CREATOR",
    createdTime = LocalDateTime.now(),
    updatedBy = null,
    updatedTime = null,
  )

  private fun createSyncCreateEmploymentRequest() = SyncCreateEmploymentRequest(
    organisationId = 2L,
    contactId = 2L,
    active = true,
    createdBy = "CREATOR",
    createdTime = LocalDateTime.now(),
  )

  private fun updateSyncUpdateEmploymentRequest() = SyncUpdateEmploymentRequest(
    organisationId = 101L,
    contactId = 201L,
    active = false,
    updatedBy = "NEW_USER",
    updatedTime = LocalDateTime.now(),
  )
}
