package uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.resource

import org.assertj.core.api.Assertions.assertThat
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
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.identity.IdentityDocument
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.ContactDetails
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.ContactIdentityInfo
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.ContactInfo
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.PersonReference
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.Source
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class CreateContactIntegrationTest : SecureAPIIntegrationTestBase() {

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
      "createdBy must not be null;{\"firstName\": \"first\", \"lastName\": \"last\", \"createdBy\": null}",
      "createdBy must not be null;{\"firstName\": \"first\", \"lastName\": \"last\"}",
    ],
    delimiter = ';',
  )
  fun `should return bad request if required fields are null`(expectedMessage: String, json: String) {
    val errors = webTestClient.post()
      .uri("/contact")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CONTACTS_ADMIN")))
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
      .headers(setAuthorisation(roles = listOf("ROLE_CONTACTS_ADMIN")))
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
      .headers(setAuthorisation(roles = listOf("ROLE_CONTACTS_ADMIN")))
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
      .headers(setAuthorisation(roles = listOf("ROLE_CONTACTS_ADMIN")))
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
      .headers(setAuthorisation(roles = listOf("ROLE_CONTACTS_ADMIN")))
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
      additionalInfo = ContactInfo(contactReturnedOnCreate.id, Source.DPS),
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
      additionalInfo = ContactInfo(contactReturnedOnCreate.id, Source.DPS),
      personReference = PersonReference(dpsContactId = contactReturnedOnCreate.id),
    )

    contactReturnedOnCreate.identities.forEach { identity ->
      stubEvents.assertHasEvent(
        event = OutboundEvent.CONTACT_IDENTITY_CREATED,
        additionalInfo = ContactIdentityInfo(identity.contactIdentityId, Source.DPS),
        personReference = PersonReference(dpsContactId = contactReturnedOnCreate.id),
      )
    }
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
      createdBy = "created",
    )

    val contact = testAPIClient.createAContact(request, role)

    assertContactsAreEqualExcludingTimestamps(contact, request)

    stubEvents.assertHasEvent(
      event = OutboundEvent.CONTACT_CREATED,
      additionalInfo = ContactInfo(contact.id, Source.DPS),
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
      assertThat(createdBy).isEqualTo(request.createdBy)
      assertThat(isStaff).isEqualTo(request.isStaff)
      assertThat(languageCode).isEqualTo(request.languageCode)
      assertThat(interpreterRequired).isEqualTo(request.interpreterRequired)
      assertThat(domesticStatusCode).isEqualTo(request.domesticStatusCode)
      assertThat(genderCode).isEqualTo(request.genderCode)
    }
  }

  companion object {
    @JvmStatic
    fun allFieldConstraintViolations(): List<Arguments> = listOf(
      Arguments.of("titleCode must be <= 12 characters", aMinimalCreateContactRequest().copy(titleCode = "".padStart(13, 'X'))),
      Arguments.of("genderCode must be <= 12 characters", aMinimalCreateContactRequest().copy(genderCode = "".padStart(13, 'X'))),
      Arguments.of("languageCode must be <= 12 characters", aMinimalCreateContactRequest().copy(languageCode = "".padStart(13, 'X'))),
      Arguments.of("domesticStatusCode must be <= 12 characters", aMinimalCreateContactRequest().copy(domesticStatusCode = "".padStart(13, 'X'))),
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
        "createdBy must be <= 100 characters",
        aMinimalCreateContactRequest().copy(createdBy = "".padStart(101, 'X')),
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
    )

    @JvmStatic
    fun allReferenceCodeViolations(): List<Arguments> = listOf(
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

    )

    private fun aMinimalCreateContactRequest(identityDocuments: List<IdentityDocument> = emptyList()) = CreateContactRequest(
      lastName = "last",
      firstName = "first",
      createdBy = "created",
      identities = identityDocuments,
    )

    private const val LOCAL_CONTACT_ID_SEQUENCE_MIN: Long = 20000000L
  }
}
