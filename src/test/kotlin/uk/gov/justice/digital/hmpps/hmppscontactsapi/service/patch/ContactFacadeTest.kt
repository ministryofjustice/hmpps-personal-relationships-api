package uk.gov.justice.digital.hmpps.hmppscontactsapi.service.patch

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import uk.gov.justice.digital.hmpps.hmppscontactsapi.facade.ContactFacade
import uk.gov.justice.digital.hmpps.hmppscontactsapi.helpers.createContactAddressDetails
import uk.gov.justice.digital.hmpps.hmppscontactsapi.helpers.createContactEmailDetails
import uk.gov.justice.digital.hmpps.hmppscontactsapi.helpers.createContactIdentityDetails
import uk.gov.justice.digital.hmpps.hmppscontactsapi.helpers.createContactPhoneNumberDetails
import uk.gov.justice.digital.hmpps.hmppscontactsapi.helpers.createPrisonerContactRelationshipDetails
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.AddContactRelationshipRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.ContactRelationship
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.ContactSearchRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.CreateContactRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.PatchContactRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.PatchRelationshipRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.ContactCreationResult
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.ContactDetails
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.ContactSearchResultItem
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.PatchContactResponse
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.ContactPatchService
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.ContactService
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.OutboundEventsService
import java.time.LocalDateTime

class ContactFacadeTest {

  private val outboundEventsService: OutboundEventsService = mock()
  private val contactPatchService: ContactPatchService = mock()
  private val contactService: ContactService = mock()

  private val contactFacade = ContactFacade(outboundEventsService, contactPatchService, contactService)

  @Test
  fun `patch should patch contact and send domain event`() {
    val contactId = 1L
    val request = mock(PatchContactRequest::class.java)
    val response = mock(PatchContactResponse::class.java)

    whenever(contactPatchService.patch(contactId, request)).thenReturn(response)

    val result = contactFacade.patch(contactId, request)

    assertThat(response).isEqualTo(result)
    verify(contactPatchService).patch(contactId, request)
    verify(outboundEventsService).send(OutboundEvent.CONTACT_UPDATED, contactId, contactId)
  }

  @Test
  fun `create contact without relationship should send contact domain event only`() {
    val request = CreateContactRequest(
      lastName = "last",
      firstName = "first",
      createdBy = "created",
    )
    val createdContact = aContactDetails().copy(id = 98765)
    val expected = ContactCreationResult(createdContact, null)
    whenever(contactService.createContact(request)).thenReturn(expected)

    val result = contactFacade.createContact(request)

    assertThat(result).isEqualTo(expected)
    verify(outboundEventsService).send(OutboundEvent.CONTACT_CREATED, createdContact.id, createdContact.id)
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
      createdBy = "created",
    )
    val createdContact = aContactDetails().copy(id = 98765)
    val createdRelationship = createPrisonerContactRelationshipDetails(id = 123456)
    val expected = ContactCreationResult(createdContact, createdRelationship)
    whenever(contactService.createContact(request)).thenReturn(expected)

    val result = contactFacade.createContact(request)

    assertThat(result).isEqualTo(expected)
    verify(outboundEventsService).send(OutboundEvent.CONTACT_CREATED, createdContact.id, createdContact.id)
    verify(outboundEventsService).send(OutboundEvent.PRISONER_CONTACT_CREATED, 123456, createdContact.id, "A1234BC")
  }

  @Test
  fun `create contact relationship should send prisoner contact domain event`() {
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
      "user",
    )
    val prisonerContactId = 123456L
    val createdRelationship = createPrisonerContactRelationshipDetails(id = prisonerContactId)
    whenever(contactService.addContactRelationship(request)).thenReturn(createdRelationship)

    contactFacade.addContactRelationship(request)

    verify(outboundEventsService).send(OutboundEvent.PRISONER_CONTACT_CREATED, prisonerContactId, 99, "A1234BC")
  }

  @Test
  fun `search should send no domain event`() {
    val pageable = Pageable.unpaged()
    val request = ContactSearchRequest(lastName = "foo", firstName = null, middleNames = null, dateOfBirth = null)
    val result = PageImpl<ContactSearchResultItem>(listOf())

    whenever(contactService.searchContacts(any(), any())).thenReturn(result)

    assertThat(contactFacade.searchContacts(pageable, request)).isEqualTo(result)

    verify(outboundEventsService, never()).send(any(), any(), any(), any(), any(), any())
  }

  @Test
  fun `get by id should send no domain event`() {
    val expectedContact = aContactDetails()

    whenever(contactService.getContact(any())).thenReturn(expectedContact)

    assertThat(contactFacade.getContact(99L)).isEqualTo(expectedContact)

    verify(outboundEventsService, never()).send(any(), any(), any(), any(), any(), any())
  }

  @Test
  fun `patch relationship should send domain event`() {
    val contactId = 321L
    val prisonerContactId = 123L
    val prisonerNumber = "A1234BC"
    val request = mock(PatchRelationshipRequest::class.java)

    whenever(contactService.updateContactRelationship(prisonerContactId, request)).thenReturn(
      createPrisonerContactRelationshipDetails(prisonerContactId, contactId, prisonerNumber),
    )

    contactFacade.patchRelationship(prisonerContactId, request)

    verify(contactService).updateContactRelationship(prisonerContactId, request)
    verify(outboundEventsService).send(OutboundEvent.PRISONER_CONTACT_UPDATED, prisonerContactId, contactId, prisonerNumber)
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
