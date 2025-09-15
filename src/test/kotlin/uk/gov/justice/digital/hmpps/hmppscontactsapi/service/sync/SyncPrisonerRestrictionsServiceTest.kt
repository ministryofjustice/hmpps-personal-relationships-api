package uk.gov.justice.digital.hmpps.hmppscontactsapi.service.sync

import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.doNothing
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.PrisonerRestriction
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.ReferenceCodeEntity
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.ReferenceCodeGroup
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync.SyncCreatePrisonerRestrictionRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync.SyncUpdatePrisonerRestrictionRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.sync.SyncPrisonerRestriction
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.PrisonerRestrictionsRepository
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.ReferenceCodeRepository
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Optional

class SyncPrisonerRestrictionsServiceTest {

  private var prisonerRestrictionsRepository: PrisonerRestrictionsRepository = mock()
  private var referenceCodeRepository: ReferenceCodeRepository = mock()
  private var service: SyncPrisonerRestrictionsService =
    SyncPrisonerRestrictionsService(prisonerRestrictionsRepository, referenceCodeRepository)
  private val referenceCode = ReferenceCodeEntity(1L, ReferenceCodeGroup.RESTRICTION, "CCTV", "CCTV", 0, true, "name")

  @Test
  fun `should get prisoner restriction by id`() {
    val entity = createPrisonerRestrictionEntity()
    whenever(prisonerRestrictionsRepository.findById(1L)).thenReturn(Optional.of(entity))

    val result = service.getPrisonerRestrictionById(1L)

    assertThat(result).usingRecursiveComparison().isEqualTo(mockPrisonerRestrictionResponse())
  }

  @Test
  fun `should throw when prisoner restriction not found`() {
    whenever(prisonerRestrictionsRepository.findById(1L)).thenReturn(Optional.empty())

    assertThatThrownBy { service.getPrisonerRestrictionById(1L) }
      .isInstanceOf(EntityNotFoundException::class.java)
      .hasMessageContaining("PrisonerRestriction with ID 1 not found")
  }

  @Test
  fun `should delete prisoner restriction`() {
    val entity = createPrisonerRestrictionEntity()
    whenever(prisonerRestrictionsRepository.findById(1L)).thenReturn(Optional.of(entity))
    doNothing().whenever(prisonerRestrictionsRepository).deleteById(1L)

    val response = service.deletePrisonerRestriction(1L)

    verify(prisonerRestrictionsRepository).deleteById(1L)
    assertThat(response).usingRecursiveComparison().isEqualTo(mockPrisonerRestrictionResponse())
  }

  @Test
  fun `should throw when deleting non-existent prisoner restriction`() {
    whenever(prisonerRestrictionsRepository.findById(1L)).thenReturn(Optional.empty())

    assertThatThrownBy { service.deletePrisonerRestriction(1L) }
      .isInstanceOf(EntityNotFoundException::class.java)
      .hasMessageContaining("PrisonerRestriction with ID 1 not found")
  }

  @Test
  fun `should create prisoner restriction`() {
    val request = syncCreatePrisonerRestrictionRequest()
    val entity = createPrisonerRestrictionEntity()

    whenever(referenceCodeRepository.findByGroupCodeAndCode(ReferenceCodeGroup.RESTRICTION, "CCTV")).thenReturn(
      referenceCode,
    )
    whenever(prisonerRestrictionsRepository.existsById(any())).thenReturn(false)
    val captor = ArgumentCaptor.forClass(PrisonerRestriction::class.java)
    whenever(prisonerRestrictionsRepository.saveAndFlush(captor.capture())).thenReturn(entity)

    val result = service.createPrisonerRestriction(request)

    assertThat(captor.value).usingRecursiveComparison().ignoringFields("prisonerRestrictionId").isEqualTo(entity)
    assertThat(result).usingRecursiveComparison().isEqualTo(mockPrisonerRestrictionResponse())
  }

  @Test
  fun `should throw when creating prisoner restriction with invalid restriction type`() {
    val request = syncCreatePrisonerRestrictionRequest().copy(restrictionType = "INVALID")
    whenever(referenceCodeRepository.findByGroupCodeAndCode(ReferenceCodeGroup.RESTRICTION, "INVALID")).thenReturn(
      null,
    )

    assertThatThrownBy { service.createPrisonerRestriction(request) }
      .isInstanceOf(EntityNotFoundException::class.java)
      .hasMessageContaining("No reference data found for groupCode: ReferenceCodeGroup.RESTRICTION and code: INVALID")
  }

  @Test
  fun `should update prisoner restriction`() {
    val entity = createPrisonerRestrictionEntity()
    val request = syncUpdatePrisonerRestrictionRequest().copy(commentText = "Updated comment")
    val updatedEntity = entity.copy(commentText = "Updated comment")
    whenever(referenceCodeRepository.findByGroupCodeAndCode(ReferenceCodeGroup.RESTRICTION, "CCTV")).thenReturn(
      referenceCode,
    )
    whenever(prisonerRestrictionsRepository.findById(1L)).thenReturn(Optional.of(entity))
    whenever(prisonerRestrictionsRepository.saveAndFlush(any())).thenReturn(updatedEntity)

    val result = service.updatePrisonerRestriction(1L, request)

    assertThat(result).usingRecursiveComparison().ignoringFields("commentText").isEqualTo(mockPrisonerRestrictionResponse())
    assertThat(result.commentText).isEqualTo("Updated comment")
  }

  @Test
  fun `should throw when updating prisoner restriction with mismatched prisoner number`() {
    val entity = createPrisonerRestrictionEntity()
    val request = syncUpdatePrisonerRestrictionRequest().copy(prisonerNumber = "DIFFERENT123")
    whenever(prisonerRestrictionsRepository.findById(1L)).thenReturn(Optional.of(entity))
    verify(referenceCodeRepository, never()).findByGroupCodeAndCode(any(), any())
    verify(prisonerRestrictionsRepository, never()).saveAndFlush(any())

    assertThatThrownBy { service.updatePrisonerRestriction(1L, request) }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessageContaining("Prisoner number in request does not match existing prisoner restriction")
  }

  @Test
  fun `should throw when updating prisoner restriction with invalid restriction type`() {
    val entity = createPrisonerRestrictionEntity()
    val request = syncUpdatePrisonerRestrictionRequest().copy(restrictionType = "INVALID")
    whenever(prisonerRestrictionsRepository.findById(1L)).thenReturn(Optional.of(entity))
    whenever(referenceCodeRepository.findByGroupCodeAndCode(ReferenceCodeGroup.RESTRICTION, "INVALID")).thenReturn(
      null,
    )

    assertThatThrownBy { service.updatePrisonerRestriction(1L, request) }
      .isInstanceOf(EntityNotFoundException::class.java)
      .hasMessageContaining("No reference data found for groupCode: ReferenceCodeGroup.RESTRICTION and code: INVALID")
  }

  private fun syncCreatePrisonerRestrictionRequest() = SyncCreatePrisonerRestrictionRequest(
    prisonerNumber = "A1234BC",
    restrictionType = "CCTV",
    effectiveDate = LocalDate.of(2024, 6, 11),
    expiryDate = LocalDate.of(2024, 12, 31),
    commentText = "No visits allowed",
    authorisedUsername = "JSMITH",
    currentTerm = true,
    createdBy = "JSMITH_ADM",
    createdTime = LocalDateTime.of(2024, 6, 11, 10, 0),
  )

  private fun syncUpdatePrisonerRestrictionRequest() = SyncUpdatePrisonerRestrictionRequest(
    prisonerNumber = "A1234BC",
    restrictionType = "CCTV",
    effectiveDate = LocalDate.of(2024, 6, 11),
    expiryDate = LocalDate.of(2024, 12, 31),
    commentText = "No visits allowed",
    authorisedUsername = "JSMITH",
    currentTerm = true,
    updatedBy = "PRISON_ADM",
    updatedTime = null,
  )

  private fun createPrisonerRestrictionEntity() = PrisonerRestriction(
    prisonerRestrictionId = 1L,
    prisonerNumber = "A1234BC",
    restrictionType = "CCTV",
    effectiveDate = LocalDate.of(2024, 6, 11),
    expiryDate = LocalDate.of(2024, 12, 31),
    commentText = "No visits allowed",
    authorisedUsername = "JSMITH",
    createdBy = "JSMITH_ADM",
    createdTime = LocalDateTime.of(2024, 6, 11, 10, 0),
    currentTerm = true,
    updatedBy = null,
    updatedTime = null,
  )

  private fun mockPrisonerRestrictionResponse() = SyncPrisonerRestriction(
    prisonerRestrictionId = 1L,
    prisonerNumber = "A1234BC",
    restrictionType = "CCTV",
    effectiveDate = LocalDate.of(2024, 6, 11),
    expiryDate = LocalDate.of(2024, 12, 31),
    commentText = "No visits allowed",
    authorisedUsername = "JSMITH",
    createdBy = "JSMITH_ADM",
    createdTime = LocalDateTime.of(2024, 6, 11, 10, 0),
    currentTerm = true,
    updatedBy = null,
    updatedTime = null,
  )
}
