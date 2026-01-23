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
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.personalrelationships.entity.ContactAddressEntity
import uk.gov.justice.digital.hmpps.personalrelationships.entity.ContactAddressPhoneEntity
import uk.gov.justice.digital.hmpps.personalrelationships.entity.ContactPhoneEntity
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.sync.SyncCreateContactAddressPhoneRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.sync.SyncUpdateContactAddressPhoneRequest
import uk.gov.justice.digital.hmpps.personalrelationships.repository.ContactAddressPhoneRepository
import uk.gov.justice.digital.hmpps.personalrelationships.repository.ContactAddressRepository
import uk.gov.justice.digital.hmpps.personalrelationships.repository.ContactPhoneRepository
import uk.gov.justice.digital.hmpps.personalrelationships.repository.ContactRepository
import java.time.LocalDateTime
import java.util.Optional

class SyncContactAddressPhoneServiceTest {
  private val contactRepository: ContactRepository = mock()
  private val contactAddressRepository: ContactAddressRepository = mock()
  private val contactPhoneRepository: ContactPhoneRepository = mock()
  private val contactAddressPhoneRepository: ContactAddressPhoneRepository = mock()

  private val syncService = SyncContactAddressPhoneService(
    contactRepository,
    contactAddressRepository,
    contactPhoneRepository,
    contactAddressPhoneRepository,
  )

  @Nested
  inner class SyncContactAddressPhoneTests {
    @Test
    fun `should get an address-specific phone number by ID`() {
      whenever(contactAddressPhoneRepository.findById(4L)).thenReturn(Optional.of(contactAddressPhoneEntity()))
      whenever(contactPhoneRepository.findById(2L)).thenReturn(Optional.of(contactPhoneEntity()))

      val contactAddressPhone = syncService.getContactAddressPhoneById(4L)

      with(contactAddressPhone) {
        assertThat(contactAddressPhoneId).isEqualTo(4L)
        assertThat(contactAddressId).isEqualTo(3L)
        assertThat(contactPhoneId).isEqualTo(2L)
        assertThat(contactId).isEqualTo(1L)
        assertThat(phoneType).isEqualTo("MOB")
        assertThat(phoneNumber).isEqualTo("0909 111222")
        assertThat(extNumber).isNullOrEmpty()
        assertThat(createdBy).isEqualTo("CREATOR")
      }

      verify(contactAddressPhoneRepository).findById(4L)
      verify(contactPhoneRepository).findById(2L)
    }

    @Test
    fun `should fail to get an address-specific phone number when the ID is not found`() {
      whenever(contactAddressPhoneRepository.findById(4L)).thenReturn(Optional.empty())
      assertThrows<EntityNotFoundException> {
        syncService.getContactAddressPhoneById(4L)
      }
      verify(contactAddressPhoneRepository).findById(4L)
      verify(contactPhoneRepository, never()).findById(any())
    }

    @Test
    fun `should fail to get an address-specific phone number when phone details are not found`() {
      whenever(contactAddressPhoneRepository.findById(4L)).thenReturn(Optional.of(contactAddressPhoneEntity()))
      whenever(contactPhoneRepository.findById(2L)).thenReturn(Optional.empty())

      assertThrows<EntityNotFoundException> {
        syncService.getContactAddressPhoneById(4L)
      }

      verify(contactAddressPhoneRepository).findById(4L)
      verify(contactPhoneRepository).findById(2L)
    }

    @Test
    fun `should create an address-specific phone number`() {
      val request = createContactAddressPhoneRequest()

      whenever(contactAddressRepository.findById(3L)).thenReturn(Optional.of(contactAddressEntity()))
      whenever(contactPhoneRepository.saveAndFlush(any())).thenReturn(contactPhoneEntity())
      whenever(contactAddressPhoneRepository.saveAndFlush(any())).thenReturn(contactAddressPhoneEntity())

      val response = syncService.createContactAddressPhone(request)

      val phoneCaptor = argumentCaptor<ContactPhoneEntity>()

      verify(contactPhoneRepository).saveAndFlush(phoneCaptor.capture())

      with(phoneCaptor.firstValue) {
        assertThat(phoneType).isEqualTo(request.phoneType)
        assertThat(phoneNumber).isEqualTo(request.phoneNumber)
        assertThat(extNumber).isEqualTo(request.extNumber)
        assertThat(createdBy).isEqualTo(request.createdBy)
      }

      with(response) {
        assertThat(contactAddressPhoneId).isEqualTo(4L)
        assertThat(contactAddressId).isEqualTo(3L)
        assertThat(contactPhoneId).isEqualTo(2L)
        assertThat(contactId).isEqualTo(1L)
        assertThat(phoneType).isEqualTo(request.phoneType)
        assertThat(phoneNumber).isEqualTo(request.phoneNumber)
        assertThat(extNumber).isEqualTo(request.extNumber)
        assertThat(createdBy).isEqualTo(request.createdBy)
      }

      verify(contactAddressRepository).findById(3L)
      verify(contactPhoneRepository).saveAndFlush(any())
      verify(contactAddressPhoneRepository).saveAndFlush(any())
    }

    @Test
    fun `should fail to create an address-specific phone number when the address is not found`() {
      val request = createContactAddressPhoneRequest()
      whenever(contactAddressRepository.findById(3L)).thenReturn(Optional.empty())
      assertThrows<EntityNotFoundException> {
        syncService.createContactAddressPhone(request)
      }
      verifyNoInteractions(contactPhoneRepository)
      verifyNoInteractions(contactAddressPhoneRepository)
    }

    @Test
    fun `should delete an address-specific phone number`() {
      whenever(contactAddressPhoneRepository.findById(4L)).thenReturn(Optional.of(contactAddressPhoneEntity()))
      whenever(contactPhoneRepository.findById(2L)).thenReturn(Optional.of(contactPhoneEntity()))

      syncService.deleteContactAddressPhone(4L)

      verify(contactAddressPhoneRepository).findById(4L)
      verify(contactPhoneRepository).findById(2L)
      verify(contactPhoneRepository).deleteById(2L)
      verify(contactAddressPhoneRepository).deleteById(4L)
    }

    @Test
    fun `should fail to delete an address-specific phone number if the ID is not found`() {
      whenever(contactAddressPhoneRepository.findById(any())).thenReturn(Optional.empty())
      assertThrows<EntityNotFoundException> {
        syncService.deleteContactAddressPhone(4L)
      }
      verify(contactAddressPhoneRepository).findById(any())
    }

    @Test
    fun `should update an address-specific phone number by ID`() {
      val request = updateContactAddressPhoneRequest()

      whenever(contactAddressPhoneRepository.findById(4L)).thenReturn(Optional.of(contactAddressPhoneEntity()))
      whenever(contactPhoneRepository.findById(2L)).thenReturn(Optional.of(contactPhoneEntity()))
      whenever(contactPhoneRepository.saveAndFlush(any())).thenReturn(contactPhoneEntity())

      val updated = syncService.updateContactAddressPhone(4L, request)

      val phoneCaptor = argumentCaptor<ContactPhoneEntity>()

      verify(contactPhoneRepository).saveAndFlush(phoneCaptor.capture())

      with(phoneCaptor.firstValue) {
        assertThat(phoneType).isEqualTo(request.phoneType)
        assertThat(phoneNumber).isEqualTo(request.phoneNumber)
        assertThat(extNumber).isEqualTo(request.extNumber)
        assertThat(updatedBy).isEqualTo(request.updatedBy)
        assertThat(updatedTime).isEqualTo(request.updatedTime)
      }

      with(updated) {
        assertThat(contactAddressPhoneId).isEqualTo(4L)
        assertThat(contactAddressId).isEqualTo(3L)
        assertThat(contactPhoneId).isEqualTo(2L)
        assertThat(contactId).isEqualTo(1L)
        assertThat(phoneType).isEqualTo(request.phoneType)
        assertThat(phoneNumber).isEqualTo(request.phoneNumber)
        assertThat(extNumber).isEqualTo(request.extNumber)
      }

      verify(contactAddressPhoneRepository).findById(4L)
      verify(contactPhoneRepository).findById(2L)
    }

    @Test
    fun `should fail to update an address-specific phone number if the ID is not found`() {
      val request = updateContactAddressPhoneRequest()
      whenever(contactAddressPhoneRepository.findById(4L)).thenReturn(Optional.empty())
      assertThrows<EntityNotFoundException> {
        syncService.updateContactAddressPhone(4L, request)
      }
      verifyNoInteractions(contactPhoneRepository)
    }

    @Test
    fun `should fail to update an address-specific phone number if the phone details are not found`() {
      val request = updateContactAddressPhoneRequest()

      whenever(contactAddressPhoneRepository.findById(4L)).thenReturn(Optional.of(contactAddressPhoneEntity()))
      whenever(contactPhoneRepository.findById(2L)).thenReturn(Optional.empty())

      assertThrows<EntityNotFoundException> {
        syncService.updateContactAddressPhone(4L, request)
      }

      verify(contactAddressPhoneRepository).findById(4L)
      verify(contactPhoneRepository).findById(2L)
      verify(contactPhoneRepository, never()).saveAndFlush(any())
    }
  }

  private fun createContactAddressPhoneRequest() = SyncCreateContactAddressPhoneRequest(
    contactAddressId = 3L,
    phoneType = "MOB",
    phoneNumber = "0909 111222",
    createdBy = "CREATOR",
    createdTime = LocalDateTime.now(),
  )

  private fun updateContactAddressPhoneRequest() = SyncUpdateContactAddressPhoneRequest(
    phoneType = "MOB",
    phoneNumber = "0909 111222",
    updatedBy = "UPDATER",
    updatedTime = LocalDateTime.now(),
  )

  private fun contactAddressPhoneEntity() = ContactAddressPhoneEntity(
    contactAddressPhoneId = 4L,
    contactAddressId = 3L,
    contactPhoneId = 2L,
    contactId = 1L,
    createdBy = "CREATOR",
    createdTime = LocalDateTime.now(),
  )

  private fun contactAddressEntity() = ContactAddressEntity(
    contactAddressId = 3L,
    contactId = 1L,
    createdBy = "CREATOR",
    createdTime = LocalDateTime.now(),
  )

  private fun contactPhoneEntity() = ContactPhoneEntity(
    contactPhoneId = 2L,
    contactId = 1L,
    phoneType = "MOB",
    phoneNumber = "0909 111222",
    createdBy = "CREATOR",
    createdTime = LocalDateTime.now(),
  )
}
