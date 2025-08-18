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
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.ContactEntity
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.ContactRestrictionEntity
import uk.gov.justice.digital.hmpps.hmppscontactsapi.mapping.sync.toEntity
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync.SyncCreateContactRestrictionRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync.SyncUpdateContactRestrictionRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.ContactRepository
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.ContactRestrictionRepository
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class SyncContactRestrictionServiceTest {
  private val contactRepository: ContactRepository = mock()
  private val contactRestrictionRepository: ContactRestrictionRepository = mock()
  private val syncService = SyncContactRestrictionService(contactRepository, contactRestrictionRepository)

  @Nested
  inner class ContactRestrictionTests {
    @Test
    fun `should get a contact restriction by ID`() {
      whenever(contactRestrictionRepository.findById(1L)).thenReturn(Optional.of(contactRestrictionEntity()))
      val contactRestriction = syncService.getContactRestrictionById(1L)
      with(contactRestriction) {
        assertThat(restrictionType).isEqualTo("DRIVING")
        assertThat(startDate).isEqualTo(LocalDate.of(1980, 2, 1))
        assertThat(expiryDate).isEqualTo(LocalDate.of(2025, 2, 1))
        assertThat(comments).isEqualTo("N/A")
      }
      verify(contactRestrictionRepository).findById(1L)
    }

    @Test
    fun `should fail to get a contact restriction by ID when not found`() {
      whenever(contactRestrictionRepository.findById(1L)).thenReturn(Optional.empty())
      assertThrows<EntityNotFoundException> {
        syncService.getContactRestrictionById(1L)
      }
      verify(contactRestrictionRepository).findById(1L)
    }

    @Test
    fun `should create a contact restriction`() {
      val request = createContactRestrictionRequest()
      whenever(contactRepository.findById(1L)).thenReturn(Optional.of(contactEntity()))
      whenever(contactRestrictionRepository.saveAndFlush(request.toEntity())).thenReturn(request.toEntity())

      val contactRestriction = syncService.createContactRestriction(request)
      val restrictionCaptor = argumentCaptor<ContactRestrictionEntity>()

      verify(contactRestrictionRepository).saveAndFlush(restrictionCaptor.capture())

      // Checks the entity saved
      with(restrictionCaptor.firstValue) {
        assertThat(restrictionType).isEqualTo(request.restrictionType)
        assertThat(startDate).isEqualTo(request.startDate)
        assertThat(expiryDate).isEqualTo(request.expiryDate)
        assertThat(createdBy).isEqualTo(request.createdBy)
      }

      // Checks the model response
      with(contactRestriction) {
        assertThat(contactRestrictionId).isEqualTo(0L)
        assertThat(restrictionType).isEqualTo(request.restrictionType)
        assertThat(startDate).isEqualTo(request.startDate)
        assertThat(expiryDate).isEqualTo(request.expiryDate)
        assertThat(comments).isEqualTo(request.comments)
        assertThat(createdBy).isEqualTo(request.createdBy)
      }

      verify(contactRepository).findById(1L)
    }

    @Test
    fun `should not error when creating a contact restriction with expiry date is before start date`() {
      val request = createContactRestrictionRequest(
        startDate = LocalDate.of(2025, 2, 1),
        expiryDate = LocalDate.of(1980, 2, 1),
      )
      whenever(contactRepository.findById(1L)).thenReturn(Optional.of(contactEntity()))
      whenever(contactRestrictionRepository.saveAndFlush(request.toEntity())).thenReturn(request.toEntity())

      val contactRestriction = syncService.createContactRestriction(request)
      val restrictionCaptor = argumentCaptor<ContactRestrictionEntity>()

      verify(contactRestrictionRepository).saveAndFlush(restrictionCaptor.capture())

      // Checks the entity saved
      with(restrictionCaptor.firstValue) {
        assertThat(restrictionType).isEqualTo(request.restrictionType)
        assertThat(startDate).isEqualTo(request.startDate)
        assertThat(expiryDate).isEqualTo(request.expiryDate)
        assertThat(createdBy).isEqualTo(request.createdBy)
      }

      // Checks the model response
      with(contactRestriction) {
        assertThat(contactRestrictionId).isEqualTo(0L)
        assertThat(restrictionType).isEqualTo(request.restrictionType)
        assertThat(startDate).isEqualTo(request.startDate)
        assertThat(expiryDate).isEqualTo(request.expiryDate)
        assertThat(comments).isEqualTo(request.comments)
        assertThat(createdBy).isEqualTo(request.createdBy)
      }
    }

    @Test
    fun `should fail to create a contact restriction when the contact ID is not present`() {
      val request = createContactRestrictionRequest()
      whenever(contactRepository.findById(1L)).thenReturn(Optional.empty())
      assertThrows<EntityNotFoundException> {
        syncService.createContactRestriction(request)
      }
      verifyNoInteractions(contactRestrictionRepository)
    }

    @Test
    fun `should delete contact restriction by ID`() {
      whenever(contactRestrictionRepository.findById(1L)).thenReturn(Optional.of(contactRestrictionEntity()))
      syncService.deleteContactRestriction(1L)
      verify(contactRestrictionRepository).deleteById(1L)
    }

    @Test
    fun `should fail to delete contact restriction by ID when not found`() {
      whenever(contactRestrictionRepository.findById(1L)).thenReturn(Optional.empty())
      assertThrows<EntityNotFoundException> {
        syncService.deleteContactRestriction(1L)
      }
      verify(contactRestrictionRepository).findById(1L)
    }

    @Test
    fun `should update a contact restriction by ID`() {
      val request = updateContactRestrictionRequest()
      whenever(contactRepository.findById(1L)).thenReturn(Optional.of(contactEntity()))
      whenever(contactRestrictionRepository.findById(1L)).thenReturn(Optional.of(request.toEntity()))
      whenever(contactRestrictionRepository.saveAndFlush(any())).thenReturn(request.toEntity())

      val updated = syncService.updateContactRestriction(1L, request)

      val restrictionCaptor = argumentCaptor<ContactRestrictionEntity>()

      verify(contactRestrictionRepository).saveAndFlush(restrictionCaptor.capture())

      // Checks the entity saved
      with(restrictionCaptor.firstValue) {
        assertThat(restrictionType).isEqualTo(request.restrictionType)
        assertThat(startDate).isEqualTo(request.startDate)
        assertThat(expiryDate).isEqualTo(request.expiryDate)
        assertThat(comments).isEqualTo(request.comments)
        assertThat(updatedBy).isEqualTo(request.updatedBy)
        assertThat(updatedTime).isEqualTo(request.updatedTime)
      }

      // Checks the model returned
      with(updated) {
        assertThat(restrictionType).isEqualTo(request.restrictionType)
        assertThat(startDate).isEqualTo(request.startDate)
        assertThat(expiryDate).isEqualTo(request.expiryDate)
        assertThat(comments).isEqualTo(request.comments)
        assertThat(updatedBy).isEqualTo(request.updatedBy)
        assertThat(updatedTime).isEqualTo(request.updatedTime)
      }
    }

    @Test
    fun `should not error when updating a contact restriction with expiry date is before start date`() {
      val updateRequest = updateContactRestrictionRequest(
        startDate = LocalDate.of(2025, 2, 1),
        expiryDate = LocalDate.of(1980, 2, 1),
      )
      whenever(contactRepository.findById(1L)).thenReturn(Optional.of(contactEntity()))
      whenever(contactRestrictionRepository.findById(1L)).thenReturn(Optional.of(updateRequest.toEntity()))
      whenever(contactRestrictionRepository.saveAndFlush(any())).thenReturn(updateRequest.toEntity())

      val updated = syncService.updateContactRestriction(1L, updateRequest)

      val restrictionCaptor = argumentCaptor<ContactRestrictionEntity>()

      verify(contactRestrictionRepository).saveAndFlush(restrictionCaptor.capture())

      // Checks the entity saved
      with(restrictionCaptor.firstValue) {
        assertThat(restrictionType).isEqualTo(updateRequest.restrictionType)
        assertThat(startDate).isEqualTo(updateRequest.startDate)
        assertThat(expiryDate).isEqualTo(updateRequest.expiryDate)
        assertThat(comments).isEqualTo(updateRequest.comments)
        assertThat(updatedBy).isEqualTo(updateRequest.updatedBy)
        assertThat(updatedTime).isEqualTo(updateRequest.updatedTime)
      }

      // Checks the model returned
      with(updated) {
        assertThat(restrictionType).isEqualTo(updateRequest.restrictionType)
        assertThat(startDate).isEqualTo(updateRequest.startDate)
        assertThat(expiryDate).isEqualTo(updateRequest.expiryDate)
        assertThat(comments).isEqualTo(updateRequest.comments)
        assertThat(updatedBy).isEqualTo(updateRequest.updatedBy)
        assertThat(updatedTime).isEqualTo(updateRequest.updatedTime)
      }
    }

    @Test
    fun `should fail to update a contact restriction by ID when contact is not found`() {
      val updateRequest = updateContactRestrictionRequest()
      whenever(contactRepository.findById(1L)).thenReturn(Optional.empty())
      assertThrows<EntityNotFoundException> {
        syncService.updateContactRestriction(1L, updateRequest)
      }
      verifyNoInteractions(contactRestrictionRepository)
    }

    @Test
    fun `should fail to update a contact restriction when contact restriction is not found`() {
      val updateRequest = updateContactRestrictionRequest()
      whenever(contactRepository.findById(1L)).thenReturn(Optional.of(contactEntity(1L)))
      whenever(contactRestrictionRepository.findById(updateRequest.contactId)).thenReturn(Optional.empty())
      assertThrows<EntityNotFoundException> {
        syncService.updateContactRestriction(1L, updateRequest)
      }
      verify(contactRepository).findById(updateRequest.contactId)
      verify(contactRestrictionRepository).findById(1L)
    }
  }

  private fun updateContactRestrictionRequest(
    contactId: Long = 1L,
    startDate: LocalDate? = LocalDate.of(1980, 2, 1),
    expiryDate: LocalDate? = LocalDate.of(2025, 2, 1),
  ) = SyncUpdateContactRestrictionRequest(
    contactId = contactId,
    restrictionType = "DRIVING",
    startDate = startDate,
    expiryDate = expiryDate,
    comments = "N/A",
    updatedBy = "TEST",
    updatedTime = LocalDateTime.now(),
  )

  private fun createContactRestrictionRequest(
    startDate: LocalDate? = LocalDate.of(1980, 2, 1),
    expiryDate: LocalDate? = LocalDate.of(2025, 2, 1),
  ) = SyncCreateContactRestrictionRequest(
    contactId = 1L,
    restrictionType = "DRIVING",
    startDate = startDate,
    expiryDate = expiryDate,
    comments = "N/A",
    createdBy = "TEST",
  )

  private fun contactEntity(contactId: Long = 1L) = ContactEntity(
    contactId = contactId,
    title = "Mr",
    firstName = "John",
    middleNames = null,
    lastName = "Smith",
    dateOfBirth = null,
    deceasedDate = null,
    createdBy = "TEST",
    createdTime = LocalDateTime.now(),
  )

  private fun contactRestrictionEntity() = ContactRestrictionEntity(
    contactRestrictionId = 1L,
    contactId = 1L,
    restrictionType = "DRIVING",
    startDate = LocalDate.of(1980, 2, 1),
    expiryDate = LocalDate.of(2025, 2, 1),
    comments = "N/A",
    createdBy = "TEST",
    createdTime = LocalDateTime.now(),
  )

  private fun SyncUpdateContactRestrictionRequest.toEntity(contactRestrictionId: Long = 1L): ContactRestrictionEntity {
    val updatedBy = this.updatedBy
    val updatedTime = this.updatedTime

    return ContactRestrictionEntity(
      contactRestrictionId = contactRestrictionId,
      contactId = this.contactId,
      restrictionType = this.restrictionType,
      startDate = this.startDate,
      expiryDate = this.expiryDate,
      comments = this.comments,
      createdBy = "TEST",
      createdTime = LocalDateTime.now(),
      updatedBy = updatedBy,
      updatedTime = updatedTime,
    )
  }
}
