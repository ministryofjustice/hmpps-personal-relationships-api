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
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.personalrelationships.entity.ContactEntity
import uk.gov.justice.digital.hmpps.personalrelationships.entity.ContactPhoneEntity
import uk.gov.justice.digital.hmpps.personalrelationships.mapping.sync.toEntity
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.sync.SyncCreateContactPhoneRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.sync.SyncUpdateContactPhoneRequest
import uk.gov.justice.digital.hmpps.personalrelationships.repository.ContactPhoneRepository
import uk.gov.justice.digital.hmpps.personalrelationships.repository.ContactRepository
import java.time.LocalDateTime
import java.util.*

class SyncContactPhoneServiceTest {
  private val contactRepository: ContactRepository = mock()
  private val contactPhoneRepository: ContactPhoneRepository = mock()
  private val syncService = SyncContactPhoneService(contactRepository, contactPhoneRepository)

  @Nested
  inner class SyncContactPhoneTests {
    @Test
    fun `should get a contact phone by ID`() {
      whenever(contactPhoneRepository.findById(1L)).thenReturn(Optional.of(contactPhoneEntity()))
      val contactPhone = syncService.getContactPhoneById(1L)
      with(contactPhone) {
        assertThat(phoneType).isEqualTo("Mobile")
        assertThat(phoneNumber).isEqualTo("555-1234")
        assertThat(phoneNumber).isEqualTo("555-1234")
        assertThat(extNumber).isEqualTo("101")
      }
      verify(contactPhoneRepository).findById(1L)
    }

    @Test
    fun `should fail to get a contact phone by ID when not found`() {
      whenever(contactPhoneRepository.findById(1L)).thenReturn(Optional.empty())
      assertThrows<EntityNotFoundException> {
        syncService.getContactPhoneById(1L)
      }
      verify(contactPhoneRepository).findById(1L)
    }

    @Test
    fun `should create a contact phone`() {
      val request = createContactPhoneRequest()
      whenever(contactRepository.findById(1L)).thenReturn(Optional.of(contactEntity()))
      whenever(contactPhoneRepository.saveAndFlush(request.toEntity())).thenReturn(request.toEntity())

      val contactPhone = syncService.createContactPhone(request)
      val phoneCaptor = argumentCaptor<ContactPhoneEntity>()

      verify(contactPhoneRepository).saveAndFlush(phoneCaptor.capture())

      // Checks the entity saved
      with(phoneCaptor.firstValue) {
        assertThat(phoneType).isEqualTo(request.phoneType)
        assertThat(phoneNumber).isEqualTo(request.phoneNumber)
        assertThat(extNumber).isEqualTo(request.extNumber)
        assertThat(createdBy).isEqualTo(request.createdBy)
      }

      // Checks the model response
      with(contactPhone) {
        assertThat(contactPhoneId).isEqualTo(0L)
        assertThat(phoneType).isEqualTo(request.phoneType)
        assertThat(phoneNumber).isEqualTo(request.phoneNumber)
        assertThat(extNumber).isEqualTo(request.extNumber)
        assertThat(createdBy).isEqualTo(request.createdBy)
      }

      verify(contactRepository).findById(1L)
    }

    @Test
    fun `should fail to create a contact phone when the contact ID is not present`() {
      val request = createContactPhoneRequest()
      whenever(contactRepository.findById(1L)).thenReturn(Optional.empty())
      assertThrows<EntityNotFoundException> {
        syncService.createContactPhone(request)
      }
      verifyNoInteractions(contactPhoneRepository)
    }

    @Test
    fun `should delete contact phone by ID`() {
      whenever(contactPhoneRepository.findById(1L)).thenReturn(Optional.of(contactPhoneEntity()))
      syncService.deleteContactPhone(1L)
      verify(contactPhoneRepository).deleteById(1L)
    }

    @Test
    fun `should fail to delete contact phone by ID when not found`() {
      whenever(contactPhoneRepository.findById(1L)).thenReturn(Optional.empty())
      assertThrows<EntityNotFoundException> {
        syncService.deleteContactPhone(1L)
      }
      verify(contactPhoneRepository).findById(1L)
    }

    @Test
    fun `should update a contact phone by ID`() {
      val request = updateContactPhoneRequest()
      whenever(contactRepository.findById(1L)).thenReturn(Optional.of(contactEntity()))
      whenever(contactPhoneRepository.findById(1L)).thenReturn(Optional.of(request.toEntity()))
      whenever(contactPhoneRepository.saveAndFlush(any())).thenReturn(request.toEntity())

      val updated = syncService.updateContactPhone(1L, request)

      val phoneCaptor = argumentCaptor<ContactPhoneEntity>()

      verify(contactPhoneRepository).saveAndFlush(phoneCaptor.capture())

      // Checks the entity saved
      with(phoneCaptor.firstValue) {
        assertThat(phoneType).isEqualTo(request.phoneType)
        assertThat(phoneNumber).isEqualTo(request.phoneNumber)
        assertThat(extNumber).isEqualTo(request.extNumber)
        assertThat(updatedBy).isEqualTo(request.updatedBy)
        assertThat(updatedTime).isEqualTo(request.updatedTime)
      }

      // Checks the model returned
      with(updated) {
        assertThat(phoneType).isEqualTo(request.phoneType)
        assertThat(phoneNumber).isEqualTo(request.phoneNumber)
        assertThat(extNumber).isEqualTo(request.extNumber)
        assertThat(updatedBy).isEqualTo(request.updatedBy)
        assertThat(updatedTime).isEqualTo(request.updatedTime)
      }
    }

    @Test
    fun `should fail to update a contact phone by ID when contact is not found`() {
      val updateRequest = updateContactPhoneRequest()
      whenever(contactRepository.findById(1L)).thenReturn(Optional.empty())
      assertThrows<EntityNotFoundException> {
        syncService.updateContactPhone(1L, updateRequest)
      }
      verifyNoInteractions(contactPhoneRepository)
    }

    @Test
    fun `should fail to update a contact phone when contact phone is not found`() {
      val updateRequest = updateContactPhoneRequest()
      whenever(contactRepository.findById(1L)).thenReturn(Optional.of(contactEntity(1L)))
      whenever(contactPhoneRepository.findById(updateRequest.contactId)).thenReturn(Optional.empty())
      assertThrows<EntityNotFoundException> {
        syncService.updateContactPhone(1L, updateRequest)
      }
      verify(contactRepository).findById(updateRequest.contactId)
      verify(contactPhoneRepository).findById(1L)
    }
  }

  private fun updateContactPhoneRequest(contactId: Long = 1L) = SyncUpdateContactPhoneRequest(
    contactId = contactId,
    phoneType = "Mobile",
    phoneNumber = "555-1234",
    extNumber = "101",
    updatedBy = "TEST",
    updatedTime = LocalDateTime.now(),
  )

  private fun createContactPhoneRequest() = SyncCreateContactPhoneRequest(
    contactId = 1L,
    phoneType = "Mobile",
    phoneNumber = "555-1234",
    extNumber = "101",
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

  private fun contactPhoneEntity() = ContactPhoneEntity(
    contactPhoneId = 1L,
    contactId = 1L,
    phoneType = "Mobile",
    phoneNumber = "555-1234",
    extNumber = "101",
    createdBy = "TEST",
    createdTime = LocalDateTime.now(),
  )

  private fun SyncUpdateContactPhoneRequest.toEntity(contactPhoneId: Long = 1L): ContactPhoneEntity {
    val updatedBy = this.updatedBy
    val updatedTime = this.updatedTime

    return ContactPhoneEntity(
      contactPhoneId = contactPhoneId,
      contactId = this.contactId,
      phoneType = this.phoneType,
      phoneNumber = this.phoneNumber,
      extNumber = this.extNumber,
      createdBy = "TEST",
      updatedBy = updatedBy,
      updatedTime = updatedTime,
      createdTime = LocalDateTime.now(),
    )
  }
}
