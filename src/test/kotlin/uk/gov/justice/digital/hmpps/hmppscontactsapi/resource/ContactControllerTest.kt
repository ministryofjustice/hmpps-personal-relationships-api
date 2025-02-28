package uk.gov.justice.digital.hmpps.hmppscontactsapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.kotlin.whenever
import org.openapitools.jackson.nullable.JsonNullable
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.hmppscontactsapi.facade.ContactFacade
import uk.gov.justice.digital.hmpps.hmppscontactsapi.helpers.createContactAddressDetails
import uk.gov.justice.digital.hmpps.hmppscontactsapi.helpers.createContactEmailDetails
import uk.gov.justice.digital.hmpps.hmppscontactsapi.helpers.createContactIdentityDetails
import uk.gov.justice.digital.hmpps.hmppscontactsapi.helpers.createContactPhoneNumberDetails
import uk.gov.justice.digital.hmpps.hmppscontactsapi.helpers.createPrisonerContactRelationshipDetails
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.ContactSearchRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.CreateContactRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.PatchContactRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.ContactCreationResult
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.ContactDetails
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.ContactSearchResultItem
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.PatchContactResponse
import java.net.URI
import java.time.LocalDate
import java.time.LocalDateTime

class ContactControllerTest {

  private val contactFacade: ContactFacade = mock()
  private val controller = ContactController(contactFacade)

  @Nested
  inner class CreateContact {
    @Test
    fun `should create a contact successfully`() {
      val request = CreateContactRequest(
        lastName = "last",
        firstName = "first",
        createdBy = "created",
      )
      val createdContact = ContactDetails(
        id = 99,
        lastName = request.lastName,
        firstName = request.firstName,
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
        createdBy = request.createdBy,
        createdTime = LocalDateTime.now(),
      )
      val createdRelationship = createPrisonerContactRelationshipDetails(id = 123456)
      val expected = ContactCreationResult(createdContact, createdRelationship)
      whenever(contactFacade.createContact(request)).thenReturn(expected)

      val response = controller.createContact(request)

      assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
      assertThat(response.body).isEqualTo(expected)
      assertThat(response.headers.location).isEqualTo(URI.create("/contact/99"))
      verify(contactFacade).createContact(request)
    }

    @Test
    fun `should propagate exceptions creating a contact`() {
      val request = CreateContactRequest(
        lastName = "last",
        firstName = "first",
        createdBy = "created",
      )
      whenever(contactFacade.createContact(request)).thenThrow(RuntimeException("Bang!"))

      assertThrows<RuntimeException>("Bang!") {
        controller.createContact(request)
      }
    }
  }

  @Nested
  inner class GetContact {
    private val id = 123456L
    private val contact = ContactDetails(
      id = id,
      lastName = "last",
      firstName = "first",
      deceasedDate = null,
      languageCode = null,
      languageDescription = null,
      interpreterRequired = false,
      addresses = listOf(createContactAddressDetails()),
      phoneNumbers = listOf(createContactPhoneNumberDetails()),
      emailAddresses = listOf(createContactEmailDetails()),
      identities = listOf(createContactIdentityDetails()),
      employments = emptyList(),
      domesticStatusCode = null,
      domesticStatusDescription = null,
      genderCode = null,
      genderDescription = null,
      createdBy = "user",
      createdTime = LocalDateTime.now(),
    )

    @Test
    fun `should get a contact successfully`() {
      whenever(contactFacade.getContact(id)).thenReturn(contact)

      val response = controller.getContact(id)

      assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
      assertThat(response.body).isEqualTo(contact)
      verify(contactFacade).getContact(id)
    }

    @Test
    fun `should return 404 if contact not found`() {
      whenever(contactFacade.getContact(id)).thenReturn(null)

      val response = controller.getContact(id)

      assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
      verify(contactFacade).getContact(id)
    }

    @Test
    fun `should propagate exceptions getting a contact`() {
      whenever(contactFacade.getContact(id)).thenThrow(RuntimeException("Bang!"))

      assertThrows<RuntimeException>("Bang!") {
        controller.getContact(id)
      }
    }
  }

  @Nested
  inner class SearchContact {

    @Test
    fun `test searchContacts with surname ,forename ,middle and date of birth`() {
      // Given
      val pageable = PageRequest.of(0, 10)
      val contactEntities = listOf(
        getContact(),
      )
      val pageContacts = PageImpl(contactEntities, pageable, contactEntities.size.toLong())

      // When
      whenever(
        contactFacade.searchContacts(
          pageable,
          ContactSearchRequest("last", "first", "middle", LocalDate.of(1980, 1, 1)),
        ),
      ).thenReturn(pageContacts)

      // Act
      val result: Page<ContactSearchResultItem> = controller.searchContacts(pageable, ContactSearchRequest("last", "first", "middle", LocalDate.of(1980, 1, 1)))

      // Then
      assertNotNull(result)
      assertThat(result.totalElements).isEqualTo(1)
      assertThat(result.content[0].lastName).isEqualTo("last")
      assertThat(result.content[0].firstName).isEqualTo("first")
      assertThat(result.content[0].mailAddress).isEqualTo(true)
      assertThat(result.content[0].noFixedAddress).isEqualTo(true)
    }
  }

  @Nested
  inner class PatchContact {
    private val id = 123456L
    private val contact = patchContactResponse(id)

    @Test
    fun `should patch a contact successfully`() {
      val request = patchContactRequest()
      whenever(contactFacade.patch(id, request)).thenReturn(contact)

      val result = controller.patchContact(id, request)

      assertThat(result).isEqualTo(contact)
    }

    @Test
    fun `should return 404 if contact not found`() {
      val request = patchContactRequest()
      whenever(contactFacade.patch(id, request)).thenReturn(null)

      val response = controller.patchContact(id, request)

      assertThat(response).isEqualTo(null)
    }

    @Test
    fun `should propagate exceptions getting a contact`() {
      val request = patchContactRequest()
      whenever(contactFacade.patch(id, request)).thenThrow(RuntimeException("Bang!"))

      assertThrows<RuntimeException>("Bang!") {
        controller.patchContact(id, request)
      }
    }

    private fun patchContactRequest() = PatchContactRequest(
      languageCode = JsonNullable.of("ENG"),
      updatedBy = "system",
    )

    private fun patchContactResponse(id: Long) = PatchContactResponse(
      id = id,
      titleCode = "MR",
      lastName = "Doe",
      firstName = "John",
      middleNames = "William",
      dateOfBirth = LocalDate.of(1980, 1, 1),
      isStaff = false,
      deceasedDate = null,
      genderCode = "Male",
      domesticStatusCode = "Single",
      languageCode = "EN",
      interpreterRequired = false,
      createdBy = "JD000001",
      createdTime = LocalDateTime.now(),
      updatedBy = "UPDATE",
      updatedTime = LocalDateTime.now(),
    )
  }

  private fun getContact() = ContactSearchResultItem(
    id = 1L,
    lastName = "last",
    firstName = "first",
    middleNames = "first",
    dateOfBirth = LocalDate.of(1980, 2, 1),
    createdBy = "user",
    createdTime = LocalDateTime.now(),
    flat = "user",
    street = "user",
    area = "user",
    postCode = "user",
    mailAddress = true,
    noFixedAddress = true,
    comments = "Some comments",
  )
}
