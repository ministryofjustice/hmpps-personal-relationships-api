package uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.SecureAPIIntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.CreateContactRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.identity.CreateIdentityRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.ContactIdentityDetails
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.ContactIdentityInfo
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.PersonReference
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.Source
import uk.gov.justice.digital.hmpps.hmppscontactsapi.util.StubUser
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

class CreateContactIdentityIntegrationTest : SecureAPIIntegrationTestBase() {
  private var savedContactId = 0L

  override val allowedRoles: Set<String> = setOf("ROLE_CONTACTS_ADMIN", "ROLE_CONTACTS__RW")

  @BeforeEach
  fun initialiseData() {
    setCurrentUser(StubUser.CREATING_USER)
    savedContactId = testAPIClient.createAContact(
      CreateContactRequest(
        lastName = "identity",
        firstName = "has",
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
      "identityType must not be null;{\"identityType\": null, \"identityValue\": \"0123456789\"}",
      "identityType must not be null;{\"identityValue\": \"0123456789\"}",
      "identityValue must not be null;{\"identityType\": \"DL\", \"identityValue\": null}",
      "identityValue must not be null;{\"identityType\": \"DL\"}",
    ],
    delimiter = ';',
  )
  fun `should return bad request if required fields are null`(expectedMessage: String, json: String) {
    val errors = webTestClient.post()
      .uri("/contact/$savedContactId/identity")
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
  fun `should not create the identity if the type is unknown`() {
    val request = CreateIdentityRequest(
      identityType = "MACRO CARD",
      identityValue = "DL123456789",
    )

    val errors = webTestClient.post()
      .uri("/contact/$savedContactId/identity")
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
    )

    val errors = webTestClient.post()
      .uri("/contact/$savedContactId/identity")
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
      .headers(setAuthorisationUsingCurrentUser())
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
      additionalInfo = ContactIdentityInfo(created.contactIdentityId, Source.DPS, "created", "BXI"),
      personReference = PersonReference(dpsContactId = created.contactId),
    )
  }

  @Test
  fun `should create the identity with all fields`() {
    val request = CreateIdentityRequest(
      identityType = "DL",
      identityValue = "DL123456789",
      issuingAuthority = "DVLA",
    )

    val created = testAPIClient.createAContactIdentity(savedContactId, request)

    assertEqualsExcludingTimestamps(created, request)

    stubEvents.assertHasEvent(
      event = OutboundEvent.CONTACT_IDENTITY_CREATED,
      additionalInfo = ContactIdentityInfo(created.contactIdentityId, Source.DPS, "created", "BXI"),
      personReference = PersonReference(created.contactId),
    )
  }

  private fun assertEqualsExcludingTimestamps(identity: ContactIdentityDetails, request: CreateIdentityRequest) {
    with(identity) {
      assertThat(identityType).isEqualTo(request.identityType)
      assertThat(identityValue).isEqualTo(request.identityValue)
      assertThat(issuingAuthority).isEqualTo(request.issuingAuthority)
      assertThat(createdBy).isEqualTo("created")
      assertThat(createdTime).isNotNull()
    }
  }

  companion object {
    @JvmStatic
    fun allFieldConstraintViolations(): List<Arguments> = listOf(
      Arguments.of("identityType must be <= 12 characters", aMinimalRequest().copy(identityType = "".padStart(13, 'X'))),
      Arguments.of("identityValue must be <= 20 characters", aMinimalRequest().copy(identityValue = "".padStart(21, 'X'))),
      Arguments.of(
        "issuingAuthority must be <= 40 characters",
        aMinimalRequest().copy(issuingAuthority = "".padStart(41, 'X')),
      ),
    )

    private fun aMinimalRequest() = CreateIdentityRequest(
      identityType = "DL",
      identityValue = "DL123456789",
    )
  }
}
