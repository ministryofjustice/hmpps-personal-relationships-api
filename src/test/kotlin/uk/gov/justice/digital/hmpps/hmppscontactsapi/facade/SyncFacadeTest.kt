package uk.gov.justice.digital.hmpps.hmppscontactsapi.facade

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppscontactsapi.config.User
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync.SyncCreateContactAddressPhoneRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync.SyncCreateContactAddressRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync.SyncCreateContactEmailRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync.SyncCreateContactIdentityRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync.SyncCreateContactPhoneRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync.SyncCreateContactRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync.SyncCreateContactRestrictionRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync.SyncCreateEmploymentRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync.SyncCreatePrisonerContactRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync.SyncCreatePrisonerContactRestrictionRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync.SyncUpdateContactAddressPhoneRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync.SyncUpdateContactAddressRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync.SyncUpdateContactEmailRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync.SyncUpdateContactIdentityRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync.SyncUpdateContactPhoneRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync.SyncUpdateContactRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync.SyncUpdateContactRestrictionRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync.SyncUpdateEmploymentRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync.SyncUpdatePrisonerContactRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync.SyncUpdatePrisonerContactRestrictionRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.sync.SyncContact
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.sync.SyncContactAddress
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.sync.SyncContactAddressPhone
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.sync.SyncContactEmail
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.sync.SyncContactIdentity
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.sync.SyncContactPhone
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.sync.SyncContactRestriction
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.sync.SyncEmployment
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.sync.SyncPrisonerContact
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.sync.SyncPrisonerContactRestriction
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.OutboundEventsService
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.Source
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.sync.SyncAdminService
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.sync.SyncContactAddressPhoneService
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.sync.SyncContactAddressService
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.sync.SyncContactEmailService
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.sync.SyncContactIdentityService
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.sync.SyncContactPhoneService
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.sync.SyncContactReconciliationService
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.sync.SyncContactRestrictionService
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.sync.SyncContactService
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.sync.SyncEmploymentService
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.sync.SyncPrisonerContactRestrictionService
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.sync.SyncPrisonerContactService
import java.time.LocalDate
import java.time.LocalDateTime

class SyncFacadeTest {
  private val syncContactService: SyncContactService = mock()
  private val syncContactPhoneService: SyncContactPhoneService = mock()
  private val syncContactAddressService: SyncContactAddressService = mock()
  private val syncContactAddressPhoneService: SyncContactAddressPhoneService = mock()
  private val syncContactEmailService: SyncContactEmailService = mock()
  private val syncContactIdentityService: SyncContactIdentityService = mock()
  private val syncContactRestrictionService: SyncContactRestrictionService = mock()
  private val syncPrisonerContactService: SyncPrisonerContactService = mock()
  private val syncPrisonerContactRestrictionService: SyncPrisonerContactRestrictionService = mock()
  private val syncEmploymentService: SyncEmploymentService = mock()
  private val syncAdminService: SyncAdminService = mock()
  private val syncContactReconciliationService: SyncContactReconciliationService = mock()
  private val outboundEventsService: OutboundEventsService = mock()

  private val facade = SyncFacade(
    syncContactService,
    syncContactPhoneService,
    syncContactAddressService,
    syncContactAddressPhoneService,
    syncContactEmailService,
    syncContactIdentityService,
    syncContactRestrictionService,
    syncPrisonerContactService,
    syncPrisonerContactRestrictionService,
    syncEmploymentService,
    syncAdminService,
    syncContactReconciliationService,
    outboundEventsService,
  )

  @Nested
  inner class ContactSyncFacadeEvents {
    @Test
    fun `should send domain event on contact create success`() {
      val request = createSyncContactRequest(personId = 1L)
      val response = contactResponse(1L)

      whenever(syncContactService.createContact(any())).thenReturn(response)
      whenever(outboundEventsService.send(any(), any(), any(), any(), any(), any(), any())).then {}

      val result = facade.createContact(request)

      assertThat(result.id).isEqualTo(request.personId)

      verify(syncContactService).createContact(request)
      verify(outboundEventsService).send(
        outboundEvent = OutboundEvent.CONTACT_CREATED,
        identifier = result.id,
        contactId = result.id,
        source = Source.NOMIS,
        user = User.SYS_USER,
      )
    }

    @Test
    fun `should not send domain event on contact create failure`() {
      val request = createSyncContactRequest(personId = 2L)
      val expectedException = RuntimeException("Bang!")

      whenever(syncContactService.createContact(any())).thenThrow(expectedException)
      whenever(outboundEventsService.send(any(), any(), any(), any(), any(), any(), any())).then {}

      val exception = assertThrows<RuntimeException> {
        facade.createContact(request)
      }

      assertThat(exception.message).isEqualTo(expectedException.message)

      verify(syncContactService).createContact(request)
      verify(outboundEventsService, never()).send(any(), any(), any(), any(), any(), any(), any())
    }

    @Test
    fun `should send domain event on contact update success`() {
      val request = updateContactSyncRequest()
      val response = contactResponse(3L)

      whenever(syncContactService.updateContact(any(), any())).thenReturn(response)
      whenever(outboundEventsService.send(any(), any(), any(), any(), any(), any(), any())).then {}

      val result = facade.updateContact(3L, request)

      assertThat(result.id).isEqualTo(3L)

      verify(syncContactService).updateContact(3L, request)

      verify(outboundEventsService).send(
        outboundEvent = OutboundEvent.CONTACT_UPDATED,
        identifier = result.id,
        contactId = result.id,
        source = Source.NOMIS,
        user = User.SYS_USER,
      )
    }

    @Test
    fun `should not send domain event on contact update failure`() {
      val request = updateContactSyncRequest()
      val expectedException = RuntimeException("Bang!")

      whenever(syncContactService.updateContact(any(), any())).thenThrow(expectedException)
      whenever(outboundEventsService.send(any(), any(), any(), any(), any(), any(), any())).then {}

      val exception = assertThrows<RuntimeException> {
        facade.updateContact(4L, request)
      }

      assertThat(exception.message).isEqualTo(expectedException.message)

      verify(syncContactService).updateContact(4L, request)
      verify(outboundEventsService, never()).send(any(), any(), any(), any(), any(), any(), any())
    }

    @Test
    fun `should send domain event on contact delete success`() {
      whenever(syncContactService.deleteContact(any())).then {}
      whenever(outboundEventsService.send(any(), any(), any(), any(), any(), any(), any())).then {}

      facade.deleteContact(1L)

      verify(syncContactService).deleteContact(1L)

      verify(outboundEventsService).send(
        outboundEvent = OutboundEvent.CONTACT_DELETED,
        identifier = 1L,
        contactId = 1L,
        source = Source.NOMIS,
        user = User.SYS_USER,
      )
    }

    @Test
    fun `should not send domain event on contact delete failure`() {
      val expectedException = RuntimeException("Bang!")

      whenever(syncContactService.deleteContact(any())).thenThrow(expectedException)
      whenever(outboundEventsService.send(any(), any(), any(), any(), any(), any(), any())).then {}

      val exception = assertThrows<RuntimeException> {
        facade.deleteContact(1L)
      }

      assertThat(exception.message).isEqualTo(expectedException.message)
      verify(syncContactService).deleteContact(1L)
      verify(outboundEventsService, never()).send(any(), any(), any(), any(), any(), any(), any())
    }

    private fun createSyncContactRequest(personId: Long) = SyncCreateContactRequest(
      personId = personId,
      firstName = "John",
      lastName = "Doe",
      dateOfBirth = LocalDate.of(1980, 1, 1),
      createdBy = "ANYONE",
    )

    private fun contactResponse(contactId: Long) = SyncContact(
      id = contactId,
      firstName = "John",
      lastName = "Doe",
      createdBy = "ANYONE",
      createdTime = LocalDateTime.now(),
      updatedBy = null,
      updatedTime = null,
    )

    private fun updateContactSyncRequest() = SyncUpdateContactRequest(
      firstName = "John",
      lastName = "Doe",
      updatedBy = "ANYONE",
      updatedTime = LocalDateTime.now(),
    )
  }

  @Nested
  inner class ContactPhoneSyncFacadeEvents {
    @Test
    fun `should send domain event on contact phone create success`() {
      val request = createContactPhoneRequest()
      val response = contactPhoneResponse(contactId = 1L, contactPhoneId = 1L)

      whenever(syncContactPhoneService.createContactPhone(any())).thenReturn(response)
      whenever(outboundEventsService.send(any(), any(), any(), any(), any(), any(), any())).then {}

      val result = facade.createContactPhone(request)

      verify(syncContactPhoneService).createContactPhone(request)
      verify(outboundEventsService).send(
        outboundEvent = OutboundEvent.CONTACT_PHONE_CREATED,
        identifier = result.contactPhoneId,
        contactId = result.contactId,
        source = Source.NOMIS,
        user = User.SYS_USER,
      )
    }

    @Test
    fun `should send domain event on contact phone update success`() {
      val request = updateContactPhoneRequest()
      val response = contactPhoneResponse(contactId = 2L, contactPhoneId = 3L)

      whenever(syncContactPhoneService.updateContactPhone(any(), any())).thenReturn(response)
      whenever(outboundEventsService.send(any(), any(), any(), any(), any(), any(), any())).then {}

      val result = facade.updateContactPhone(3L, request)

      verify(syncContactPhoneService).updateContactPhone(3L, request)
      verify(outboundEventsService).send(
        outboundEvent = OutboundEvent.CONTACT_PHONE_UPDATED,
        identifier = result.contactPhoneId,
        contactId = result.contactId,
        source = Source.NOMIS,
        user = User.SYS_USER,
      )
    }

    @Test
    fun `should send domain event on contact phone delete success`() {
      val response = contactPhoneResponse(contactId = 3L, contactPhoneId = 4L)

      whenever(syncContactPhoneService.deleteContactPhone(any())).thenReturn(response)
      whenever(outboundEventsService.send(any(), any(), any(), any(), any(), any(), any())).then {}

      val result = facade.deleteContactPhone(4L)

      verify(syncContactPhoneService).deleteContactPhone(4L)

      verify(outboundEventsService).send(
        outboundEvent = OutboundEvent.CONTACT_PHONE_DELETED,
        identifier = result.contactPhoneId,
        contactId = result.contactId,
        source = Source.NOMIS,
        user = User.SYS_USER,
      )
    }

    private fun createContactPhoneRequest() = SyncCreateContactPhoneRequest(
      contactId = 1L,
      phoneType = "MOB",
      phoneNumber = "0909 111222",
      createdBy = "CREATOR",
      createdTime = LocalDateTime.now(),
    )

    private fun updateContactPhoneRequest() = SyncUpdateContactPhoneRequest(
      contactId = 2L,
      phoneType = "MOB",
      phoneNumber = "0909 111222",
      updatedBy = "UPDATER",
      updatedTime = LocalDateTime.now(),
    )
    private fun contactPhoneResponse(contactId: Long, contactPhoneId: Long) = SyncContactPhone(
      contactPhoneId = contactPhoneId,
      contactId = contactId,
      phoneType = "MOB",
      phoneNumber = "0909 111222",
      extNumber = null,
      createdBy = "CREATOR",
      createdTime = LocalDateTime.now(),
      updatedBy = null,
      updatedTime = null,
    )
  }

  @Nested
  inner class ContactEmailSyncFacadeEvents {
    @Test
    fun `should send domain event on contact email create success`() {
      val request = createContactEmailRequest()
      val response = contactEmailResponse(contactId = 1L, contactEmailId = 1L)

      whenever(syncContactEmailService.createContactEmail(any())).thenReturn(response)
      whenever(outboundEventsService.send(any(), any(), any(), any(), any(), any(), any())).then {}

      val result = facade.createContactEmail(request)

      verify(syncContactEmailService).createContactEmail(request)
      verify(outboundEventsService).send(
        outboundEvent = OutboundEvent.CONTACT_EMAIL_CREATED,
        identifier = result.contactEmailId,
        contactId = result.contactId,
        source = Source.NOMIS,
        user = User.SYS_USER,
      )
    }

    @Test
    fun `should send domain event on contact email update success`() {
      val request = updateContactEmailRequest()
      val response = contactEmailResponse(contactId = 2L, contactEmailId = 3L)

      whenever(syncContactEmailService.updateContactEmail(any(), any())).thenReturn(response)
      whenever(outboundEventsService.send(any(), any(), any(), any(), any(), any(), any())).then {}

      val result = facade.updateContactEmail(3L, request)

      verify(syncContactEmailService).updateContactEmail(3L, request)
      verify(outboundEventsService).send(
        outboundEvent = OutboundEvent.CONTACT_EMAIL_UPDATED,
        identifier = result.contactEmailId,
        contactId = result.contactId,
        source = Source.NOMIS,
        user = User.SYS_USER,
      )
    }

    @Test
    fun `should send domain event on contact email delete success`() {
      val response = contactEmailResponse(contactId = 3L, contactEmailId = 4L)

      whenever(syncContactEmailService.deleteContactEmail(any())).thenReturn(response)
      whenever(outboundEventsService.send(any(), any(), any(), any(), any(), any(), any())).then {}

      val result = facade.deleteContactEmail(4L)

      verify(syncContactEmailService).deleteContactEmail(4L)

      verify(outboundEventsService).send(
        outboundEvent = OutboundEvent.CONTACT_EMAIL_DELETED,
        identifier = result.contactEmailId,
        contactId = result.contactId,
        source = Source.NOMIS,
        user = User.SYS_USER,
      )
    }

    private fun createContactEmailRequest() = SyncCreateContactEmailRequest(
      contactId = 1L,
      emailAddress = "0909 111222",
      createdBy = "CREATOR",
      createdTime = LocalDateTime.now(),
    )

    private fun updateContactEmailRequest() = SyncUpdateContactEmailRequest(
      contactId = 2L,
      emailAddress = "test@test.com",
      updatedBy = "UPDATER",
      updatedTime = LocalDateTime.now(),
    )
    private fun contactEmailResponse(contactId: Long, contactEmailId: Long) = SyncContactEmail(
      contactEmailId = contactEmailId,
      contactId = contactId,
      emailAddress = "test@test.com",
      createdBy = "CREATOR",
      createdTime = LocalDateTime.now(),
      updatedBy = null,
      updatedTime = null,
    )
  }

  @Nested
  inner class ContactIdentitySyncFacadeEvents {
    @Test
    fun `should send domain event on contact identity create success`() {
      val request = createContactIdentityRequest()
      val response = contactIdentityResponse(contactId = 1L, contactIdentityId = 1L)

      whenever(syncContactIdentityService.createContactIdentity(any())).thenReturn(response)
      whenever(outboundEventsService.send(any(), any(), any(), any(), any(), any(), any())).then {}

      val result = facade.createContactIdentity(request)

      verify(syncContactIdentityService).createContactIdentity(request)
      verify(outboundEventsService).send(
        outboundEvent = OutboundEvent.CONTACT_IDENTITY_CREATED,
        identifier = result.contactIdentityId,
        contactId = result.contactId,
        source = Source.NOMIS,
        user = User.SYS_USER,
      )
    }

    @Test
    fun `should send domain event on contact identity update success`() {
      val request = updateContactIdentityRequest()
      val response = contactIdentityResponse(contactId = 2L, contactIdentityId = 3L)

      whenever(syncContactIdentityService.updateContactIdentity(any(), any())).thenReturn(response)
      whenever(outboundEventsService.send(any(), any(), any(), any(), any(), any(), any())).then {}

      val result = facade.updateContactIdentity(3L, request)

      verify(syncContactIdentityService).updateContactIdentity(3L, request)
      verify(outboundEventsService).send(
        outboundEvent = OutboundEvent.CONTACT_IDENTITY_UPDATED,
        identifier = result.contactIdentityId,
        contactId = result.contactId,
        source = Source.NOMIS,
        user = User.SYS_USER,
      )
    }

    @Test
    fun `should send domain event on contact identity delete success`() {
      val response = contactIdentityResponse(contactId = 3L, contactIdentityId = 4L)

      whenever(syncContactIdentityService.deleteContactIdentity(any())).thenReturn(response)
      whenever(outboundEventsService.send(any(), any(), any(), any(), any(), any(), any())).then {}

      val result = facade.deleteContactIdentity(4L)

      verify(syncContactIdentityService).deleteContactIdentity(4L)

      verify(outboundEventsService).send(
        outboundEvent = OutboundEvent.CONTACT_IDENTITY_DELETED,
        identifier = result.contactIdentityId,
        contactId = result.contactId,
        source = Source.NOMIS,
        user = User.SYS_USER,
      )
    }

    private fun createContactIdentityRequest() = SyncCreateContactIdentityRequest(
      contactId = 1L,
      identityType = "PASS",
      identityValue = "PW12345678",
      issuingAuthority = "Passport Office",
      createdBy = "CREATOR",
      createdTime = LocalDateTime.now(),
    )

    private fun updateContactIdentityRequest() = SyncUpdateContactIdentityRequest(
      contactId = 2L,
      identityType = "PASS",
      identityValue = "PW12345678",
      issuingAuthority = "Passport Office",
      updatedBy = "UPDATER",
      updatedTime = LocalDateTime.now(),
    )
    private fun contactIdentityResponse(contactId: Long, contactIdentityId: Long) = SyncContactIdentity(
      contactIdentityId = contactIdentityId,
      contactId = contactId,
      identityType = "PASS",
      identityValue = "PW12345678",
      issuingAuthority = "Passport Office",
      createdBy = "CREATOR",
      createdTime = LocalDateTime.now(),
      updatedBy = null,
      updatedTime = null,
    )
  }

  @Nested
  inner class ContactRestrictionSyncFacadeEvents {
    @Test
    fun `should send domain event on contact restriction create success`() {
      val request = createContactRestrictionRequest()
      val response = contactRestrictionResponse(contactId = 1L, contactRestrictionId = 1L)

      whenever(syncContactRestrictionService.createContactRestriction(any())).thenReturn(response)
      whenever(outboundEventsService.send(any(), any(), any(), any(), any(), any(), any())).then {}

      val result = facade.createContactRestriction(request)

      verify(syncContactRestrictionService).createContactRestriction(request)
      verify(outboundEventsService).send(
        outboundEvent = OutboundEvent.CONTACT_RESTRICTION_CREATED,
        identifier = result.contactRestrictionId,
        contactId = result.contactId,
        source = Source.NOMIS,
        user = User.SYS_USER,
      )
    }

    @Test
    fun `should send domain event on contact restriction update success`() {
      val request = updateContactRestrictionRequest()
      val response = contactRestrictionResponse(contactId = 2L, contactRestrictionId = 3L)

      whenever(syncContactRestrictionService.updateContactRestriction(any(), any())).thenReturn(response)
      whenever(outboundEventsService.send(any(), any(), any(), any(), any(), any(), any())).then {}

      val result = facade.updateContactRestriction(3L, request)

      verify(syncContactRestrictionService).updateContactRestriction(3L, request)
      verify(outboundEventsService).send(
        outboundEvent = OutboundEvent.CONTACT_RESTRICTION_UPDATED,
        identifier = result.contactRestrictionId,
        contactId = result.contactId,
        source = Source.NOMIS,
        user = User.SYS_USER,
      )
    }

    @Test
    fun `should send domain event on contact restriction delete success`() {
      val response = contactRestrictionResponse(contactId = 3L, contactRestrictionId = 4L)

      whenever(syncContactRestrictionService.deleteContactRestriction(any())).thenReturn(response)
      whenever(outboundEventsService.send(any(), any(), any(), any(), any(), any(), any())).then {}

      val result = facade.deleteContactRestriction(4L)

      verify(syncContactRestrictionService).deleteContactRestriction(4L)

      verify(outboundEventsService).send(
        outboundEvent = OutboundEvent.CONTACT_RESTRICTION_DELETED,
        identifier = result.contactRestrictionId,
        contactId = result.contactId,
        source = Source.NOMIS,
        user = User.SYS_USER,
      )
    }

    private fun createContactRestrictionRequest() = SyncCreateContactRestrictionRequest(
      contactId = 2L,
      restrictionType = "BAN",
      startDate = LocalDate.now().minusDays(10),
      expiryDate = LocalDate.now().plusDays(10),
      comments = "Comment",
      createdBy = "CREATOR",
      createdTime = LocalDateTime.now(),
    )

    private fun updateContactRestrictionRequest() = SyncUpdateContactRestrictionRequest(
      contactId = 2L,
      restrictionType = "BAN",
      startDate = LocalDate.now().minusDays(10),
      expiryDate = LocalDate.now().plusDays(10),
      comments = "Comment",
      updatedBy = "UPDATER",
      updatedTime = LocalDateTime.now(),
    )
    private fun contactRestrictionResponse(contactId: Long, contactRestrictionId: Long) = SyncContactRestriction(
      contactRestrictionId = contactRestrictionId,
      contactId = contactId,
      restrictionType = "BAN",
      startDate = LocalDate.now().minusDays(10),
      expiryDate = LocalDate.now().plusDays(10),
      comments = "Comment",
      createdBy = "CREATOR",
      createdTime = LocalDateTime.now(),
      updatedBy = null,
      updatedTime = null,
    )
  }

  @Nested
  inner class ContactAddressSyncFacadeEvents {
    @Test
    fun `should send domain event on contact address create success`() {
      val request = createContactAddressRequest()
      val response = contactAddressResponse(contactId = 1L, contactAddressId = 1L)

      whenever(syncContactAddressService.createContactAddress(any())).thenReturn(response)
      whenever(outboundEventsService.send(any(), any(), any(), any(), any(), any(), any())).then {}

      val result = facade.createContactAddress(request)

      verify(syncContactAddressService).createContactAddress(request)
      verify(outboundEventsService).send(
        outboundEvent = OutboundEvent.CONTACT_ADDRESS_CREATED,
        identifier = result.contactAddressId,
        contactId = result.contactId,
        source = Source.NOMIS,
        user = User.SYS_USER,
      )
    }

    @Test
    fun `should send domain event on contact address update success`() {
      val request = updateContactAddressRequest()
      val response = contactAddressResponse(contactId = 2L, contactAddressId = 3L)

      whenever(syncContactAddressService.updateContactAddress(any(), any())).thenReturn(response)
      whenever(outboundEventsService.send(any(), any(), any(), any(), any(), any(), any())).then {}

      val result = facade.updateContactAddress(3L, request)

      verify(syncContactAddressService).updateContactAddress(3L, request)
      verify(outboundEventsService).send(
        outboundEvent = OutboundEvent.CONTACT_ADDRESS_UPDATED,
        identifier = result.contactAddressId,
        contactId = result.contactId,
        source = Source.NOMIS,
        user = User.SYS_USER,
      )
    }

    @Test
    fun `should send domain event on contact address delete success`() {
      val response = contactAddressResponse(contactId = 3L, contactAddressId = 4L)

      whenever(syncContactAddressService.deleteContactAddress(any())).thenReturn(response)
      whenever(outboundEventsService.send(any(), any(), any(), any(), any(), any(), any())).then {}

      val result = facade.deleteContactAddress(4L)

      verify(syncContactAddressService).deleteContactAddress(4L)

      verify(outboundEventsService).send(
        outboundEvent = OutboundEvent.CONTACT_ADDRESS_DELETED,
        identifier = result.contactAddressId,
        contactId = result.contactId,
        source = Source.NOMIS,
        user = User.SYS_USER,
      )
    }

    private fun createContactAddressRequest() = SyncCreateContactAddressRequest(
      contactId = 1L,
      addressType = "HOME",
      property = "24",
      street = "Acacia Avenue",
      area = "Dunstan",
      comments = "Comment",
      createdBy = "CREATOR",
      createdTime = LocalDateTime.now(),
    )

    private fun updateContactAddressRequest() = SyncUpdateContactAddressRequest(
      contactId = 2L,
      primaryAddress = false,
      addressType = "HOME",
      property = "24",
      street = "Acacia Avenue",
      area = "Dunstan",
      comments = "Comment",
      updatedBy = "UPDATER",
      updatedTime = LocalDateTime.now(),
    )
    private fun contactAddressResponse(contactId: Long, contactAddressId: Long) = SyncContactAddress(
      contactAddressId = contactAddressId,
      contactId = contactId,
      primaryAddress = false,
      addressType = "HOME",
      property = "24",
      street = "Acacia Avenue",
      area = "Dunstan",
      comments = "Comment",
      createdBy = "CREATOR",
      createdTime = LocalDateTime.now(),
      updatedBy = null,
      updatedTime = null,
    )
  }

  @Nested
  inner class ContactAddressPhoneSyncFacadeEvents {
    @Test
    fun `should send domain event on contact address phone create success`() {
      val request = createContactAddressPhoneRequest()
      val response = contactAddressPhoneResponse()

      whenever(syncContactAddressPhoneService.createContactAddressPhone(any())).thenReturn(response)
      whenever(outboundEventsService.send(any(), any(), any(), any(), any(), any(), any())).then {}

      val result = facade.createContactAddressPhone(request)

      verify(syncContactAddressPhoneService).createContactAddressPhone(request)
      verify(outboundEventsService).send(
        outboundEvent = OutboundEvent.CONTACT_ADDRESS_PHONE_CREATED,
        identifier = result.contactAddressPhoneId,
        contactId = result.contactId,
        source = Source.NOMIS,
        secondIdentifier = result.contactAddressId,
        user = User.SYS_USER,
      )
    }

    @Test
    fun `should send domain event on contact address phone update success`() {
      val request = updateContactAddressPhoneRequest()
      val response = contactAddressPhoneResponse()

      whenever(syncContactAddressPhoneService.updateContactAddressPhone(any(), any())).thenReturn(response)
      whenever(outboundEventsService.send(any(), any(), any(), any(), any(), any(), any())).then {}

      val result = facade.updateContactAddressPhone(4L, request)

      verify(syncContactAddressPhoneService).updateContactAddressPhone(4L, request)
      verify(outboundEventsService).send(
        outboundEvent = OutboundEvent.CONTACT_ADDRESS_PHONE_UPDATED,
        identifier = result.contactAddressPhoneId,
        contactId = result.contactId,
        source = Source.NOMIS,
        secondIdentifier = result.contactAddressId,
        user = User.SYS_USER,
      )
    }

    @Test
    fun `should send domain event on contact address phone delete success`() {
      val response = contactAddressPhoneResponse()

      whenever(syncContactAddressPhoneService.deleteContactAddressPhone(any())).thenReturn(response)
      whenever(outboundEventsService.send(any(), any(), any(), any(), any(), any(), any())).then {}

      val result = facade.deleteContactAddressPhone(4L)

      verify(syncContactAddressPhoneService).deleteContactAddressPhone(4L)

      verify(outboundEventsService).send(
        outboundEvent = OutboundEvent.CONTACT_ADDRESS_PHONE_DELETED,
        identifier = result.contactAddressPhoneId,
        contactId = result.contactId,
        source = Source.NOMIS,
        secondIdentifier = result.contactAddressId,
        user = User.SYS_USER,
      )
    }

    private fun createContactAddressPhoneRequest() = SyncCreateContactAddressPhoneRequest(
      contactAddressId = 3L,
      phoneType = "MOB",
      phoneNumber = "0909 111222",
      extNumber = null,
      createdBy = "CREATOR",
      createdTime = LocalDateTime.now(),
    )

    private fun updateContactAddressPhoneRequest() = SyncUpdateContactAddressPhoneRequest(
      phoneType = "MOB",
      phoneNumber = "0909 111222",
      extNumber = null,
      updatedBy = "UPDATER",
      updatedTime = LocalDateTime.now(),
    )
    private fun contactAddressPhoneResponse() = SyncContactAddressPhone(
      contactAddressPhoneId = 4L,
      contactAddressId = 3L,
      contactPhoneId = 2L,
      contactId = 1L,
      phoneType = "MOB",
      phoneNumber = "0909 111222",
      extNumber = null,
      createdBy = "CREATOR",
      createdTime = LocalDateTime.now(),
      updatedBy = null,
      updatedTime = null,
    )
  }

  @Nested
  inner class PrisonerContactSyncFacadeEvents {
    @Test
    fun `should send domain event on prisoner contact create success`() {
      val request = createPrisonerContactRequest()
      val response = prisonerContactResponse(contactId = 1L, prisonerContactId = 1L)

      whenever(syncPrisonerContactService.createPrisonerContact(any())).thenReturn(response)
      whenever(outboundEventsService.send(any(), any(), any(), any(), any(), any(), any())).then {}

      val result = facade.createPrisonerContact(request)

      verify(syncPrisonerContactService).createPrisonerContact(request)
      verify(outboundEventsService).send(
        outboundEvent = OutboundEvent.PRISONER_CONTACT_CREATED,
        identifier = result.id,
        contactId = result.contactId,
        noms = result.prisonerNumber,
        source = Source.NOMIS,
        user = User.SYS_USER,
      )
    }

    @Test
    fun `should send domain event on prisoner contact update success`() {
      val request = updatePrisonerContactRequest()
      val response = prisonerContactResponse(contactId = 2L, prisonerContactId = 3L)

      whenever(syncPrisonerContactService.updatePrisonerContact(any(), any())).thenReturn(response)
      whenever(outboundEventsService.send(any(), any(), any(), any(), any(), any(), any())).then {}

      val result = facade.updatePrisonerContact(3L, request)

      verify(syncPrisonerContactService).updatePrisonerContact(3L, request)
      verify(outboundEventsService).send(
        outboundEvent = OutboundEvent.PRISONER_CONTACT_UPDATED,
        identifier = result.id,
        contactId = result.contactId,
        noms = result.prisonerNumber,
        source = Source.NOMIS,
        user = User.SYS_USER,
      )
    }

    @Test
    fun `should send domain event on prisoner contact delete success`() {
      val response = prisonerContactResponse(contactId = 3L, prisonerContactId = 4L)

      whenever(syncPrisonerContactService.deletePrisonerContact(any())).thenReturn(response)
      whenever(outboundEventsService.send(any(), any(), any(), any(), any(), any(), any())).then {}

      val result = facade.deletePrisonerContact(4L)

      verify(syncPrisonerContactService).deletePrisonerContact(4L)

      verify(outboundEventsService).send(
        outboundEvent = OutboundEvent.PRISONER_CONTACT_DELETED,
        identifier = result.id,
        contactId = result.contactId,
        noms = result.prisonerNumber,
        source = Source.NOMIS,
        user = User.SYS_USER,
      )
    }

    private fun createPrisonerContactRequest() = SyncCreatePrisonerContactRequest(
      contactId = 1L,
      contactType = "S",
      relationshipType = "MOT",
      prisonerNumber = "A1234AA",
      approvedVisitor = false,
      nextOfKin = false,
      emergencyContact = false,
      comments = "Comment",
      active = true,
      currentTerm = true,
      createdBy = "CREATOR",
      createdTime = LocalDateTime.now(),
    )

    private fun updatePrisonerContactRequest() = SyncUpdatePrisonerContactRequest(
      contactId = 2L,
      contactType = "S",
      relationshipType = "MOT",
      prisonerNumber = "A1234AA",
      approvedVisitor = false,
      nextOfKin = false,
      emergencyContact = false,
      comments = "Comment",
      active = true,
      currentTerm = true,
      updatedBy = "UPDATER",
      updatedTime = LocalDateTime.now(),
    )
    private fun prisonerContactResponse(contactId: Long, prisonerContactId: Long) = SyncPrisonerContact(
      id = prisonerContactId,
      contactId = contactId,
      contactType = "S",
      relationshipType = "MOT",
      prisonerNumber = "A1234AA",
      approvedVisitor = false,
      nextOfKin = false,
      emergencyContact = false,
      comments = "Comment",
      createdBy = "CREATOR",
      createdTime = LocalDateTime.now(),
      active = true,
      currentTerm = true,
      updatedBy = null,
      updatedTime = null,
    )
  }

  @Nested
  inner class PrisonerContactRestrictionSyncFacadeEvents {
    @Test
    fun `should send domain event on prisoner contact restriction create success`() {
      val request = createPrisonerContactRestrictionRequest()
      val response = prisonerContactRestrictionResponse()

      whenever(syncPrisonerContactRestrictionService.createPrisonerContactRestriction(any())).thenReturn(response)
      whenever(outboundEventsService.send(any(), any(), any(), any(), any(), any(), any())).then {}

      val result = facade.createPrisonerContactRestriction(request)

      verify(syncPrisonerContactRestrictionService).createPrisonerContactRestriction(request)
      verify(outboundEventsService).send(
        outboundEvent = OutboundEvent.PRISONER_CONTACT_RESTRICTION_CREATED,
        identifier = result.prisonerContactRestrictionId,
        contactId = result.contactId,
        noms = result.prisonerNumber,
        source = Source.NOMIS,
        user = User.SYS_USER,
      )
    }

    @Test
    fun `should send domain event on prisoner contact restriction update success`() {
      val request = updatePrisonerContactRestrictionRequest()
      val response = prisonerContactRestrictionResponse()

      whenever(syncPrisonerContactRestrictionService.updatePrisonerContactRestriction(any(), any())).thenReturn(response)
      whenever(outboundEventsService.send(any(), any(), any(), any(), any(), any(), any())).then {}

      val result = facade.updatePrisonerContactRestriction(3L, request)

      verify(syncPrisonerContactRestrictionService).updatePrisonerContactRestriction(3L, request)
      verify(outboundEventsService).send(
        outboundEvent = OutboundEvent.PRISONER_CONTACT_RESTRICTION_UPDATED,
        identifier = result.prisonerContactRestrictionId,
        contactId = result.contactId,
        noms = result.prisonerNumber,
        source = Source.NOMIS,
        user = User.SYS_USER,
      )
    }

    @Test
    fun `should send domain event on prisoner contact restriction delete success`() {
      val response = prisonerContactRestrictionResponse()

      whenever(syncPrisonerContactRestrictionService.deletePrisonerContactRestriction(any())).thenReturn(response)
      whenever(outboundEventsService.send(any(), any(), any(), any(), any(), any(), any())).then {}

      val result = facade.deletePrisonerContactRestriction(3L)

      verify(syncPrisonerContactRestrictionService).deletePrisonerContactRestriction(3L)

      verify(outboundEventsService).send(
        outboundEvent = OutboundEvent.PRISONER_CONTACT_RESTRICTION_DELETED,
        identifier = result.prisonerContactRestrictionId,
        contactId = result.contactId,
        noms = result.prisonerNumber,
        source = Source.NOMIS,
        user = User.SYS_USER,
      )
    }

    private fun createPrisonerContactRestrictionRequest() = SyncCreatePrisonerContactRestrictionRequest(
      prisonerContactId = 2L,
      restrictionType = "BAN",
      startDate = LocalDate.now().minusDays(10),
      expiryDate = LocalDate.now().plusDays(10),
      comments = "Not allowed to visit. Ever.",
      createdBy = "CREATOR",
      createdTime = LocalDateTime.now(),
    )

    private fun updatePrisonerContactRestrictionRequest() = SyncUpdatePrisonerContactRestrictionRequest(
      restrictionType = "BAN",
      startDate = LocalDate.now().minusDays(10),
      expiryDate = LocalDate.now().plusDays(10),
      comments = "Not allowed to visit. Ever.",
      updatedBy = "UPDATER",
      updatedTime = LocalDateTime.now(),
    )
    private fun prisonerContactRestrictionResponse() = SyncPrisonerContactRestriction(
      prisonerContactRestrictionId = 3L,
      prisonerContactId = 2L,
      contactId = 1L,
      prisonerNumber = "A1234AA",
      restrictionType = "BAN",
      startDate = LocalDate.now().minusDays(10),
      expiryDate = LocalDate.now().plusDays(10),
      comments = "Not allowed to visit. Ever.",
      createdBy = "CREATOR",
      createdTime = LocalDateTime.now(),
      updatedBy = null,
      updatedTime = null,
    )
  }

  @Nested
  inner class EmploymentSyncFacadeEvents {
    @Test
    fun `should send domain event on employment create success`() {
      val request = createEmploymentSyncRequest()
      val response = employmentSyncResponse()

      whenever(syncEmploymentService.createEmployment(any())).thenReturn(response)
      whenever(outboundEventsService.send(any(), any(), any(), any(), any(), any(), any())).then {}

      val result = facade.createEmployment(request)

      verify(syncEmploymentService).createEmployment(request)
      verify(outboundEventsService).send(
        outboundEvent = OutboundEvent.EMPLOYMENT_CREATED,
        identifier = result.employmentId,
        contactId = result.contactId,
        source = Source.NOMIS,
        user = User.SYS_USER,
      )
    }

    @Test
    fun `should send domain event on employment update success`() {
      val request = updateEmploymentRequest()
      val response = employmentSyncResponse()

      whenever(syncEmploymentService.updateEmployment(any(), any())).thenReturn(response)
      whenever(outboundEventsService.send(any(), any(), any(), any(), any(), any(), any())).then {}

      val result = facade.updateEmployment(3L, request)

      verify(syncEmploymentService).updateEmployment(3L, request)
      verify(outboundEventsService).send(
        outboundEvent = OutboundEvent.EMPLOYMENT_UPDATED,
        identifier = result.employmentId,
        contactId = result.contactId,
        source = Source.NOMIS,
        user = User.SYS_USER,
      )
    }

    @Test
    fun `should send domain event on employment delete success`() {
      val response = employmentSyncResponse()

      whenever(syncEmploymentService.deleteEmployment(any())).thenReturn(response)
      whenever(outboundEventsService.send(any(), any(), any(), any(), any(), any(), any())).then {}

      val result = facade.deleteEmployment(3L)

      verify(syncEmploymentService).deleteEmployment(3L)

      verify(outboundEventsService).send(
        outboundEvent = OutboundEvent.EMPLOYMENT_DELETED,
        identifier = result.employmentId,
        contactId = result.contactId,
        source = Source.NOMIS,
        user = User.SYS_USER,
      )
    }

    private fun createEmploymentSyncRequest() = SyncCreateEmploymentRequest(
      organisationId = 2L,
      contactId = 2L,
      active = true,
      createdBy = "CREATOR",
      createdTime = LocalDateTime.now(),
    )

    private fun updateEmploymentRequest() = SyncUpdateEmploymentRequest(
      organisationId = 2L,
      contactId = 2L,
      active = true,
      updatedBy = "UPDATER",
      updatedTime = LocalDateTime.now(),
    )
    private fun employmentSyncResponse() = SyncEmployment(
      employmentId = 2L,
      organisationId = 2L,
      contactId = 2L,
      active = true,
      createdBy = "CREATOR",
      createdTime = LocalDateTime.now(),
      updatedBy = null,
      updatedTime = null,
    )
  }
}
