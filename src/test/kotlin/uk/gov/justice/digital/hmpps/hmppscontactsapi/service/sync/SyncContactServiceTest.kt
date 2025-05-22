package uk.gov.justice.digital.hmpps.hmppscontactsapi.service.sync

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
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.ContactWithFixedIdEntity
import uk.gov.justice.digital.hmpps.hmppscontactsapi.exception.DuplicatePersonException
import uk.gov.justice.digital.hmpps.hmppscontactsapi.mapping.sync.mapSyncRequestToEntity
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync.SyncCreateContactRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync.SyncUpdateContactRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.ContactWithFixedIdRepository
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class SyncContactServiceTest {
  private val contactRepository: ContactWithFixedIdRepository = mock()
  private val syncService = SyncContactService(contactRepository)

  @Nested
  inner class ContactTests {
    @Test
    fun `should get a contact by ID`() {
      whenever(contactRepository.findById(1L)).thenReturn(Optional.of(contactEntity()))
      val contact = syncService.getContactById(1L)
      with(contact) {
        assertThat(title).isEqualTo("Mr")
        assertThat(firstName).isEqualTo("John")
        assertThat(middleName).isNull()
        assertThat(lastName).isEqualTo("Smith")
        assertThat(createdBy).isEqualTo("TEST")
        assertThat(gender).isEqualTo("M")
        assertThat(domesticStatus).isEqualTo("S")
        assertThat(languageCode).isEqualTo("EN")
        assertThat(dateOfBirth).isNull()
        assertThat(deceasedDate).isNull()
        assertThat(isStaff).isFalse()
        assertThat(deceasedFlag).isFalse()
        assertThat(interpreterRequired).isFalse()
      }
      verify(contactRepository).findById(1L)
    }

    @Test
    fun `should fail to get a contact by ID when not found`() {
      whenever(contactRepository.findById(1L)).thenReturn(Optional.empty())
      assertThrows<EntityNotFoundException> {
        syncService.getContactById(1L)
      }
      verify(contactRepository).findById(1L)
    }

    @Test
    fun `should create a contact`() {
      whenever(contactRepository.existsById(2L)).thenReturn(false)
      val request = createSyncContactRequest(2L)
      whenever(contactRepository.saveAndFlush(request.mapSyncRequestToEntity())).thenReturn(request.mapSyncRequestToEntity())

      val contact = syncService.createContact(request)
      val contactCaptor = argumentCaptor<ContactWithFixedIdEntity>()

      verify(contactRepository).saveAndFlush(contactCaptor.capture())

      // Checks the entity saved
      with(contactCaptor.firstValue) {
        assertThat(title).isEqualTo(request.title)
        assertThat(lastName).isEqualTo(request.lastName)
        assertThat(createdBy).isEqualTo(request.createdBy)
      }

      // Checks the model response
      with(contact) {
        assertThat(id).isEqualTo(2L)
        assertThat(title).isEqualTo(request.title)
        assertThat(lastName).isEqualTo(request.lastName)
        assertThat(createdBy).isEqualTo(request.createdBy)
      }
    }

    @Test
    fun `should fail to create a contact if the personId already exists`() {
      whenever(contactRepository.existsById(1L)).thenReturn(true)
      val exceptionExpected = DuplicatePersonException("Sync: Duplicate person ID received 1")
      val request = createSyncContactRequest(1L)
      val exceptionThrown = assertThrows<DuplicatePersonException> {
        syncService.createContact(request)
      }
      assertThat(exceptionThrown.message).isEqualTo(exceptionExpected.message)
      assertThat(exceptionThrown.javaClass).isEqualTo(exceptionExpected.javaClass)
      verify(contactRepository, never()).saveAndFlush(any())
    }

    @Test
    fun `should delete contact by ID`() {
      whenever(contactRepository.findById(1L)).thenReturn(Optional.of(contactEntity()))
      syncService.deleteContact(1L)
      verify(contactRepository).deleteById(1L)
    }

    @Test
    fun `should fail to delete contact by ID when not found`() {
      whenever(contactRepository.findById(1L)).thenReturn(Optional.empty())
      assertThrows<EntityNotFoundException> {
        syncService.deleteContact(1L)
      }
      verify(contactRepository).findById(1L)
    }

    @Test
    fun `should update a contact by ID`() {
      val request = updateContactRequest()
      whenever(contactRepository.findById(1L)).thenReturn(Optional.of(contactEntity()))
      whenever(contactRepository.findById(1L)).thenReturn(Optional.of(request.toEntity()))
      whenever(contactRepository.saveAndFlush(any())).thenReturn(request.toEntity())

      val updated = syncService.updateContact(1L, request)

      val contactCaptor = argumentCaptor<ContactWithFixedIdEntity>()

      verify(contactRepository).saveAndFlush(contactCaptor.capture())

      // Checks the entity saved
      with(contactCaptor.firstValue) {
        assertThat(title).isEqualTo(request.title)
        assertThat(lastName).isEqualTo(request.lastName)
        assertThat(updatedBy).isEqualTo(request.updatedBy)
        assertThat(updatedTime).isEqualTo(request.updatedTime)
      }

      // Checks the model returned
      with(updated) {
        assertThat(title).isEqualTo(request.title)
        assertThat(lastName).isEqualTo(request.lastName)
        assertThat(updatedBy).isEqualTo(request.updatedBy)
        assertThat(updatedTime).isEqualTo(request.updatedTime)
      }
    }

    @Test
    fun `should fail to update a contact when contact is not found`() {
      val updateRequest = updateContactRequest()
      whenever(contactRepository.findById(1L)).thenReturn(Optional.empty())
      assertThrows<EntityNotFoundException> {
        syncService.updateContact(1L, updateRequest)
      }
      verify(contactRepository).findById(1L)
    }
  }

  private fun updateContactRequest() = SyncUpdateContactRequest(
    title = "Mr",
    firstName = "John",
    lastName = "Doe",
    middleName = "William",
    dateOfBirth = LocalDate.of(1980, 1, 1),
    isStaff = false,
    deceasedFlag = false,
    deceasedDate = null,
    gender = "M",
    domesticStatus = "S",
    languageCode = "EN",
    interpreterRequired = false,
    updatedBy = "Admin",
    updatedTime = LocalDateTime.now(),
  )

  private fun createSyncContactRequest(personId: Long) = SyncCreateContactRequest(
    personId = personId,
    title = "Mr",
    firstName = "John",
    lastName = "Doe",
    middleName = "William",
    dateOfBirth = LocalDate.of(1980, 1, 1),
    createdBy = "JD000001",
    createdTime = LocalDateTime.of(2024, 1, 1, 0, 0, 0),
    isStaff = false,
    deceasedFlag = false,
    deceasedDate = null,
    gender = "M",
    domesticStatus = "S",
    languageCode = "EN",
    interpreterRequired = false,
  )

  private fun contactEntity() = ContactWithFixedIdEntity(
    contactId = 1L,
    title = "Mr",
    firstName = "John",
    middleNames = null,
    lastName = "Smith",
    dateOfBirth = null,
    deceasedDate = null,
    createdBy = "TEST",
    createdTime = LocalDateTime.now(),
    staffFlag = false,
    gender = "M",
    domesticStatus = "S",
    languageCode = "EN",
    interpreterRequired = false,
  )

  private fun SyncUpdateContactRequest.toEntity(contactId: Long = 1L): ContactWithFixedIdEntity {
    val updatedBy = this.updatedBy
    val updatedTime = this.updatedTime

    return ContactWithFixedIdEntity(
      contactId = contactId,
      title = this.title,
      firstName = this.firstName,
      lastName = this.lastName,
      middleNames = this.middleName,
      dateOfBirth = this.dateOfBirth,
      deceasedDate = null,
      createdBy = "Admin",
      staffFlag = this.isStaff,
      gender = this.gender,
      domesticStatus = this.domesticStatus,
      languageCode = this.languageCode,
      interpreterRequired = this.interpreterRequired ?: false,
      updatedBy = updatedBy,
      updatedTime = updatedTime,
    )
  }
}
