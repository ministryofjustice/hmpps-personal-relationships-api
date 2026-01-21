package uk.gov.justice.digital.hmpps.personalrelationships.integration.resource

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
import uk.gov.justice.digital.hmpps.personalrelationships.integration.SecureAPIIntegrationTestBase
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.CreateContactRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.phone.CreatePhoneRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.ContactPhoneDetails
import uk.gov.justice.digital.hmpps.personalrelationships.service.events.ContactPhoneInfo
import uk.gov.justice.digital.hmpps.personalrelationships.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.personalrelationships.service.events.PersonReference
import uk.gov.justice.digital.hmpps.personalrelationships.service.events.Source
import uk.gov.justice.digital.hmpps.personalrelationships.util.StubUser
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

class CreateContactPhoneIntegrationTest : SecureAPIIntegrationTestBase() {
  private var savedContactId = 0L

  override val allowedRoles: Set<String> = setOf("ROLE_CONTACTS_ADMIN", "ROLE_CONTACTS__RW")

  @BeforeEach
  fun initialiseData() {
    setCurrentUser(StubUser.CREATING_USER)
    savedContactId = testAPIClient.createAContact(
      CreateContactRequest(
        lastName = "phone",
        firstName = "has",
      ),
    ).id
  }

  override fun baseRequestBuilder(): WebTestClient.RequestHeadersSpec<*> = webTestClient.post()
    .uri("/contact/$savedContactId/phone")
    .accept(MediaType.APPLICATION_JSON)
    .contentType(MediaType.APPLICATION_JSON)
    .bodyValue(aMinimalRequest())

  @ParameterizedTest
  @CsvSource(
    value = [
      "phoneType must not be null;{\"phoneType\": null, \"phoneNumber\": \"0123456789\"}",
      "phoneType must not be null;{\"phoneNumber\": \"0123456789\"}",
      "phoneNumber must not be null;{\"phoneType\": \"MOB\", \"phoneNumber\": null}",
      "phoneNumber must not be null;{\"phoneType\": \"MOB\"}",
    ],
    delimiter = ';',
  )
  fun `should return bad request if required fields are null`(expectedMessage: String, json: String) {
    val errors = webTestClient.post()
      .uri("/contact/$savedContactId/phone")
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
      event = OutboundEvent.CONTACT_PHONE_CREATED,
    )
  }

  @ParameterizedTest
  @MethodSource("allFieldConstraintViolations")
  fun `should enforce field constraints`(expectedMessage: String, request: CreatePhoneRequest) {
    val errors = webTestClient.post()
      .uri("/contact/$savedContactId/phone")
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
      event = OutboundEvent.CONTACT_PHONE_CREATED,
    )
  }

  @ParameterizedTest
  @CsvSource(
    "Plus only at start,123+456",
    "Hash not allowed,#",
  )
  fun `should not create the phone if the phone number contains unsupported chars`(case: String, phoneNumber: String) {
    val request = CreatePhoneRequest(
      phoneType = "MOB",
      phoneNumber = phoneNumber,
    )

    val errors = webTestClient.post()
      .uri("/contact/$savedContactId/phone")
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

    assertThat(errors.userMessage).isEqualTo("Validation failure: Phone number invalid, it can only contain numbers, () and whitespace with an optional + at the start")
    stubEvents.assertHasNoEvents(
      event = OutboundEvent.CONTACT_PHONE_CREATED,
    )
  }

  @Test
  fun `should not create the phone if the type is not supported`() {
    val request = CreatePhoneRequest(
      phoneType = "SATELLITE",
      phoneNumber = "+44777777777 (0123)",
    )

    val errors = webTestClient.post()
      .uri("/contact/$savedContactId/phone")
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

    assertThat(errors.userMessage).isEqualTo("Validation failure: Unsupported phone type (SATELLITE)")
    stubEvents.assertHasNoEvents(
      event = OutboundEvent.CONTACT_PHONE_CREATED,
    )
  }

  @Test
  fun `should not create the phone if the contact is not found`() {
    val request = aMinimalRequest()

    val errors = webTestClient.post()
      .uri("/contact/-321/phone")
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
      event = OutboundEvent.CONTACT_PHONE_CREATED,
    )
  }

  @Test
  fun `should create the phone with minimal fields`() {
    val request = aMinimalRequest()

    val created = testAPIClient.createAContactPhone(savedContactId, request)

    assertEqualsExcludingTimestamps(created, request)

    stubEvents.assertHasEvent(
      event = OutboundEvent.CONTACT_PHONE_CREATED,
      additionalInfo = ContactPhoneInfo(created.contactPhoneId, Source.DPS, "created", "BXI"),
      personReference = PersonReference(dpsContactId = created.contactId),
    )
  }

  @ParameterizedTest
  @ValueSource(strings = ["ROLE_CONTACTS_ADMIN", "ROLE_CONTACTS__RW"])
  fun `should create the phone with all fields`(role: String) {
    setCurrentUser(StubUser.CREATING_USER.copy(roles = listOf(role)))
    val request = CreatePhoneRequest(
      phoneType = "MOB",
      phoneNumber = "+44777777777 (0123)",
      extNumber = "9999",
    )

    val created = testAPIClient.createAContactPhone(savedContactId, request)

    assertEqualsExcludingTimestamps(created, request)

    stubEvents.assertHasEvent(
      event = OutboundEvent.CONTACT_PHONE_CREATED,
      additionalInfo = ContactPhoneInfo(created.contactPhoneId, Source.DPS, "created", "BXI"),
      personReference = PersonReference(dpsContactId = created.contactId),
    )
  }

  private fun assertEqualsExcludingTimestamps(phone: ContactPhoneDetails, request: CreatePhoneRequest) {
    with(phone) {
      assertThat(phoneType).isEqualTo(request.phoneType)
      assertThat(phoneNumber).isEqualTo(request.phoneNumber)
      assertThat(extNumber).isEqualTo(request.extNumber)
      assertThat(createdBy).isEqualTo("created")
      assertThat(createdTime).isNotNull()
    }
  }

  companion object {
    @JvmStatic
    fun allFieldConstraintViolations(): List<Arguments> = listOf(
      Arguments.of("phoneType must be <= 12 characters", aMinimalRequest().copy(phoneType = "".padStart(13, 'X'))),
      Arguments.of("phoneNumber must be <= 40 characters", aMinimalRequest().copy(phoneNumber = "".padStart(241, 'X'))),
      Arguments.of(
        "extNumber must be <= 7 characters",
        aMinimalRequest().copy(extNumber = "".padStart(8, 'X')),
      ),
    )

    private fun aMinimalRequest() = CreatePhoneRequest(
      phoneType = "MOB",
      phoneNumber = "+44777777777 (0123)",
    )
  }
}
