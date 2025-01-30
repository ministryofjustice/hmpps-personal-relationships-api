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
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.CreateIdentityRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.ContactIdentityDetails
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.ContactIdentityInfo
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.PersonReference
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.Source
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

class CreateContactIdentityIntegrationTest : SecureAPIIntegrationTestBase() {
  private var savedContactId = 0L

  override val allowedRoles: Set<String> = setOf("ROLE_CONTACTS_ADMIN", "ROLE_CONTACTS__RW")

  @BeforeEach
  fun initialiseData() {
    savedContactId = testAPIClient.createAContact(
      CreateContactRequest(
        lastName = "identity",
        firstName = "has",
        createdBy = "created",
      ),
    ).id
  }

  override fun baseRequestBuilder(): WebTestClient.RequestHeadersSpec<*> = webTestClient.post()
    .uri("/contact/$savedContactId/identity")
    .accept(MediaType.APPLICATION_JSON)
    .contentType(MediaType.APPLICATION_JSON)
    .bodyValue(aMinimalRequest())

  @ParameterizedTest
  @CsvSource(
    value = [
      "identityType must not be null;{\"identityType\": null, \"identityValue\": \"0123456789\", \"createdBy\": \"created\"}",
      "identityType must not be null;{\"identityValue\": \"0123456789\", \"createdBy\": \"created\"}",
      "identityValue must not be null;{\"identityType\": \"DL\", \"identityValue\": null, \"createdBy\": \"created\"}",
      "identityValue must not be null;{\"identityType\": \"DL\", \"createdBy\": \"created\"}",
      "createdBy must not be null;{\"identityType\": \"DL\", \"identityValue\": \"0123456789\", \"createdBy\": null}",
      "createdBy must not be null;{\"identityType\": \"DL\", \"identityValue\": \"0123456789\"}",
    ],
    delimiter = ';',
  )
  fun `should return bad request if required fields are null`(expectedMessage: String, json: String) {
    val errors = webTestClient.post()
      .uri("/contact/$savedContactId/identity")
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
      event = OutboundEvent.CONTACT_IDENTITY_CREATED,
    )
  }

  @ParameterizedTest
  @MethodSource("allFieldConstraintViolations")
  fun `should enforce field constraints`(expectedMessage: String, request: CreateIdentityRequest) {
    val errors = webTestClient.post()
      .uri("/contact/$savedContactId/identity")
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
      event = OutboundEvent.CONTACT_IDENTITY_CREATED,
    )
  }

  @Test
  fun `should validate PNC number`() {
    val expectedMessage = "Identity value (1923/1Z34567A) is not a valid PNC Number"
    val request = aMinimalRequest().copy(identityType = "PNC", identityValue = "1923/1Z34567A")
    val errors = webTestClient.post()
      .uri("/contact/$savedContactId/identity")
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
  fun `should not create the identity if the type is unknown`() {
    val request = CreateIdentityRequest(
      identityType = "MACRO CARD",
      identityValue = "DL123456789",
      createdBy = "created",
    )

    val errors = webTestClient.post()
      .uri("/contact/$savedContactId/identity")
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

    assertThat(errors.userMessage).isEqualTo("Validation failure: Unsupported identity type (MACRO CARD)")
    stubEvents.assertHasNoEvents(
      event = OutboundEvent.CONTACT_IDENTITY_CREATED,
    )
  }

  @Test
  fun `should not create the identity if the type is no longer active`() {
    val request = CreateIdentityRequest(
      identityType = "NHS",
      identityValue = "Is active is false",
      createdBy = "created",
    )

    val errors = webTestClient.post()
      .uri("/contact/$savedContactId/identity")
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

    assertThat(errors.userMessage).isEqualTo("Validation failure: Unsupported identity type (NHS). This code is no longer active.")
    stubEvents.assertHasNoEvents(
      event = OutboundEvent.CONTACT_IDENTITY_CREATED,
    )
  }

  @Test
  fun `should not create the identity if the contact is not found`() {
    val request = aMinimalRequest()

    val errors = webTestClient.post()
      .uri("/contact/-321/identity")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CONTACTS_ADMIN")))
      .bodyValue(request)
      .exchange()
      .expectStatus()
      .isNotFound
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody!!

    assertThat(errors.userMessage).isEqualTo("Entity not found : Contact (-321) not found")
    stubEvents.assertHasNoEvents(
      event = OutboundEvent.CONTACT_IDENTITY_CREATED,
    )
  }

  @Test
  fun `should create the identity with minimal fields`() {
    val request = aMinimalRequest()

    val created = testAPIClient.createAContactIdentity(savedContactId, request)

    assertEqualsExcludingTimestamps(created, request)

    stubEvents.assertHasEvent(
      event = OutboundEvent.CONTACT_IDENTITY_CREATED,
      additionalInfo = ContactIdentityInfo(created.contactIdentityId, Source.DPS),
      personReference = PersonReference(dpsContactId = created.contactId),
    )
  }

  @ParameterizedTest
  @ValueSource(strings = ["ROLE_CONTACTS_ADMIN", "ROLE_CONTACTS__RW"])
  fun `should create the identity with all fields`(role: String) {
    val request = CreateIdentityRequest(
      identityType = "DL",
      identityValue = "DL123456789",
      issuingAuthority = "DVLA",
      createdBy = "created",
    )

    val created = testAPIClient.createAContactIdentity(savedContactId, request, role)

    assertEqualsExcludingTimestamps(created, request)

    stubEvents.assertHasEvent(
      event = OutboundEvent.CONTACT_IDENTITY_CREATED,
      additionalInfo = ContactIdentityInfo(created.contactIdentityId, Source.DPS),
      personReference = PersonReference(created.contactId),
    )
  }

  private fun assertEqualsExcludingTimestamps(identity: ContactIdentityDetails, request: CreateIdentityRequest) {
    with(identity) {
      assertThat(identityType).isEqualTo(request.identityType)
      assertThat(identityValue).isEqualTo(request.identityValue)
      assertThat(issuingAuthority).isEqualTo(request.issuingAuthority)
      assertThat(createdBy).isEqualTo(request.createdBy)
      assertThat(createdTime).isNotNull()
    }
  }

  companion object {
    @JvmStatic
    fun allFieldConstraintViolations(): List<Arguments> = listOf(
      Arguments.of("identityType must be <= 20 characters", aMinimalRequest().copy(identityType = "".padStart(21))),
      Arguments.of("identityValue must be <= 20 characters", aMinimalRequest().copy(identityValue = "".padStart(21))),
      Arguments.of(
        "issuingAuthority must be <= 40 characters",
        aMinimalRequest().copy(issuingAuthority = "".padStart(41)),
      ),
      Arguments.of(
        "createdBy must be <= 100 characters",
        aMinimalRequest().copy(createdBy = "".padStart(101)),
      ),
    )

    private fun aMinimalRequest() = CreateIdentityRequest(
      identityType = "DL",
      identityValue = "DL123456789",
      createdBy = "created",
    )
  }
}
