package uk.gov.justice.digital.hmpps.hmppscontactsapi.service.sync

import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.PrisonerContactEntity
import uk.gov.justice.digital.hmpps.hmppscontactsapi.mapping.sync.toEntity
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync.SyncCreatePrisonerContactRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync.SyncUpdatePrisonerContactRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.PrisonerContactRepository
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class SyncPrisonerContactServiceTest {
  private val prisonerContactRepository: PrisonerContactRepository = mock()
  private val syncService = SyncPrisonerContactService(prisonerContactRepository)

  @Nested
  inner class PrisonerContactTests {
    @Test
    fun `should get a prisoner contact by ID`() {
      whenever(prisonerContactRepository.findById(1L)).thenReturn(Optional.of(contactEntity()))
      val prisonerContact = syncService.getPrisonerContactById(1L)
      with(prisonerContact) {
        assertThat(id).isEqualTo(1L)
        assertThat(contactId).isEqualTo(12345L)
        assertThat(prisonerNumber).isEqualTo("A1234BC")
        assertThat(relationshipType).isEqualTo("Family")
        assertThat(nextOfKin).isTrue
        assertThat(emergencyContact).isFalse
        assertThat(comments).isEqualTo("Updated relationship type to family")
        assertThat(active).isTrue
        assertThat(approvedVisitor).isTrue
        assertThat(currentTerm).isTrue
        assertThat(expiryDate).isEqualTo(LocalDate.of(2025, 12, 31))
        assertThat(createdAtPrison).isEqualTo("LONDN")
        assertThat(createdBy).isEqualTo("TEST")
        assertThat(createdTime).isAfter(LocalDateTime.now().minusMinutes(5))
        assertThat(updatedBy).isEqualTo("adminUser")
        assertThat(updatedTime).isAfter(LocalDateTime.now().minusMinutes(5))
      }
      verify(prisonerContactRepository).findById(1L)
    }

    @Test
    fun `should fail to get a prisoner contact by ID when not found`() {
      whenever(prisonerContactRepository.findById(1L)).thenReturn(Optional.empty())
      assertThrows<EntityNotFoundException> {
        syncService.getPrisonerContactById(1L)
      }
      verify(prisonerContactRepository).findById(1L)
    }

    @Test
    fun `should create a prisoner contact`() {
      val request = createPrisonerContactRequest()
      whenever(prisonerContactRepository.saveAndFlush(request.toEntity())).thenReturn(request.toEntity())

      val contact = syncService.createPrisonerContact(request)
      val contactCaptor = argumentCaptor<PrisonerContactEntity>()

      verify(prisonerContactRepository).saveAndFlush(contactCaptor.capture())

      // Checks the entity saved
      with(contactCaptor.firstValue) {
        assertThat(contactId).isEqualTo(12345L)
        assertThat(prisonerNumber).isEqualTo("A1234BC")
        assertThat(relationshipType).isEqualTo("S")
        assertThat(relationshipToPrisoner).isEqualTo("FRI")
        assertThat(nextOfKin).isTrue
        assertThat(emergencyContact).isFalse
        assertThat(comments).isEqualTo("Updated relationship type to family")
        assertThat(active).isTrue
        assertThat(approvedVisitor).isTrue
        assertThat(currentTerm).isTrue
        assertThat(expiryDate).isEqualTo(LocalDate.of(2025, 12, 31))
        assertThat(createdAtPrison).isEqualTo("LONDN")
        assertThat(createdBy).isEqualTo("adminUser")
        assertThat(createdTime).isAfter(LocalDateTime.now().minusMinutes(5))
        assertThat(updatedBy).isNull()
        assertThat(updatedTime).isNull()
      }

      // Checks the model response
      with(contact) {
        assertThat(contactId).isEqualTo(12345L)
        assertThat(prisonerNumber).isEqualTo("A1234BC")
        assertThat(contactType).isEqualTo("S")
        assertThat(relationshipType).isEqualTo("FRI")
        assertThat(nextOfKin).isTrue
        assertThat(emergencyContact).isFalse
        assertThat(comments).isEqualTo("Updated relationship type to family")
        assertThat(active).isTrue
        assertThat(approvedVisitor).isTrue
        assertThat(currentTerm).isTrue
        assertThat(expiryDate).isEqualTo(LocalDate.of(2025, 12, 31))
        assertThat(createdAtPrison).isEqualTo("LONDN")
        assertThat(createdBy).isEqualTo("adminUser")
        assertThat(createdTime).isAfter(LocalDateTime.now().minusMinutes(5))
        assertThat(updatedBy).isNull()
        assertThat(updatedTime).isNull()
      }
    }

    @Test
    fun `should delete prisoner contact by ID`() {
      whenever(prisonerContactRepository.findById(1L)).thenReturn(Optional.of(contactEntity()))
      syncService.deletePrisonerContact(1L)
      verify(prisonerContactRepository).deleteById(1L)
    }

    @Test
    fun `should fail to delete prisoner contact by ID when not found`() {
      whenever(prisonerContactRepository.findById(1L)).thenReturn(Optional.empty())
      assertThrows<EntityNotFoundException> {
        syncService.deletePrisonerContact(1L)
      }
      verify(prisonerContactRepository).findById(1L)
    }

    @Test
    fun `should update a prisoner contact by ID`() {
      val request = updatePrisonerContactRequest()
      val prisonerContactID = 1L
      whenever(prisonerContactRepository.findById(prisonerContactID)).thenReturn(Optional.of(contactEntity()))
      whenever(prisonerContactRepository.saveAndFlush(any())).thenReturn(request.toEntity())

      val updated = syncService.updatePrisonerContact(prisonerContactID, request)

      val contactCaptor = argumentCaptor<PrisonerContactEntity>()

      verify(prisonerContactRepository).saveAndFlush(contactCaptor.capture())

      // Checks the entity saved
      with(contactCaptor.firstValue) {
        assertThat(prisonerContactId).isEqualTo(prisonerContactID)
        assertThat(contactId).isEqualTo(12345L)
        assertThat(prisonerNumber).isEqualTo("A1234BC")
        assertThat(relationshipType).isEqualTo("O")
        assertThat(relationshipToPrisoner).isEqualTo("LAW")
        assertThat(nextOfKin).isTrue
        assertThat(emergencyContact).isFalse
        assertThat(comments).isEqualTo("Updated prison location")
        assertThat(active).isTrue
        assertThat(approvedVisitor).isTrue
        assertThat(currentTerm).isTrue
        assertThat(expiryDate).isEqualTo(LocalDate.of(2025, 12, 31))
        assertThat(createdAtPrison).isEqualTo("HMP Wales")
        assertThat(createdBy).isEqualTo("TEST")
        assertThat(createdTime).isAfter(LocalDateTime.now().minusMinutes(5))
        assertThat(updatedBy).isEqualTo("adminUser")
        assertThat(updatedTime).isNotNull
      }

      // Checks the model returned
      with(updated) {
        assertThat(contactId).isEqualTo(12345L)
        assertThat(prisonerNumber).isEqualTo("A1234BC")
        assertThat(contactType).isEqualTo("O")
        assertThat(relationshipType).isEqualTo("LAW")
        assertThat(nextOfKin).isTrue
        assertThat(emergencyContact).isFalse
        assertThat(comments).isEqualTo("Updated relationship type to social")
        assertThat(active).isFalse
        assertThat(approvedVisitor).isFalse
        assertThat(currentTerm).isTrue
        assertThat(expiryDate).isEqualTo(LocalDate.of(2025, 12, 31))
        assertThat(createdAtPrison).isEqualTo("HMP Wales")
        assertThat(createdBy).isEqualTo("TEST")
        assertThat(createdTime).isAfter(LocalDateTime.now().minusMinutes(5))
        assertThat(updatedBy).isEqualTo("adminUser")
        assertThat(updatedTime).isNotNull
      }
    }

    @Test
    fun `should fail to update a prisoner contact when prisoner contact is not found`() {
      val updateRequest = updatePrisonerContactRequest()
      whenever(prisonerContactRepository.findById(1L)).thenReturn(Optional.empty())
      assertThrows<EntityNotFoundException> {
        syncService.updatePrisonerContact(1L, updateRequest)
      }
      verify(prisonerContactRepository).findById(1L)
    }
  }

  private fun updatePrisonerContactRequest() = SyncUpdatePrisonerContactRequest(
    contactId = 12345L,
    prisonerNumber = "A1234BC",
    contactType = "O",
    relationshipType = "LAW",
    nextOfKin = true,
    emergencyContact = false,
    comments = "Updated prison location",
    active = true,
    approvedVisitor = true,
    currentTerm = true,
    expiryDate = LocalDate.of(2025, 12, 31),
    createdAtPrison = "HMP Wales",
    updatedBy = "adminUser",
    updatedTime = LocalDateTime.now(),
  )

  private fun createPrisonerContactRequest() = SyncCreatePrisonerContactRequest(
    contactId = 12345L,
    prisonerNumber = "A1234BC",
    contactType = "S",
    relationshipType = "FRI",
    nextOfKin = true,
    emergencyContact = false,
    comments = "Updated relationship type to family",
    active = true,
    approvedVisitor = true,
    currentTerm = true,
    expiryDate = LocalDate.of(2025, 12, 31),
    createdAtPrison = "LONDN",
    createdBy = "adminUser",
    createdTime = LocalDateTime.now(),
  )

  private fun contactEntity() = PrisonerContactEntity(
    prisonerContactId = 1L,
    contactId = 12345L,
    prisonerNumber = "A1234BC",
    relationshipType = "S",
    relationshipToPrisoner = "Family",
    nextOfKin = true,
    emergencyContact = false,
    approvedVisitor = true,
    active = true,
    currentTerm = true,
    comments = "Updated relationship type to family",
    createdBy = "TEST",
    createdTime = LocalDateTime.now(),
  ).also {
    it.approvedBy = "officer456"
    it.approvedTime = LocalDateTime.now()
    it.expiryDate = LocalDate.of(2025, 12, 31)
    it.createdAtPrison = "LONDN"
    it.updatedBy = "adminUser"
    it.updatedTime = LocalDateTime.now()
  }

  private fun SyncUpdatePrisonerContactRequest.toEntity(): PrisonerContactEntity {
    val updatedBy = this.updatedBy
    val updatedTime = this.updatedTime

    return PrisonerContactEntity(
      prisonerContactId = 1L,
      contactId = 12345L,
      prisonerNumber = "A1234BC",
      relationshipType = "O",
      relationshipToPrisoner = "LAW",
      nextOfKin = true,
      emergencyContact = false,
      active = false,
      approvedVisitor = false,
      currentTerm = true,
      comments = "Updated relationship type to social",
      createdBy = "TEST",
      createdTime = LocalDateTime.now(),
    ).also {
      it.approvedBy = "ADMIN"
      it.approvedTime = LocalDateTime.now()
      it.expiryDate = LocalDate.of(2025, 12, 31)
      it.createdAtPrison = "HMP Wales"
      it.updatedBy = updatedBy
      it.updatedTime = updatedTime
    }
  }
}
