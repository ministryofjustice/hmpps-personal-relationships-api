package uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.SecureAPIIntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.CreateContactRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.address.Address
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.email.EmailAddress
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.employment.Employment
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.identity.IdentityDocument
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.phone.PhoneNumber
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.ContactDetails
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.ContactAddressInfo
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.ContactAddressPhoneInfo
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.ContactEmailInfo
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.ContactIdentityInfo
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.ContactInfo
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.ContactPhoneInfo
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.EmploymentInfo
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.PersonReference
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.Source
import uk.gov.justice.digital.hmpps.hmppscontactsapi.util.StubUser
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class CreateContactIntegrationTest : SecureAPIIntegrationTestBase() {

  @BeforeEach
  fun setUp() {
    setCurrentUser(StubUser.CREATING_USER)
  }

  override val allowedRoles: Set<String> = setOf("ROLE_CONTACTS_ADMIN", "ROLE_CONTACTS__RW")

  override fun baseRequestBuilder(): WebTestClient.RequestHeadersSpec<*> = webTestClient.post()
    .uri("/contact")
    .accept(MediaType.APPLICATION_JSON)
    .contentType(MediaType.APPLICATION_JSON)
    .bodyValue(aMinimalCreateContactRequest())

  @ParameterizedTest
  @CsvSource(
    value = [
      "firstName must not be null;{\"firstName\": null, \"lastName\": \"last\", \"createdBy\": \"created\"}",
      "firstName must not be null;{\"lastName\": \"last\", \"createdBy\": \"created\"}",
      "lastName must not be null;{\"firstName\": \"first\", \"lastName\": null, \"createdBy\": \"created\"}",
      "lastName must not be null;{\"firstName\": \"first\", \"createdBy\": \"created\"}",
    ],
    delimiter = ';',
  )
  fun `should return bad request if required fields are null`(expectedMessage: String, json: String) {
    val errors = webTestClient.post()
      .uri("/contact")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .headers(setAuthorisationUsingCurrentUser())
      .bodyValue(json)
      .exchange()
      .expectStatus()
      .isBadRequest
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody!!

    assertThat(errors.userMessage).isEqualTo("Validation failure: $expectedMessage")
    stubEvents.assertHasNoEvents(
      event = OutboundEvent.CONTACT_CREATED,
    )
  }

  @ParameterizedTest
  @MethodSource("allFieldConstraintViolations")
  fun `should enforce field constraints`(expectedMessage: String, request: CreateContactRequest) {
    val errors = webTestClient.post()
      .uri("/contact")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .headers(setAuthorisationUsingCurrentUser())
      .bodyValue(request)
      .exchange()
      .expectStatus()
      .isBadRequest
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody!!

    assertThat(errors.userMessage).isEqualTo("Validation failure(s): $expectedMessage")
    stubEvents.assertHasNoEvents(
      event = OutboundEvent.CONTACT_CREATED,
    )
  }

  @ParameterizedTest
  @MethodSource("allReferenceCodeViolations")
  fun `should enforce reference data`(expectedTypeDescription: String, request: CreateContactRequest) {
    val errors = webTestClient.post()
      .uri("/contact")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .headers(setAuthorisationUsingCurrentUser())
      .bodyValue(request)
      .exchange()
      .expectStatus()
      .isBadRequest
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody!!

    assertThat(errors.userMessage).isEqualTo("Validation failure: Unsupported $expectedTypeDescription (FOO)")
    stubEvents.assertHasNoEvents(
      event = OutboundEvent.CONTACT_CREATED,
    )
  }

  @ParameterizedTest
  @CsvSource(
    value = [
      "{\"firstName\": \"first\", \"lastName\": \"last\", \"createdBy\": \"created\", \"dateOfBirth\": \"1st Jan\"}",
      "{\"firstName\": \"first\", \"lastName\": \"last\", \"createdBy\": \"created\", \"dateOfBirth\": \"01-01-1970\"}",
    ],
    delimiter = ';',
  )
  fun `should handle invalid dob formats`(json: String) {
    val errors = webTestClient.post()
      .uri("/contact")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .headers(setAuthorisationUsingCurrentUser())
      .bodyValue(json)
      .exchange()
      .expectStatus()
      .isBadRequest
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody!!

    assertThat(errors.userMessage).isEqualTo("Validation failure: dateOfBirth could not be parsed as a date")
    stubEvents.assertHasNoEvents(
      event = OutboundEvent.CONTACT_CREATED,
    )
  }

  @Test
  fun `should validate PNC number`() {
    val expectedMessage = "Identity value (1923/1Z34567A) is not a valid PNC Number"
    val request = aMinimalCreateContactRequest().copy(
      identities = listOf(
        IdentityDocument(
          identityType = "PNC",
          identityValue = "1923/1Z34567A",
        ),
      ),
    )
    val errors = webTestClient.post()
      .uri("/contact")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .headers(setAuthorisationUsingCurrentUser())
      .bodyValue(request)
      .exchange()
      .expectStatus()
      .isBadRequest
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody!!

    assertThat(errors.userMessage).isEqualTo("Validation failure: $expectedMessage")
    stubEvents.assertHasNoEvents(
      event = OutboundEvent.CONTACT_IDENTITY_CREATED,
    )
  }

  @Test
  fun `should create the contact with minimal fields`() {
    val request = aMinimalCreateContactRequest()

    val contactReturnedOnCreate = testAPIClient.createAContact(request)

    assertContactsAreEqualExcludingTimestamps(contactReturnedOnCreate, request)
    val contactReturnedFromGet = testAPIClient.getContact(contactReturnedOnCreate.id)
    assertThat(contactReturnedOnCreate).usingRecursiveComparison()
      .ignoringFields("createdTime") // get a weird timestamp precision failure when run on linux
      .isEqualTo(contactReturnedFromGet)
    assertThat(contactReturnedOnCreate.createdTime.truncatedTo(ChronoUnit.SECONDS))
      .isEqualTo(contactReturnedFromGet.createdTime.truncatedTo(ChronoUnit.SECONDS))

    // Verify that a contact created locally has an ID in the appropriate range - above 20,000,000
    assertThat(contactReturnedOnCreate.id).isGreaterThanOrEqualTo(LOCAL_CONTACT_ID_SEQUENCE_MIN)

    stubEvents.assertHasEvent(
      event = OutboundEvent.CONTACT_CREATED,
      additionalInfo = ContactInfo(contactReturnedOnCreate.id, Source.DPS, "created"),
      personReference = PersonReference(dpsContactId = contactReturnedOnCreate.id),
    )
  }

  @Test
  fun `should create the contact with multiple identity documents`() {
    val identityDocuments = listOf(
      IdentityDocument(
        identityType = "DL",
        identityValue = "DL123456789",
      ),
      IdentityDocument(
        identityType = "PASS",
        identityValue = "P897654312",
      ),
    )
    val request = aMinimalCreateContactRequest(identityDocuments = identityDocuments)

    val contactReturned = testAPIClient.createAContactWithARelationship(request)

    val contactReturnedOnCreate = contactReturned.createdContact
    assertContactsAreEqualExcludingTimestamps(contactReturnedOnCreate, request)

    assertThat(contactReturned.createdContact.identities).hasSize(2)
    assertThat(contactReturned.createdContact.identities[0].identityType).isEqualTo("DL")
    assertThat(contactReturned.createdContact.identities[0].identityValue).isEqualTo("DL123456789")
    assertThat(contactReturned.createdContact.identities[1].identityType).isEqualTo("PASS")
    assertThat(contactReturned.createdContact.identities[1].identityValue).isEqualTo("P897654312")

    stubEvents.assertHasEvent(
      event = OutboundEvent.CONTACT_CREATED,
      additionalInfo = ContactInfo(contactReturnedOnCreate.id, Source.DPS, "created"),
      personReference = PersonReference(dpsContactId = contactReturnedOnCreate.id),
    )

    contactReturnedOnCreate.identities.forEach { identity ->
      stubEvents.assertHasEvent(
        event = OutboundEvent.CONTACT_IDENTITY_CREATED,
        additionalInfo = ContactIdentityInfo(identity.contactIdentityId, Source.DPS, "created"),
        personReference = PersonReference(dpsContactId = contactReturnedOnCreate.id),
      )
    }
  }

  @Test
  fun `should create the contact with multiple addresses`() {
    val minimalAddress = Address(
      addressType = "HOME",
      primaryAddress = false,
      flat = null,
      property = null,
      street = null,
      area = null,
      cityCode = null,
      countyCode = null,
      postcode = null,
      countryCode = "ENG",
      verified = false,
      mailFlag = false,
      startDate = LocalDate.of(2020, 2, 3),
      endDate = null,
      noFixedAddress = false,
      phoneNumbers = emptyList(),
      comments = null,
    )
    val addressWithEverything = Address(
      addressType = "BUS",
      primaryAddress = true,
      flat = "Flat",
      property = "Property",
      street = "Street",
      area = "Area",
      cityCode = "25343",
      countyCode = "S.YORKSHIRE",
      postcode = "POST CODE",
      countryCode = "ENG",
      verified = true,
      mailFlag = true,
      startDate = LocalDate.of(2020, 2, 3),
      endDate = LocalDate.of(2050, 4, 5),
      noFixedAddress = true,
      phoneNumbers = listOf(
        PhoneNumber("MOB", "012345789", extNumber = "#123"),
      ),
      comments = "Some comments",
    )
    val request = aMinimalCreateContactRequest().copy(addresses = listOf(minimalAddress, addressWithEverything))

    val contactReturned = testAPIClient.createAContactWithARelationship(request)
    val contactId = contactReturned.createdContact.id
    assertThat(contactReturned.createdContact.addresses).hasSize(2)
    val minimalCreated = contactReturned.createdContact.addresses.find { it.addressType == "HOME" }!!
    with(minimalCreated) {
      assertThat(addressType).isEqualTo("HOME")
      assertThat(addressTypeDescription).isEqualTo("Home address")
      assertThat(primaryAddress).isFalse()
      assertThat(flat).isNull()
      assertThat(property).isNull()
      assertThat(street).isNull()
      assertThat(area).isNull()
      assertThat(cityCode).isNull()
      assertThat(cityDescription).isNull()
      assertThat(countyCode).isNull()
      assertThat(countyDescription).isNull()
      assertThat(postcode).isNull()
      assertThat(countryCode).isEqualTo("ENG")
      assertThat(countryDescription).isEqualTo("England")
      assertThat(verified).isFalse()
      assertThat(mailFlag).isFalse()
      assertThat(startDate).isEqualTo(LocalDate.of(2020, 2, 3))
      assertThat(endDate).isNull()
      assertThat(noFixedAddress).isFalse()
      assertThat(phoneNumbers).isEmpty()
      assertThat(comments).isNull()
    }

    val everythingCreated = contactReturned.createdContact.addresses.find { it.addressType == "BUS" }!!
    with(everythingCreated) {
      assertThat(addressType).isEqualTo("BUS")
      assertThat(addressTypeDescription).isEqualTo("Business address")
      assertThat(primaryAddress).isTrue()
      assertThat(flat).isEqualTo("Flat")
      assertThat(property).isEqualTo("Property")
      assertThat(street).isEqualTo("Street")
      assertThat(area).isEqualTo("Area")
      assertThat(cityCode).isEqualTo("25343")
      assertThat(cityDescription).isEqualTo("Sheffield")
      assertThat(countyCode).isEqualTo("S.YORKSHIRE")
      assertThat(countyDescription).isEqualTo("South Yorkshire")
      assertThat(postcode).isEqualTo("POST CODE")
      assertThat(countryCode).isEqualTo("ENG")
      assertThat(countryDescription).isEqualTo("England")
      assertThat(verified).isTrue()
      assertThat(mailFlag).isTrue()
      assertThat(startDate).isEqualTo(LocalDate.of(2020, 2, 3))
      assertThat(endDate).isEqualTo(LocalDate.of(2050, 4, 5))
      assertThat(noFixedAddress).isTrue()
      assertThat(comments).isEqualTo("Some comments")
      assertThat(phoneNumbers).hasSize(1)
      assertThat(phoneNumbers[0].phoneType).isEqualTo("MOB")
      assertThat(phoneNumbers[0].phoneTypeDescription).isEqualTo("Mobile")
      assertThat(phoneNumbers[0].phoneNumber).isEqualTo("012345789")
      assertThat(phoneNumbers[0].extNumber).isEqualTo("#123")
    }

    stubEvents.assertHasEvent(
      event = OutboundEvent.CONTACT_CREATED,
      additionalInfo = ContactInfo(contactId, Source.DPS, "created"),
      personReference = PersonReference(dpsContactId = contactId),
    )

    stubEvents.assertHasEvent(
      event = OutboundEvent.CONTACT_ADDRESS_CREATED,
      additionalInfo = ContactAddressInfo(minimalCreated.contactAddressId, Source.DPS, "created"),
      personReference = PersonReference(dpsContactId = contactId),
    )

    stubEvents.assertHasEvent(
      event = OutboundEvent.CONTACT_ADDRESS_CREATED,
      additionalInfo = ContactAddressInfo(everythingCreated.contactAddressId, Source.DPS, "created"),
      personReference = PersonReference(dpsContactId = contactId),
    )

    stubEvents.assertHasEvent(
      event = OutboundEvent.CONTACT_ADDRESS_PHONE_CREATED,
      additionalInfo = ContactAddressPhoneInfo(
        everythingCreated.phoneNumbers[0].contactAddressPhoneId,
        everythingCreated.contactAddressId,
        Source.DPS,
      ),
      personReference = PersonReference(dpsContactId = contactId),
    )
  }

  @Test
  fun `should rollback if addresses or address phones are invalid`() {
    val addressWithInvalidPhone = Address(
      addressType = "HOME",
      primaryAddress = false,
      flat = null,
      property = null,
      street = null,
      area = null,
      cityCode = null,
      countyCode = null,
      postcode = null,
      countryCode = "ENG",
      verified = false,
      mailFlag = false,
      startDate = LocalDate.of(2020, 2, 3),
      endDate = null,
      noFixedAddress = false,
      phoneNumbers = listOf(
        PhoneNumber("FOO", "123456789"),
      ),
      comments = null,
    )
    val errors = webTestClient.post()
      .uri("/contact")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .headers(setAuthorisationUsingCurrentUser())
      .bodyValue(aMinimalCreateContactRequest().copy(addresses = listOf(addressWithInvalidPhone)))
      .exchange()
      .expectStatus()
      .isBadRequest
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody!!

    assertThat(errors.userMessage).isEqualTo("Validation failure: Unsupported phone type (FOO)")
    stubEvents.assertHasNoEvents(event = OutboundEvent.CONTACT_CREATED)
    stubEvents.assertHasNoEvents(event = OutboundEvent.CONTACT_ADDRESS_CREATED)
    stubEvents.assertHasNoEvents(event = OutboundEvent.CONTACT_ADDRESS_PHONE_CREATED)
  }

  @Test
  fun `should create the contact with multiple phone numbers`() {
    val minimalPhoneNumber = PhoneNumber("HOME", "123456")
    val phoneNumberWithEverything = PhoneNumber("MOB", "987654321", "#123")

    val request =
      aMinimalCreateContactRequest().copy(phoneNumbers = listOf(minimalPhoneNumber, phoneNumberWithEverything))

    val contactReturned = testAPIClient.createAContactWithARelationship(request)
    val contactId = contactReturned.createdContact.id
    assertThat(contactReturned.createdContact.phoneNumbers).hasSize(2)
    val minimalCreated = contactReturned.createdContact.phoneNumbers.find { it.phoneType == "HOME" }!!
    with(minimalCreated) {
      assertThat(phoneType).isEqualTo("HOME")
      assertThat(phoneTypeDescription).isEqualTo("Home")
      assertThat(phoneNumber).isEqualTo("123456")
      assertThat(extNumber).isNull()
    }

    val everythingCreated = contactReturned.createdContact.phoneNumbers.find { it.phoneType == "MOB" }!!
    with(everythingCreated) {
      assertThat(phoneType).isEqualTo("MOB")
      assertThat(phoneTypeDescription).isEqualTo("Mobile")
      assertThat(phoneNumber).isEqualTo("987654321")
      assertThat(extNumber).isEqualTo("#123")
    }

    stubEvents.assertHasEvent(
      event = OutboundEvent.CONTACT_CREATED,
      additionalInfo = ContactInfo(contactId, Source.DPS, "created"),
      personReference = PersonReference(dpsContactId = contactId),
    )

    stubEvents.assertHasEvent(
      event = OutboundEvent.CONTACT_PHONE_CREATED,
      additionalInfo = ContactPhoneInfo(minimalCreated.contactPhoneId, Source.DPS),
      personReference = PersonReference(dpsContactId = contactId),
    )

    stubEvents.assertHasEvent(
      event = OutboundEvent.CONTACT_PHONE_CREATED,
      additionalInfo = ContactPhoneInfo(everythingCreated.contactPhoneId, Source.DPS),
      personReference = PersonReference(dpsContactId = contactId),
    )
  }

  @Test
  fun `should rollback if phones are invalid`() {
    val invalidPhone = PhoneNumber(
      phoneType = "FOO",
      phoneNumber = "123456",
    )
    val errors = webTestClient.post()
      .uri("/contact")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .headers(setAuthorisationUsingCurrentUser())
      .bodyValue(aMinimalCreateContactRequest().copy(phoneNumbers = listOf(invalidPhone)))
      .exchange()
      .expectStatus()
      .isBadRequest
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody!!

    assertThat(errors.userMessage).isEqualTo("Validation failure: Unsupported phone type (FOO)")
    stubEvents.assertHasNoEvents(event = OutboundEvent.CONTACT_CREATED)
    stubEvents.assertHasNoEvents(event = OutboundEvent.CONTACT_PHONE_CREATED)
  }

  @Test
  fun `should create the contact with multiple email addresses`() {
    val request =
      aMinimalCreateContactRequest().copy(emailAddresses = listOf(EmailAddress("test@example.com"), EmailAddress("another@example.com")))

    val contactReturned = testAPIClient.createAContactWithARelationship(request)
    val contactId = contactReturned.createdContact.id
    assertThat(contactReturned.createdContact.emailAddresses).hasSize(2)
    val firstCreated = contactReturned.createdContact.emailAddresses.find { it.emailAddress == "test@example.com" }
    assertThat(firstCreated).isNotNull
    val secondCreated = contactReturned.createdContact.emailAddresses.find { it.emailAddress == "another@example.com" }
    assertThat(secondCreated).isNotNull

    stubEvents.assertHasEvent(
      event = OutboundEvent.CONTACT_CREATED,
      additionalInfo = ContactInfo(contactId, Source.DPS, "created"),
      personReference = PersonReference(dpsContactId = contactId),
    )

    stubEvents.assertHasEvent(
      event = OutboundEvent.CONTACT_EMAIL_CREATED,
      additionalInfo = ContactEmailInfo(firstCreated!!.contactEmailId, Source.DPS),
      personReference = PersonReference(dpsContactId = contactId),
    )

    stubEvents.assertHasEvent(
      event = OutboundEvent.CONTACT_EMAIL_CREATED,
      additionalInfo = ContactEmailInfo(secondCreated!!.contactEmailId, Source.DPS),
      personReference = PersonReference(dpsContactId = contactId),
    )
  }

  @Test
  fun `should rollback if emails are invalid`() {
    val invalid = EmailAddress(
      emailAddress = "FOO",
    )
    val errors = webTestClient.post()
      .uri("/contact")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .headers(setAuthorisationUsingCurrentUser())
      .bodyValue(aMinimalCreateContactRequest().copy(emailAddresses = listOf(invalid)))
      .exchange()
      .expectStatus()
      .isBadRequest
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody!!

    assertThat(errors.userMessage).isEqualTo("Validation failure: Email address is invalid")
    stubEvents.assertHasNoEvents(event = OutboundEvent.CONTACT_CREATED)
    stubEvents.assertHasNoEvents(event = OutboundEvent.CONTACT_EMAIL_CREATED)
  }

  @Test
  fun `should create the contact with multiple employments`() {
    stubOrganisationSummary(1, "First ltd.")
    stubOrganisationSummary(2, "Second ltd.")

    val request =
      aMinimalCreateContactRequest().copy(employments = listOf(Employment(1, true), Employment(2, false)))

    val contactReturned = testAPIClient.createAContactWithARelationship(request)
    val contactId = contactReturned.createdContact.id
    assertThat(contactReturned.createdContact.employments).hasSize(2)
    val firstCreated = contactReturned.createdContact.employments.find { it.employer.organisationId == 1L }
    assertThat(firstCreated).isNotNull
    assertThat(firstCreated!!.isActive).isTrue()
    val secondCreated = contactReturned.createdContact.employments.find { it.employer.organisationId == 2L }
    assertThat(secondCreated).isNotNull
    assertThat(secondCreated!!.isActive).isFalse()

    stubEvents.assertHasEvent(
      event = OutboundEvent.CONTACT_CREATED,
      additionalInfo = ContactInfo(contactId, Source.DPS, "created"),
      personReference = PersonReference(dpsContactId = contactId),
    )

    stubEvents.assertHasEvent(
      event = OutboundEvent.EMPLOYMENT_CREATED,
      additionalInfo = EmploymentInfo(firstCreated.employmentId, Source.DPS),
      personReference = PersonReference(dpsContactId = contactId),
    )

    stubEvents.assertHasEvent(
      event = OutboundEvent.EMPLOYMENT_CREATED,
      additionalInfo = EmploymentInfo(secondCreated.employmentId, Source.DPS),
      personReference = PersonReference(dpsContactId = contactId),
    )
  }

  @Test
  fun `should rollback if employments are invalid`() {
    stubOrganisationSummaryNotFound(1)
    val invalid = Employment(
      organisationId = 1,
      isActive = false,
    )
    val errors = webTestClient.post()
      .uri("/contact")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .headers(setAuthorisationUsingCurrentUser())
      .bodyValue(aMinimalCreateContactRequest().copy(employments = listOf(invalid)))
      .exchange()
      .expectStatus()
      .isNotFound
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody!!

    assertThat(errors.userMessage).isEqualTo("Entity not found : Organisation with id 1 not found")
    stubEvents.assertHasNoEvents(event = OutboundEvent.CONTACT_CREATED)
    stubEvents.assertHasNoEvents(event = OutboundEvent.EMPLOYMENT_CREATED)
  }

  @ParameterizedTest
  @ValueSource(strings = ["ROLE_CONTACTS_ADMIN", "ROLE_CONTACTS__RW"])
  fun `should create the contact with all fields`(role: String) {
    val request = CreateContactRequest(
      titleCode = "MR",
      lastName = "last",
      firstName = "first",
      middleNames = "middle",
      dateOfBirth = LocalDate.of(1982, 6, 15),
      isStaff = true,
      languageCode = "ENG",
      interpreterRequired = true,
      domesticStatusCode = "S",
      genderCode = "M",
    )

    val contact = testAPIClient.createAContact(request, role)

    assertContactsAreEqualExcludingTimestamps(contact, request)

    stubEvents.assertHasEvent(
      event = OutboundEvent.CONTACT_CREATED,
      additionalInfo = ContactInfo(contact.id, Source.DPS, "created"),
      personReference = PersonReference(contact.id),
    )
  }

  private fun assertContactsAreEqualExcludingTimestamps(contact: ContactDetails, request: CreateContactRequest) {
    with(contact) {
      assertThat(titleCode).isEqualTo(request.titleCode)
      assertThat(lastName).isEqualTo(request.lastName)
      assertThat(firstName).isEqualTo(request.firstName)
      assertThat(middleNames).isEqualTo(request.middleNames)
      assertThat(dateOfBirth).isEqualTo(request.dateOfBirth)
      assertThat(createdBy).isEqualTo("created")
      assertThat(isStaff).isEqualTo(request.isStaff)
      assertThat(languageCode).isEqualTo(request.languageCode)
      assertThat(interpreterRequired).isEqualTo(request.interpreterRequired)
      assertThat(domesticStatusCode).isEqualTo(request.domesticStatusCode)
      assertThat(genderCode).isEqualTo(request.genderCode)
    }
  }

  companion object {
    @JvmStatic
    fun allFieldConstraintViolations(): List<Arguments> {
      val minimalAddress = Address(
        addressType = "HOME",
        primaryAddress = false,
        flat = null,
        property = null,
        street = null,
        area = null,
        cityCode = null,
        countyCode = null,
        postcode = null,
        countryCode = "ENG",
        verified = false,
        mailFlag = false,
        startDate = LocalDate.of(2020, 2, 3),
        endDate = null,
        noFixedAddress = false,
        phoneNumbers = listOf(
          PhoneNumber("FOO", "123456789"),
        ),
        comments = null,
      )

      return listOf(
        Arguments.of(
          "titleCode must be <= 12 characters",
          aMinimalCreateContactRequest().copy(titleCode = "".padStart(13, 'X')),
        ),
        Arguments.of(
          "genderCode must be <= 12 characters",
          aMinimalCreateContactRequest().copy(genderCode = "".padStart(13, 'X')),
        ),
        Arguments.of(
          "languageCode must be <= 12 characters",
          aMinimalCreateContactRequest().copy(languageCode = "".padStart(13, 'X')),
        ),
        Arguments.of(
          "domesticStatusCode must be <= 12 characters",
          aMinimalCreateContactRequest().copy(domesticStatusCode = "".padStart(13, 'X')),
        ),
        Arguments.of(
          "lastName must be <= 35 characters",
          aMinimalCreateContactRequest().copy(lastName = "".padStart(36, 'X')),
        ),
        Arguments.of(
          "firstName must be <= 35 characters",
          aMinimalCreateContactRequest().copy(firstName = "".padStart(36, 'X')),
        ),
        Arguments.of(
          "middleNames must be <= 35 characters",
          aMinimalCreateContactRequest().copy(middleNames = "".padStart(36, 'X')),
        ),
        Arguments.of(
          "identities[0].identityType must be <= 12 characters",
          aMinimalCreateContactRequest().copy(
            identities = listOf(
              IdentityDocument(
                identityType = "".padStart(13, 'X'),
                identityValue = "DL123456789",
                issuingAuthority = "DVLA",
              ),
            ),
          ),
        ),
        Arguments.of(
          "identities[0].identityValue must be <= 20 characters",
          aMinimalCreateContactRequest().copy(
            identities = listOf(
              IdentityDocument(
                identityType = "PASS",
                identityValue = "".padStart(21, 'X'),
                issuingAuthority = "DVLA",
              ),
            ),
          ),
        ),
        Arguments.of(
          "identities[0].issuingAuthority must be <= 40 characters",
          aMinimalCreateContactRequest().copy(
            identities = listOf(
              IdentityDocument(
                identityType = "PASS",
                identityValue = "DL123456789",
                issuingAuthority = "".padStart(41, 'X'),
              ),
            ),
          ),
        ),
        Arguments.of(
          "addresses[0].flat must be <= 30 characters",
          aMinimalCreateContactRequest().copy(addresses = listOf(minimalAddress.copy(flat = "".padStart(31, 'X')))),
        ),
        Arguments.of(
          "addresses[0].property must be <= 50 characters",
          aMinimalCreateContactRequest().copy(addresses = listOf(minimalAddress.copy(property = "".padStart(51, 'X')))),
        ),
        Arguments.of(
          "addresses[0].street must be <= 160 characters",
          aMinimalCreateContactRequest().copy(addresses = listOf(minimalAddress.copy(street = "".padStart(161, 'X')))),
        ),
        Arguments.of(
          "addresses[0].area must be <= 70 characters",
          aMinimalCreateContactRequest().copy(addresses = listOf(minimalAddress.copy(area = "".padStart(71, 'X')))),
        ),
        Arguments.of(
          "addresses[0].postcode must be <= 12 characters",
          aMinimalCreateContactRequest().copy(addresses = listOf(minimalAddress.copy(postcode = "".padStart(13, 'X')))),
        ),
        Arguments.of(
          "addresses[0].comments must be <= 240 characters",
          aMinimalCreateContactRequest().copy(
            addresses = listOf(
              minimalAddress.copy(
                comments = "".padStart(
                  241,
                  'X',
                ),
              ),
            ),
          ),
        ),
        Arguments.of(
          "addresses[0].phoneNumbers[0].phoneNumber must be <= 40 characters",
          aMinimalCreateContactRequest().copy(
            addresses = listOf(
              minimalAddress.copy(
                phoneNumbers = listOf(
                  PhoneNumber(
                    phoneType = "MOB",
                    phoneNumber = "".padStart(41, 'X'),
                    extNumber = null,
                  ),
                ),
              ),
            ),
          ),
        ),
        Arguments.of(
          "addresses[0].phoneNumbers[0].phoneType must be <= 12 characters",
          aMinimalCreateContactRequest().copy(
            addresses = listOf(
              minimalAddress.copy(
                phoneNumbers = listOf(
                  PhoneNumber(
                    phoneType = "".padStart(13, 'X'),
                    phoneNumber = "07403322232",
                    extNumber = null,
                  ),
                ),
              ),
            ),
          ),
        ),
        Arguments.of(
          "addresses[0].phoneNumbers[0].extNumber must be <= 7 characters",
          aMinimalCreateContactRequest().copy(
            addresses = listOf(
              minimalAddress.copy(
                phoneNumbers = listOf(
                  PhoneNumber(
                    phoneType = "MOB",
                    phoneNumber = "07403322232",
                    extNumber = "".padStart(8, 'X'),
                  ),
                ),
              ),
            ),
          ),
        ),
        Arguments.of(
          "phoneNumbers[0].phoneNumber must be <= 40 characters",
          aMinimalCreateContactRequest().copy(
            phoneNumbers = listOf(
              PhoneNumber(
                phoneType = "MOB",
                phoneNumber = "".padStart(41, 'X'),
                extNumber = null,
              ),
            ),
          ),
        ),
        Arguments.of(
          "phoneNumbers[0].phoneType must be <= 12 characters",
          aMinimalCreateContactRequest().copy(
            phoneNumbers = listOf(
              PhoneNumber(
                phoneType = "".padStart(13, 'X'),
                phoneNumber = "07403322232",
                extNumber = null,
              ),
            ),
          ),
        ),
        Arguments.of(
          "phoneNumbers[0].extNumber must be <= 7 characters",
          aMinimalCreateContactRequest().copy(
            phoneNumbers = listOf(
              PhoneNumber(
                phoneType = "MOB",
                phoneNumber = "07403322232",
                extNumber = "".padStart(8, 'X'),
              ),
            ),
          ),
        ),
        Arguments.of(
          "emailAddresses[0].emailAddress must be <= 240 characters",
          aMinimalCreateContactRequest().copy(
            emailAddresses = listOf(
              EmailAddress(
                emailAddress = "@example.com".padStart(241, 'X'),
              ),
            ),
          ),
        ),
      )
    }

    @JvmStatic
    fun allReferenceCodeViolations(): List<Arguments> {
      val minimalAddress = Address(
        addressType = "HOME",
        primaryAddress = false,
        flat = null,
        property = null,
        street = null,
        area = null,
        cityCode = null,
        countyCode = null,
        postcode = null,
        countryCode = "ENG",
        verified = false,
        mailFlag = false,
        startDate = LocalDate.of(2020, 2, 3),
        endDate = null,
        noFixedAddress = false,
        phoneNumbers = listOf(
          PhoneNumber("FOO", "123456789"),
        ),
        comments = null,
      )
      return listOf(
        Arguments.of("title", aMinimalCreateContactRequest().copy(titleCode = "FOO")),
        Arguments.of("language", aMinimalCreateContactRequest().copy(languageCode = "FOO")),
        Arguments.of("domestic status", aMinimalCreateContactRequest().copy(domesticStatusCode = "FOO")),
        Arguments.of("gender", aMinimalCreateContactRequest().copy(genderCode = "FOO")),
        Arguments.of(
          "identity type",
          aMinimalCreateContactRequest().copy(
            identities = listOf(
              IdentityDocument(
                identityType = "DL",
                identityValue = "DL123456789",
                issuingAuthority = "DVLA",
              ),
              IdentityDocument(
                identityType = "FOO",
                identityValue = "P897654312",
                issuingAuthority = null,
              ),
            ),
          ),
        ),
        Arguments.of(
          "address type",
          aMinimalCreateContactRequest().copy(addresses = listOf(minimalAddress.copy(addressType = "FOO"))),
        ),
        Arguments.of(
          "city",
          aMinimalCreateContactRequest().copy(addresses = listOf(minimalAddress.copy(cityCode = "FOO"))),
        ),
        Arguments.of(
          "county",
          aMinimalCreateContactRequest().copy(addresses = listOf(minimalAddress.copy(countyCode = "FOO"))),
        ),
        Arguments.of(
          "country",
          aMinimalCreateContactRequest().copy(addresses = listOf(minimalAddress.copy(countryCode = "FOO"))),
        ),
        Arguments.of(
          "phone type",
          aMinimalCreateContactRequest().copy(phoneNumbers = listOf(PhoneNumber("FOO", "12456879"))),
        ),
      )
    }

    private fun aMinimalCreateContactRequest(identityDocuments: List<IdentityDocument> = emptyList()) = CreateContactRequest(
      lastName = "last",
      firstName = "first",
      identities = identityDocuments,
    )

    private const val LOCAL_CONTACT_ID_SEQUENCE_MIN: Long = 20000000L
  }
}
