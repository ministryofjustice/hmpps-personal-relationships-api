package uk.gov.justice.digital.hmpps.personalrelationships.service.patch

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PagedModel
import uk.gov.justice.digital.hmpps.personalrelationships.facade.ContactFacade
import uk.gov.justice.digital.hmpps.personalrelationships.helpers.aUser
import uk.gov.justice.digital.hmpps.personalrelationships.helpers.createContactAddressDetails
import uk.gov.justice.digital.hmpps.personalrelationships.helpers.createContactAddressPhoneDetails
import uk.gov.justice.digital.hmpps.personalrelationships.helpers.createContactEmailDetails
import uk.gov.justice.digital.hmpps.personalrelationships.helpers.createContactIdentityDetails
import uk.gov.justice.digital.hmpps.personalrelationships.helpers.createContactPhoneNumberDetails
import uk.gov.justice.digital.hmpps.personalrelationships.helpers.createEmploymentDetails
import uk.gov.justice.digital.hmpps.personalrelationships.helpers.createPrisonerContactRelationshipDetails
import uk.gov.justice.digital.hmpps.personalrelationships.model.internal.DeletedRelationshipIds
import uk.gov.justice.digital.hmpps.personalrelationships.model.internal.DeletedResponse
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.AddContactRelationshipRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.ContactRelationship
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.ContactSearchRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.CreateContactRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.PatchContactRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.PatchRelationshipRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.ContactCreationResult
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.ContactDetails
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.ContactSearchResultItem
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.PatchContactResponse
import uk.gov.justice.digital.hmpps.personalrelationships.service.ContactPatchService
import uk.gov.justice.digital.hmpps.personalrelationships.service.ContactSearchService
import uk.gov.justice.digital.hmpps.personalrelationships.service.ContactService
import uk.gov.justice.digital.hmpps.personalrelationships.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.personalrelationships.service.events.OutboundEventsService
import uk.gov.justice.digital.hmpps.personalrelationships.service.events.Source
import java.time.LocalDateTime

class ContactFacadeTest {

  private val outboundEventsService: OutboundEventsService = mock()
  private val contactPatchService: ContactPatchService = mock()
  private val contactService: ContactService = mock()
  private val contactSearchService: ContactSearchService = mock()

  private val contactFacade = ContactFacade(outboundEventsService, contactPatchService, contactService, contactSearchService)

  @Test
  fun `patch should patch contact and send domain event`() {
    val user = aUser("updated")
    val contactId = 1L
    val request = mock(PatchContactRequest::class.java)
    val response = mock(PatchContactResponse::class.java)

    whenever(contactPatchService.patch(contactId, request, user)).thenReturn(response)

    val result = contactFacade.patch(contactId, request, user)

    assertThat(response).isEqualTo(result)
    verify(contactPatchService).patch(contactId, request, user)
    verify(outboundEventsService).send(OutboundEvent.CONTACT_UPDATED, contactId, contactId, user = user)
  }

  @Nested
  inner class CreateContact {
    private val user = aUser("created")

    @Test
    fun `create contact without relationship should send contact domain event only`() {
      val request = CreateContactRequest(
        lastName = "last",
        firstName = "first",
      )
      val createdContact = aContactDetails().copy(id = 98765)
      val expected = ContactCreationResult(createdContact, null)
      whenever(contactService.createContact(request, user)).thenReturn(expected)

      val result = contactFacade.createContact(request, user)

      assertThat(result).isEqualTo(expected)
      verify(outboundEventsService).send(OutboundEvent.CONTACT_CREATED, createdContact.id, createdContact.id, user = user)
    }

    @Test
    fun `create contact with relationship should send contact and prisoner contact domain events`() {
      val request = CreateContactRequest(
        lastName = "last",
        firstName = "first",
        relationship = ContactRelationship(
          prisonerNumber = "A1234BC",
          relationshipToPrisonerCode = "FRI",
          isNextOfKin = false,
          isEmergencyContact = false,
          relationshipTypeCode = "S",
          isApprovedVisitor = false,
          comments = null,
        ),
      )
      val createdContact = aContactDetails().copy(id = 98765)
      val createdRelationship = createPrisonerContactRelationshipDetails(id = 123456)
      val expected = ContactCreationResult(createdContact, createdRelationship)
      whenever(contactService.createContact(request, user)).thenReturn(expected)

      val result = contactFacade.createContact(request, user)

      assertThat(result).isEqualTo(expected)
      verify(outboundEventsService).send(OutboundEvent.CONTACT_CREATED, createdContact.id, createdContact.id, user = user)
      verify(outboundEventsService).send(OutboundEvent.PRISONER_CONTACT_CREATED, 123456, createdContact.id, "A1234BC", user = user)
    }

    @Test
    fun `create contact without identities should send contact domain event only`() {
      val request = CreateContactRequest(
        lastName = "last",
        firstName = "first",
      )
      val createdContact = aContactDetails().copy(id = 98765, identities = emptyList())
      val expected = ContactCreationResult(createdContact, null)
      whenever(contactService.createContact(request, user)).thenReturn(expected)

      val result = contactFacade.createContact(request, user)

      assertThat(result).isEqualTo(expected)
      verify(outboundEventsService).send(OutboundEvent.CONTACT_CREATED, createdContact.id, createdContact.id, user = user)
      verify(outboundEventsService, never()).send(
        outboundEvent = OutboundEvent.CONTACT_IDENTITY_CREATED,
        identifier = 1L,
        contactId = createdContact.id,
        user = user,
      )
    }

    @Test
    fun `create contact with multiple identities should send identity created event`() {
      val request = CreateContactRequest(
        lastName = "last",
        firstName = "first",
      )
      val createdContact = aContactDetails().copy(id = 98765)
      val expected = ContactCreationResult(createdContact, null)
      whenever(contactService.createContact(request, user)).thenReturn(expected)

      val result = contactFacade.createContact(request, user)

      assertThat(result).isEqualTo(expected)
      verify(outboundEventsService).send(OutboundEvent.CONTACT_CREATED, createdContact.id, createdContact.id, user = user)

      result.createdContact.identities.forEach { identityId ->
        verify(outboundEventsService).send(
          outboundEvent = OutboundEvent.CONTACT_IDENTITY_CREATED,
          identifier = identityId.contactIdentityId,
          contactId = createdContact.id,
          user = user,
        )
      }
    }

    @Test
    fun `create contact with multiple address should send address and address phone created events`() {
      val request = CreateContactRequest(
        lastName = "last",
        firstName = "first",
      )
      val createdContact = aContactDetails().copy(
        id = 98765,
        addresses = listOf(
          createContactAddressDetails(
            id = 123456,
            contactId = 98765,
            phoneNumbers = listOf(
              createContactAddressPhoneDetails(
                contactId = 98765,
                contactAddressId = 123456,
                contactPhoneId = 999999,
                contactAddressPhoneId = 987654,
              ),
            ),
          ),
        ),
      )
      val expected = ContactCreationResult(createdContact, null)
      whenever(contactService.createContact(request, user)).thenReturn(expected)

      val result = contactFacade.createContact(request, user)

      assertThat(result).isEqualTo(expected)
      verify(outboundEventsService).send(OutboundEvent.CONTACT_CREATED, createdContact.id, createdContact.id, user = user)
      verify(outboundEventsService).send(
        outboundEvent = OutboundEvent.CONTACT_ADDRESS_CREATED,
        identifier = 123456,
        contactId = createdContact.id,
        user = user,
      )
      verify(outboundEventsService).send(
        outboundEvent = OutboundEvent.CONTACT_ADDRESS_PHONE_CREATED,
        identifier = 987654,
        secondIdentifier = 123456,
        contactId = createdContact.id,
        user = user,
      )
    }

    @Test
    fun `create contact with multiple phone numbers should send phone created events`() {
      val request = CreateContactRequest(
        lastName = "last",
        firstName = "first",
      )
      val createdContact = aContactDetails().copy(
        id = 98765,
        phoneNumbers = listOf(createContactPhoneNumberDetails(id = 999), createContactPhoneNumberDetails(id = 777)),
      )
      val expected = ContactCreationResult(createdContact, null)
      whenever(contactService.createContact(request, user)).thenReturn(expected)

      val result = contactFacade.createContact(request, user)

      assertThat(result).isEqualTo(expected)
      verify(outboundEventsService).send(OutboundEvent.CONTACT_CREATED, createdContact.id, createdContact.id, user = user)
      verify(outboundEventsService).send(
        outboundEvent = OutboundEvent.CONTACT_PHONE_CREATED,
        identifier = 999,
        contactId = createdContact.id,
        user = user,
      )
      verify(outboundEventsService).send(
        outboundEvent = OutboundEvent.CONTACT_PHONE_CREATED,
        identifier = 777,
        contactId = createdContact.id,
        user = user,
      )
    }

    @Test
    fun `create contact with multiple email addresses should send email address created events`() {
      val request = CreateContactRequest(
        lastName = "last",
        firstName = "first",
      )
      val createdContact = aContactDetails().copy(
        id = 98765,
        emailAddresses = listOf(createContactEmailDetails(id = 999), createContactEmailDetails(id = 777)),
      )
      val expected = ContactCreationResult(createdContact, null)
      whenever(contactService.createContact(request, user)).thenReturn(expected)

      val result = contactFacade.createContact(request, user)

      assertThat(result).isEqualTo(expected)
      verify(outboundEventsService).send(OutboundEvent.CONTACT_CREATED, createdContact.id, createdContact.id, user = user)
      verify(outboundEventsService).send(
        outboundEvent = OutboundEvent.CONTACT_EMAIL_CREATED,
        identifier = 999,
        contactId = createdContact.id,
        user = user,
      )
      verify(outboundEventsService).send(
        outboundEvent = OutboundEvent.CONTACT_EMAIL_CREATED,
        identifier = 777,
        contactId = createdContact.id,
        user = user,
      )
    }

    @Test
    fun `create contact with multiple employments should send employment created events`() {
      val request = CreateContactRequest(
        lastName = "last",
        firstName = "first",
      )
      val createdContact = aContactDetails().copy(
        id = 98765,
        employments = listOf(createEmploymentDetails(id = 999), createEmploymentDetails(id = 777)),
      )
      val expected = ContactCreationResult(createdContact, null)
      whenever(contactService.createContact(request, user)).thenReturn(expected)

      val result = contactFacade.createContact(request, user)

      assertThat(result).isEqualTo(expected)
      verify(outboundEventsService).send(OutboundEvent.CONTACT_CREATED, createdContact.id, createdContact.id, user = user)
      verify(outboundEventsService).send(
        outboundEvent = OutboundEvent.EMPLOYMENT_CREATED,
        identifier = 999,
        contactId = createdContact.id,
        user = user,
      )
      verify(outboundEventsService).send(
        outboundEvent = OutboundEvent.EMPLOYMENT_CREATED,
        identifier = 777,
        contactId = createdContact.id,
        user = user,
      )
    }
  }

  @Test
  fun `create contact relationship should send prisoner contact domain event`() {
    val user = aUser("created")
    val request = AddContactRelationshipRequest(
      99,
      ContactRelationship(
        prisonerNumber = "A1234BC",
        relationshipToPrisonerCode = "FRI",
        isNextOfKin = false,
        relationshipTypeCode = "S",
        isEmergencyContact = false,
        isApprovedVisitor = false,
        comments = null,
      ),
    )
    val prisonerContactId = 123456L
    val createdRelationship = createPrisonerContactRelationshipDetails(id = prisonerContactId)
    whenever(contactService.addContactRelationship(request, user)).thenReturn(createdRelationship)

    contactFacade.addContactRelationship(request, user)

    verify(outboundEventsService).send(OutboundEvent.PRISONER_CONTACT_CREATED, prisonerContactId, 99, "A1234BC", user = user)
  }

  @Test
  fun `search should send no domain event`() {
    val pageable = Pageable.unpaged()
    val request = ContactSearchRequest(lastName = "foo", firstName = null, middleNames = null, "123456", dateOfBirth = null, includeAnyExistingRelationshipsToPrisoner = null)
    val result = PageImpl<ContactSearchResultItem>(listOf())

    whenever(contactSearchService.searchContacts(any(), any())).thenReturn(result)

    assertThat(contactFacade.searchContacts(pageable, request)).isEqualTo(PagedModel(result))

    verify(outboundEventsService, never()).send(any(), any(), any(), any(), any(), any(), any())
  }

  @Test
  fun `get by id should send no domain event`() {
    val expectedContact = aContactDetails()

    whenever(contactService.getContact(any())).thenReturn(expectedContact)

    assertThat(contactFacade.getContact(99L)).isEqualTo(expectedContact)

    verify(outboundEventsService, never()).send(any(), any(), any(), any(), any(), any(), any())
  }

  @Test
  fun `patch relationship should send domain event`() {
    val contactId = 321L
    val prisonerContactId = 123L
    val prisonerNumber = "A1234BC"
    val request = mock(PatchRelationshipRequest::class.java)
    val user = aUser("updated")

    whenever(contactService.updateContactRelationship(prisonerContactId, request, user)).thenReturn(
      createPrisonerContactRelationshipDetails(prisonerContactId, contactId, prisonerNumber),
    )

    contactFacade.patchRelationship(prisonerContactId, request, user)

    verify(contactService).updateContactRelationship(prisonerContactId, request, user)
    verify(outboundEventsService).send(
      OutboundEvent.PRISONER_CONTACT_UPDATED,
      prisonerContactId,
      contactId,
      prisonerNumber,
      user = user,
    )
  }

  @Nested
  inner class DeleteContactRelationship {
    @Test
    fun `delete contact relationship sends event on success`() {
      val contactId = 321L
      val prisonerContactId = 123L
      val prisonerNumber = "A1234BC"
      val user = aUser("deleted")

      whenever(contactService.deleteContactRelationship(prisonerContactId, user)).thenReturn(
        DeletedResponse(
          ids = DeletedRelationshipIds(contactId, prisonerNumber, prisonerContactId),
          wasUpdated = true,
        ),
      )

      contactFacade.deleteContactRelationship(prisonerContactId, user)

      verify(contactService).deleteContactRelationship(prisonerContactId, user)
      verify(outboundEventsService).send(
        OutboundEvent.PRISONER_CONTACT_DELETED,
        prisonerContactId,
        contactId,
        prisonerNumber,
        user = user,
      )
      // Also sends contact updated event as wasUpdated = true
      verify(outboundEventsService).send(
        OutboundEvent.CONTACT_UPDATED,
        identifier = contactId,
        contactId = contactId,
        source = Source.DPS,
        user = user,
      )
    }

    @Test
    fun `delete contact relationship does not send event on failure`() {
      val contactId = 321L
      val prisonerContactId = 123L
      val prisonerNumber = "A1234BC"
      val user = aUser("deleted")
      val expectedException = RuntimeException("Boom")

      whenever(contactService.deleteContactRelationship(prisonerContactId, user)).thenThrow(expectedException)

      val exception = assertThrows<RuntimeException> {
        contactFacade.deleteContactRelationship(prisonerContactId, user)
      }

      assertThat(exception).isEqualTo(expectedException)
      verify(contactService).deleteContactRelationship(prisonerContactId, user)
      verify(outboundEventsService, never()).send(
        OutboundEvent.PRISONER_CONTACT_DELETED,
        prisonerContactId,
        contactId,
        prisonerNumber,
        user = user,
      )

      verify(outboundEventsService, never()).send(
        OutboundEvent.CONTACT_UPDATED,
        identifier = contactId,
        contactId = contactId,
        source = Source.DPS,
        user = user,
      )
    }

    @Test
    fun `delete contact relationship does not send update contact event if was update is false`() {
      val contactId = 321L
      val prisonerContactId = 123L
      val prisonerNumber = "A1234BC"
      val user = aUser("deleted")

      whenever(contactService.deleteContactRelationship(prisonerContactId, user)).thenReturn(
        DeletedResponse(
          ids = DeletedRelationshipIds(contactId, prisonerNumber, prisonerContactId),
          wasUpdated = false, // wasUpdated is false
        ),
      )

      contactFacade.deleteContactRelationship(prisonerContactId, user)

      verify(contactService).deleteContactRelationship(prisonerContactId, user)
      verify(outboundEventsService).send(
        OutboundEvent.PRISONER_CONTACT_DELETED,
        prisonerContactId,
        contactId,
        prisonerNumber,
        user = user,
      )
      // Also sends contact updated event as wasUpdated = true
      verify(outboundEventsService, never()).send(
        OutboundEvent.CONTACT_UPDATED,
        identifier = contactId,
        contactId = contactId,
        source = Source.DPS,
        user = user,
      )
    }
  }

  private fun aContactDetails() = ContactDetails(
    id = 99,
    lastName = "Last",
    firstName = "First",
    deceasedDate = null,
    languageCode = null,
    languageDescription = null,
    interpreterRequired = false,
    addresses = listOf(createContactAddressDetails()),
    phoneNumbers = listOf(createContactPhoneNumberDetails()),
    emailAddresses = listOf(createContactEmailDetails()),
    identities = listOf(createContactIdentityDetails()),
    employments = emptyList(),
    domesticStatusCode = "S",
    domesticStatusDescription = "Single",
    genderCode = null,
    genderDescription = null,
    createdBy = "user",
    createdTime = LocalDateTime.now(),
  )
}
