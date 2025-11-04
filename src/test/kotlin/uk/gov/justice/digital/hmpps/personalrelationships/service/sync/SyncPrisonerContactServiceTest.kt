package uk.gov.justice.digital.hmpps.personalrelationships.service.sync

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
import uk.gov.justice.digital.hmpps.personalrelationships.entity.PrisonerContactEntity
import uk.gov.justice.digital.hmpps.personalrelationships.mapping.sync.toEntity
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.sync.SyncCreatePrisonerContactRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.sync.SyncUpdatePrisonerContactRequest
import uk.gov.justice.digital.hmpps.personalrelationships.repository.PrisonerContactRepository
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
      val request: SyncUpdatePrisonerContactRequest = updatePrisonerContactRequest()
      val prisonerContactID = 1L
      val contactEntity = contactEntity()
      whenever(prisonerContactRepository.findById(prisonerContactID)).thenReturn(Optional.of(contactEntity))
      whenever(prisonerContactRepository.saveAndFlush(any())).thenAnswer { i -> i.arguments[0] }

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
        assertThat(comments).isEqualTo("Updated prison location")
        assertThat(active).isTrue()
        assertThat(approvedVisitor).isTrue()
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
    fun `should update a prisoner contact and set approved visitor details when approving`() {
      val request = updatePrisonerContactRequest().copy(approvedVisitor = true)
      val prisonerContactID = 1L
      val contactEntity = contactEntity().copy(approvedVisitor = false).also {
        it.approvedBy = null
        it.approvedTime = null
      }
      whenever(prisonerContactRepository.findById(prisonerContactID)).thenReturn(Optional.of(contactEntity))
      whenever(prisonerContactRepository.saveAndFlush(any())).thenAnswer { i -> i.arguments[0] }

      val updated = syncService.updatePrisonerContact(prisonerContactID, request)

      val contactCaptor = argumentCaptor<PrisonerContactEntity>()

      verify(prisonerContactRepository).saveAndFlush(contactCaptor.capture())

      // Checks the entity saved
      with(contactCaptor.firstValue) {
        assertThat(approvedVisitor).isTrue
        assertThat(approvedBy).isEqualTo("adminUser")
        assertThat(approvedTime).isInThePast()
      }

      // Checks the model returned
      with(updated) {
        assertThat(approvedVisitor).isTrue
      }
    }

    @Test
    fun `should update a prisoner contact and clear approved visitor details when unapproving`() {
      val request = updatePrisonerContactRequest().copy(approvedVisitor = false)
      val prisonerContactID = 1L
      val contactEntity = contactEntity().copy(approvedVisitor = true).also {
        it.approvedBy = "officer456"
        it.approvedTime = LocalDateTime.now().minusDays(1)
      }
      whenever(prisonerContactRepository.findById(prisonerContactID)).thenReturn(Optional.of(contactEntity))
      whenever(prisonerContactRepository.saveAndFlush(any())).thenAnswer { i -> i.arguments[0] }

      val updated = syncService.updatePrisonerContact(prisonerContactID, request)

      val contactCaptor = argumentCaptor<PrisonerContactEntity>()

      verify(prisonerContactRepository).saveAndFlush(contactCaptor.capture())

      // Checks the entity saved
      with(contactCaptor.firstValue) {
        assertThat(approvedVisitor).isFalse
        assertThat(approvedBy).isNull()
        assertThat(approvedTime).isNull()
      }

      // Checks the model returned
      with(updated) {
        assertThat(approvedVisitor).isFalse
      }
    }

    @Test
    fun `should update a prisoner contact and do nothing to approved visitor details when both unapproved`() {
      val request = updatePrisonerContactRequest().copy(approvedVisitor = false)
      val prisonerContactID = 1L
      val contactEntity = contactEntity().copy(approvedVisitor = false).also {
        it.approvedBy = null
        it.approvedTime = null
      }
      whenever(prisonerContactRepository.findById(prisonerContactID)).thenReturn(Optional.of(contactEntity))
      whenever(prisonerContactRepository.saveAndFlush(any())).thenAnswer { i -> i.arguments[0] }
      val updated = syncService.updatePrisonerContact(prisonerContactID, request)
      val contactCaptor = argumentCaptor<PrisonerContactEntity>()
      verify(prisonerContactRepository).saveAndFlush(contactCaptor.capture())
      // Checks the entity saved
      with(contactCaptor.firstValue) {
        assertThat(approvedVisitor).isFalse
        assertThat(approvedBy).isNull()
        assertThat(approvedTime).isNull()
      }
      // Checks the model returned
      with(updated) {
        assertThat(approvedVisitor).isFalse
      }
    }

    @Test
    fun `should update a prisoner contact and do nothing to approved visitor details when both approved`() {
      val request = updatePrisonerContactRequest().copy(approvedVisitor = true)
      val prisonerContactID = 1L
      val contactEntity = contactEntity().copy(approvedVisitor = true).also {
        it.approvedBy = "officer456"
        it.approvedTime = LocalDateTime.now().minusDays(1)
      }
      whenever(prisonerContactRepository.findById(prisonerContactID)).thenReturn(Optional.of(contactEntity))
      whenever(prisonerContactRepository.saveAndFlush(any())).thenAnswer { i -> i.arguments[0] }

      val updated = syncService.updatePrisonerContact(prisonerContactID, request)

      val contactCaptor = argumentCaptor<PrisonerContactEntity>()

      verify(prisonerContactRepository).saveAndFlush(contactCaptor.capture())

      // Checks the entity saved
      with(contactCaptor.firstValue) {
        assertThat(approvedVisitor).isTrue
        assertThat(approvedBy).isEqualTo("officer456")
        assertThat(approvedTime).isEqualTo(contactEntity.approvedTime)
      }

      // Checks the model returned
      with(updated) {
        assertThat(approvedVisitor).isTrue
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
}
