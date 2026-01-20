package uk.gov.justice.digital.hmpps.personalrelationships.service.sync

import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.personalrelationships.entity.PrisonerContactEntity
import uk.gov.justice.digital.hmpps.personalrelationships.entity.PrisonerContactRestrictionEntity
import uk.gov.justice.digital.hmpps.personalrelationships.mapping.sync.toEntity
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.sync.SyncCreatePrisonerContactRestrictionRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.sync.SyncUpdatePrisonerContactRestrictionRequest
import uk.gov.justice.digital.hmpps.personalrelationships.repository.PrisonerContactRepository
import uk.gov.justice.digital.hmpps.personalrelationships.repository.PrisonerContactRestrictionRepository
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Optional

class SyncPrisonerContactRestrictionEntityServiceTest {
  private val prisonerContactRestrictionRepository: PrisonerContactRestrictionRepository = mock()
  private val prisonerContactRepository: PrisonerContactRepository = mock()

  private val syncService = SyncPrisonerContactRestrictionService(
    prisonerContactRestrictionRepository,
    prisonerContactRepository,
  )

  @Nested
  inner class PrisonerContactRestrictionEntityTests {
    @Test
    fun `should get a prisoner contact by ID`() {
      whenever(prisonerContactRestrictionRepository.findById(3L))
        .thenReturn(Optional.of(prisonerContactRestrictionEntity()))

      whenever(prisonerContactRepository.findById(2L))
        .thenReturn(Optional.of(prisonerContactEntity()))

      val prisonerContactRestriction = syncService.getPrisonerContactRestrictionById(3L)

      with(prisonerContactRestriction) {
        assertThat(prisonerContactRestrictionId).isEqualTo(3L)
        assertThat(prisonerContactId).isEqualTo(2L)
        assertThat(contactId).isEqualTo(1L)
        assertThat(prisonerNumber).isEqualTo("A1234AA")
        assertThat(restrictionType).isEqualTo("NONCON")
        assertThat(startDate).isEqualTo(LocalDate.of(2024, 1, 1))
        assertThat(expiryDate).isEqualTo(LocalDate.of(2024, 12, 31))
        assertThat(comments).isEqualTo("Restriction due to ongoing investigation")
        assertThat(createdBy).isEqualTo("admin")
        assertThat(createdTime).isAfter(LocalDateTime.now().minusMinutes(5))
        assertThat(updatedBy).isEqualTo("editor")
        assertThat(updatedTime).isAfter(LocalDateTime.now().minusMinutes(5))
      }

      verify(prisonerContactRestrictionRepository).findById(3L)
      verify(prisonerContactRepository).findById(2L)
    }

    @Test
    fun `should fail to get a prisoner contact restriction by ID when not found`() {
      whenever(prisonerContactRestrictionRepository.findById(3L))
        .thenReturn(Optional.empty())

      whenever(prisonerContactRepository.findById(2L))
        .thenReturn(Optional.of(prisonerContactEntity()))

      assertThrows<EntityNotFoundException> {
        syncService.getPrisonerContactRestrictionById(3L)
      }

      verify(prisonerContactRestrictionRepository).findById(3L)
    }

    @Test
    fun `should create a prisoner contact restriction`() {
      val request = createPrisonerContactRestrictionRequest()

      whenever(prisonerContactRestrictionRepository.saveAndFlush(request.toEntity()))
        .thenReturn(prisonerContactRestrictionEntity(null, null))

      whenever(prisonerContactRepository.findById(2L))
        .thenReturn(Optional.of(prisonerContactEntity()))

      val contact = syncService.createPrisonerContactRestriction(request)

      val contactCaptor = argumentCaptor<PrisonerContactRestrictionEntity>()

      verify(prisonerContactRestrictionRepository).saveAndFlush(contactCaptor.capture())

      with(contactCaptor.firstValue) {
        assertThat(prisonerContactRestrictionId).isEqualTo(0L)
        assertThat(prisonerContactId).isEqualTo(2L)
        assertThat(restrictionType).isEqualTo("NONCON")
        assertThat(startDate).isEqualTo(LocalDate.of(2024, 1, 1))
        assertThat(expiryDate).isEqualTo(LocalDate.of(2024, 12, 31))
        assertThat(comments).isEqualTo("Restriction due to ongoing investigation")
        assertThat(createdBy).isEqualTo("admin")
        assertThat(createdTime).isAfter(LocalDateTime.now().minusMinutes(5))
        assertThat(updatedBy).isNull()
        assertThat(updatedTime).isNull()
      }

      with(contact) {
        assertThat(prisonerContactRestrictionId).isGreaterThan(0)
        assertThat(prisonerContactId).isEqualTo(2L)
        assertThat(prisonerNumber).isEqualTo("A1234AA")
        assertThat(contactId).isEqualTo(1L)
        assertThat(restrictionType).isEqualTo("NONCON")
        assertThat(startDate).isEqualTo(LocalDate.of(2024, 1, 1))
        assertThat(expiryDate).isEqualTo(LocalDate.of(2024, 12, 31))
        assertThat(comments).isEqualTo("Restriction due to ongoing investigation")
        assertThat(createdBy).isEqualTo("admin")
        assertThat(createdTime).isAfter(LocalDateTime.now().minusMinutes(5))
        assertThat(updatedBy).isNull()
        assertThat(updatedTime).isNull()
      }
    }

    @Test
    fun `should delete prisoner contact restriction by ID`() {
      whenever(prisonerContactRestrictionRepository.findById(3L))
        .thenReturn(Optional.of(prisonerContactRestrictionEntity()))

      whenever(prisonerContactRepository.findById(2L))
        .thenReturn(Optional.of(prisonerContactEntity()))

      syncService.deletePrisonerContactRestriction(3L)

      verify(prisonerContactRestrictionRepository).deleteById(3L)
      verify(prisonerContactRepository).findById(2L)
    }

    @Test
    fun `should fail to delete prisoner contact restriction by ID when not found`() {
      whenever(prisonerContactRestrictionRepository.findById(3L))
        .thenReturn(Optional.empty())

      whenever(prisonerContactRepository.findById(2L))
        .thenReturn(Optional.of(prisonerContactEntity()))

      assertThrows<EntityNotFoundException> {
        syncService.deletePrisonerContactRestriction(3L)
      }

      verify(prisonerContactRestrictionRepository).findById(3L)
      verify(prisonerContactRepository, never()).findById(2L)
    }

    @Test
    fun `should update a prisoner contact restriction by ID`() {
      val request = updatePrisonerContactRestrictionRequest()
      val prisonerContactRestrictionId = 3L

      whenever(prisonerContactRestrictionRepository.findById(prisonerContactRestrictionId))
        .thenReturn(Optional.of(prisonerContactRestrictionEntity()))

      whenever(prisonerContactRepository.findById(2L))
        .thenReturn(Optional.of(prisonerContactEntity()))

      whenever(prisonerContactRestrictionRepository.saveAndFlush(any()))
        .thenReturn(request.toEntity())

      val updated = syncService.updatePrisonerContactRestriction(prisonerContactRestrictionId, request)

      val contactCaptor = argumentCaptor<PrisonerContactRestrictionEntity>()

      verify(prisonerContactRestrictionRepository).saveAndFlush(contactCaptor.capture())
      verify(prisonerContactRepository).findById(2L)

      with(contactCaptor.firstValue) {
        assertThat(prisonerContactRestrictionId).isEqualTo(3L)
        assertThat(prisonerContactId).isEqualTo(2L)
        assertThat(restrictionType).isEqualTo("NONCON")
        assertThat(startDate).isEqualTo(LocalDate.of(2024, 1, 1))
        assertThat(expiryDate).isEqualTo(LocalDate.of(2024, 12, 31))
        assertThat(comments).isEqualTo("Restriction due to ongoing investigation")
        assertThat(createdBy).isEqualTo("admin")
        assertThat(createdTime).isAfter(LocalDateTime.now().minusMinutes(5))
        assertThat(updatedBy).isEqualTo("editor")
        assertThat(updatedTime).isAfter(LocalDateTime.now().minusMinutes(5))
      }

      with(updated) {
        assertThat(prisonerContactRestrictionId).isEqualTo(3L)
        assertThat(prisonerContactId).isEqualTo(2L)
        assertThat(contactId).isEqualTo(1L)
        assertThat(prisonerNumber).isEqualTo("A1234AA")
        assertThat(restrictionType).isEqualTo("NONCON")
        assertThat(startDate).isEqualTo(LocalDate.of(2024, 1, 1))
        assertThat(expiryDate).isEqualTo(LocalDate.of(2024, 12, 31))
        assertThat(comments).isEqualTo("Restriction due to ongoing investigation")
        assertThat(createdBy).isEqualTo("admin")
        assertThat(createdTime).isAfter(LocalDateTime.now().minusMinutes(5))
        assertThat(updatedBy).isEqualTo("editor")
        assertThat(updatedTime).isAfter(LocalDateTime.now().minusMinutes(5))
      }
    }

    @Test
    fun `should fail to update a prisoner contact restriction when prisoner contact restriction is not found`() {
      val updateRequest = updatePrisonerContactRestrictionRequest()

      whenever(prisonerContactRestrictionRepository.findById(3L)).thenReturn(Optional.empty())

      whenever(prisonerContactRepository.findById(2L))
        .thenReturn(Optional.of(prisonerContactEntity()))

      assertThrows<EntityNotFoundException> {
        syncService.updatePrisonerContactRestriction(3L, updateRequest)
      }

      verify(prisonerContactRestrictionRepository).findById(3L)
      verify(prisonerContactRepository, never()).findById(2L)
    }
  }

  private fun updatePrisonerContactRestrictionRequest() = SyncUpdatePrisonerContactRestrictionRequest(
    restrictionType = "NONCON",
    startDate = LocalDate.of(2024, 1, 1),
    expiryDate = LocalDate.of(2024, 12, 31),
    comments = "Restriction due to ongoing investigation",
    updatedBy = "editor",
    updatedTime = LocalDateTime.now(),
  )

  private fun createPrisonerContactRestrictionRequest() = SyncCreatePrisonerContactRestrictionRequest(
    prisonerContactId = 2L,
    restrictionType = "NONCON",
    startDate = LocalDate.of(2024, 1, 1),
    expiryDate = LocalDate.of(2024, 12, 31),
    comments = "Restriction due to ongoing investigation",
    createdBy = "admin",
    createdTime = LocalDateTime.now(),
  )

  private fun prisonerContactRestrictionEntity(
    updatedBy: String? = "editor",
    updatedTime: LocalDateTime? = LocalDateTime.now(),
  ) = PrisonerContactRestrictionEntity(
    prisonerContactRestrictionId = 3L,
    prisonerContactId = 2L,
    restrictionType = "NONCON",
    startDate = LocalDate.of(2024, 1, 1),
    expiryDate = LocalDate.of(2024, 12, 31),
    comments = "Restriction due to ongoing investigation",
    createdBy = "admin",
    createdTime = LocalDateTime.now(),
    updatedBy = updatedBy,
    updatedTime = updatedTime,
  )

  private fun prisonerContactEntity() = PrisonerContactEntity(
    prisonerContactId = 2L,
    contactId = 1L,
    prisonerNumber = "A1234AA",
    relationshipType = "S",
    active = true,
    currentTerm = true,
    approvedVisitor = true,
    nextOfKin = true,
    emergencyContact = true,
    relationshipToPrisoner = "MOT",
    comments = "Restriction due to ongoing investigation",
    createdBy = "admin",
    createdTime = LocalDateTime.now(),
  )

  private fun SyncUpdatePrisonerContactRestrictionRequest.toEntity(): PrisonerContactRestrictionEntity {
    val updatedBy = this.updatedBy
    val updatedTime = this.updatedTime

    return PrisonerContactRestrictionEntity(
      prisonerContactRestrictionId = 3L,
      prisonerContactId = 2L,
      restrictionType = "NONCON",
      startDate = LocalDate.of(2024, 1, 1),
      expiryDate = LocalDate.of(2024, 12, 31),
      comments = "Restriction due to ongoing investigation",
      createdBy = "admin",
      createdTime = LocalDateTime.now(),
      updatedBy = updatedBy,
      updatedTime = updatedTime,
    )
  }
}
