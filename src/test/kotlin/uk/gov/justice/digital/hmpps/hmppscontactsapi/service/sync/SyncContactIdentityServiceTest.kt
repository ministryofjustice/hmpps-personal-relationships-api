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
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.ContactIdentityEntity
import uk.gov.justice.digital.hmpps.hmppscontactsapi.mapping.sync.toEntity
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync.SyncCreateContactIdentityRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync.SyncUpdateContactIdentityRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.ContactIdentityRepository
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.ContactRepository
import java.time.LocalDateTime
import java.util.*

class SyncContactIdentityServiceTest {
  private val contactRepository: ContactRepository = mock()
  private val contactIdentityRepository: ContactIdentityRepository = mock()
  private val syncService = SyncContactIdentityService(contactRepository, contactIdentityRepository)

  @Nested
  inner class ContactIdentityTests {
    @Test
    fun `should get a contact identity by ID`() {
      whenever(contactIdentityRepository.findById(1L)).thenReturn(Optional.of(contactIdentityEntity()))
      val contactIdentity = syncService.getContactIdentityById(1L)
      with(contactIdentity) {
        assertThat(identityType).isEqualTo("PASS")
      }
      verify(contactIdentityRepository).findById(1L)
    }

    @Test
    fun `should fail to get a contact identity by ID when not found`() {
      whenever(contactIdentityRepository.findById(1L)).thenReturn(Optional.empty())
      assertThrows<EntityNotFoundException> {
        syncService.getContactIdentityById(1L)
      }
      verify(contactIdentityRepository).findById(1L)
    }

    @Test
    fun `should create a contact identity`() {
      val request = createContactIdentityRequest()
      whenever(contactRepository.findById(1L)).thenReturn(Optional.of(contactEntity()))
      whenever(contactIdentityRepository.saveAndFlush(request.toEntity())).thenReturn(request.toEntity())

      val contactIdentity = syncService.createContactIdentity(request)
      val identityCaptor = argumentCaptor<ContactIdentityEntity>()

      verify(contactIdentityRepository).saveAndFlush(identityCaptor.capture())

      // Checks the entity saved
      with(identityCaptor.firstValue) {
        assertThat(identityType).isEqualTo(request.identityType)
        assertThat(createdBy).isEqualTo(request.createdBy)
      }

      // Checks the model response
      with(contactIdentity) {
        assertThat(contactIdentityId).isEqualTo(0L)
        assertThat(identityType).isEqualTo(request.identityType)
        assertThat(issuingAuthority).isEqualTo(request.issuingAuthority)
        assertThat(createdBy).isEqualTo(request.createdBy)
      }

      verify(contactRepository).findById(1L)
    }

    @Test
    fun `should fail to create a contact identity when the contact ID is not present`() {
      val request = createContactIdentityRequest()
      whenever(contactRepository.findById(1L)).thenReturn(Optional.empty())
      assertThrows<EntityNotFoundException> {
        syncService.createContactIdentity(request)
      }
      verifyNoInteractions(contactIdentityRepository)
    }

    @Test
    fun `should delete contact identity by ID`() {
      whenever(contactIdentityRepository.findById(1L)).thenReturn(Optional.of(contactIdentityEntity()))
      syncService.deleteContactIdentity(1L)
      verify(contactIdentityRepository).deleteById(1L)
    }

    @Test
    fun `should fail to delete contact identity by ID when not found`() {
      whenever(contactIdentityRepository.findById(1L)).thenReturn(Optional.empty())
      assertThrows<EntityNotFoundException> {
        syncService.deleteContactIdentity(1L)
      }
      verify(contactIdentityRepository).findById(1L)
    }

    @Test
    fun `should update a contact identity by ID`() {
      val request = updateContactIdentityRequest()
      whenever(contactRepository.findById(1L)).thenReturn(Optional.of(contactEntity()))
      whenever(contactIdentityRepository.findById(1L)).thenReturn(Optional.of(request.toEntity()))
      whenever(contactIdentityRepository.saveAndFlush(any())).thenReturn(request.toEntity())

      val updated = syncService.updateContactIdentity(1L, request)

      val identityCaptor = argumentCaptor<ContactIdentityEntity>()

      verify(contactIdentityRepository).saveAndFlush(identityCaptor.capture())

      // Checks the entity saved
      with(identityCaptor.firstValue) {
        assertThat(identityType).isEqualTo(request.identityType)
        assertThat(issuingAuthority).isEqualTo(request.issuingAuthority)
        assertThat(updatedBy).isEqualTo(request.updatedBy)
        assertThat(updatedTime).isEqualTo(request.updatedTime)
      }

      // Checks the model returned
      with(updated) {
        assertThat(identityType).isEqualTo(request.identityType)
        assertThat(issuingAuthority).isEqualTo(request.issuingAuthority)
        assertThat(updatedBy).isEqualTo(request.updatedBy)
        assertThat(updatedTime).isEqualTo(request.updatedTime)
      }
    }

    @Test
    fun `should update a contact identity by ID when issuing authority is not provided then retain the existing value`() {
      val request = updateContactIdentityRequest(issuingAuthority = null)
      val existingIssuingAuthority = "UKBORDER"
      whenever(contactRepository.findById(1L)).thenReturn(Optional.of(contactEntity()))
      whenever(contactIdentityRepository.findById(1L)).thenReturn(Optional.of(request.toEntity()))
      whenever(contactIdentityRepository.saveAndFlush(any())).thenReturn(request.toEntity())

      val updated = syncService.updateContactIdentity(1L, request)

      val identityCaptor = argumentCaptor<ContactIdentityEntity>()

      verify(contactIdentityRepository).saveAndFlush(identityCaptor.capture())

      // Checks the entity saved
      with(identityCaptor.firstValue) {
        assertThat(identityType).isEqualTo(request.identityType)
        assertThat(issuingAuthority).isEqualTo(existingIssuingAuthority)
        assertThat(updatedBy).isEqualTo(request.updatedBy)
        assertThat(updatedTime).isEqualTo(request.updatedTime)
      }

      // Checks the model returned
      with(updated) {
        assertThat(identityType).isEqualTo(request.identityType)
        assertThat(issuingAuthority).isEqualTo(existingIssuingAuthority)
        assertThat(updatedBy).isEqualTo(request.updatedBy)
        assertThat(updatedTime).isEqualTo(request.updatedTime)
      }
    }

    @Test
    fun `should fail to update a contact identity by ID when contact is not found`() {
      val updateRequest = updateContactIdentityRequest()
      whenever(contactRepository.findById(1L)).thenReturn(Optional.empty())
      assertThrows<EntityNotFoundException> {
        syncService.updateContactIdentity(1L, updateRequest)
      }
      verifyNoInteractions(contactIdentityRepository)
    }

    @Test
    fun `should fail to update a contact identity when contact identity is not found`() {
      val updateRequest = updateContactIdentityRequest()
      whenever(contactRepository.findById(1L)).thenReturn(Optional.of(contactEntity(1L)))
      whenever(contactIdentityRepository.findById(updateRequest.contactId)).thenReturn(Optional.empty())
      assertThrows<EntityNotFoundException> {
        syncService.updateContactIdentity(1L, updateRequest)
      }
      verify(contactRepository).findById(updateRequest.contactId)
      verify(contactIdentityRepository).findById(1L)
    }
  }

  private fun updateContactIdentityRequest(contactId: Long = 1L, issuingAuthority: String? = "UKBORDER") = SyncUpdateContactIdentityRequest(
    contactId = contactId,
    identityType = "PASS",
    identityValue = "PP87878787878",
    issuingAuthority = issuingAuthority,
    updatedBy = "TEST",
    updatedTime = LocalDateTime.now(),
  )

  private fun createContactIdentityRequest() = SyncCreateContactIdentityRequest(
    contactId = 1L,
    identityType = "PASS",
    identityValue = "PP87878787878",
    issuingAuthority = "UKBORDER",
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

  private fun contactIdentityEntity() = ContactIdentityEntity(
    contactIdentityId = 1L,
    contactId = 1L,
    identityType = "PASS",
    identityValue = "PP87878787878",
    issuingAuthority = "UKBORDER",
    createdBy = "TEST",
    createdTime = LocalDateTime.now(),
  )

  private fun SyncUpdateContactIdentityRequest.toEntity(contactIdentityId: Long = 1L, issuingAuthority: String? = "UKBORDER"): ContactIdentityEntity = ContactIdentityEntity(
    contactIdentityId = contactIdentityId,
    contactId = this.contactId,
    identityType = this.identityType,
    identityValue = this.identityValue,
    issuingAuthority = issuingAuthority,
    createdBy = "TEST",
    createdTime = LocalDateTime.now(),
    updatedBy = this.updatedBy,
    updatedTime = this.updatedTime,
  )
}
