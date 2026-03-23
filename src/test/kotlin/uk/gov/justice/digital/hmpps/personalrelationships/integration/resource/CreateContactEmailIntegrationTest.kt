package uk.gov.justice.digital.hmpps.personalrelationships.integration.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.http.HttpStatus.CONFLICT
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.personalrelationships.config.User
import uk.gov.justice.digital.hmpps.personalrelationships.integration.SecureAPIIntegrationTestBase
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.CreateContactRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.email.CreateEmailRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.ContactEmailDetails
import uk.gov.justice.digital.hmpps.personalrelationships.service.events.ContactEmailInfo
import uk.gov.justice.digital.hmpps.personalrelationships.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.personalrelationships.service.events.PersonReference
import uk.gov.justice.digital.hmpps.personalrelationships.service.events.Source
import uk.gov.justice.digital.hmpps.personalrelationships.util.StubUser
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

class CreateContactEmailIntegrationTest : SecureAPIIntegrationTestBase() {
  private var savedContactId = 0L

  override val allowedRoles: Set<String> = setOf("ROLE_CONTACTS_ADMIN", "ROLE_CONTACTS__RW")

  @BeforeEach
  fun initialiseData() {
    setCurrentUser(StubUser.CREATING_USER)
    savedContactId = testAPIClient.createAContact(
      CreateContactRequest(
        lastName = "email",
        firstName = "has",
      ),
    ).id
  }

  override fun baseRequestBuilder(): WebTestClient.RequestHeadersSpec<*> = webTestClient.post()
    .uri("/contact/$savedContactId/email")
    .accept(MediaType.APPLICATION_JSON)
    .contentType(MediaType.APPLICATION_JSON)
    .bodyValue(aMinimalRequest())

  @ParameterizedTest
  @CsvSource(
    value = [
      "emailAddress must not be null;{\"emailAddress\": null}",
      "emailAddress must not be null;{}",
    ],
    delimiter = ';',
  )
  fun `should return bad request if required fields are null`(expectedMessage: String, json: String) {
    val errors = webTestClient.post()
      .uri("/contact/$savedContactId/email")
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
      event = OutboundEvent.CONTACT_EMAIL_CREATED,
    )
  }

  @ParameterizedTest
  @MethodSource("allFieldConstraintViolations")
  fun `should enforce field constraints`(expectedMessage: String, request: CreateEmailRequest) {
    val errors = webTestClient.post()
      .uri("/contact/$savedContactId/email")
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
      event = OutboundEvent.CONTACT_EMAIL_CREATED,
    )
  }

  @Test
  fun `should not create the email if the contact is not found`() {
    val request = aMinimalRequest()

    val errors = webTestClient.post()
      .uri("/contact/-321/email")
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
      event = OutboundEvent.CONTACT_EMAIL_CREATED,
    )
  }

  @Test
  fun `should not create the email if the contact already has the same email address`() {
    val request = aMinimalRequest()

    testAPIClient.createAContactEmail(savedContactId, request)

    val errors = webTestClient.post()
      .uri("/contact/$savedContactId/email")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .headers(setAuthorisationUsingCurrentUser())
      .bodyValue(request)
      .exchange()
      .expectStatus()
      .isEqualTo(CONFLICT)
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody!!

    assertThat(errors.userMessage).isEqualTo("Contact already has an email address matching \"test@example.com\"")
  }

  @Test
  fun `should not create the email if the email is not valid`() {
    val request = CreateEmailRequest(
      emailAddress = "@example.com",
    )

    val errors = webTestClient.post()
      .uri("/contact/$savedContactId/email")
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

    assertThat(errors.userMessage).isEqualTo("Validation failure: Email address is invalid")
  }

  @ParameterizedTest
  @ValueSource(strings = ["ROLE_CONTACTS_ADMIN", "ROLE_CONTACTS__RW"])
  fun `should create the email`(role: String) {
    setCurrentUser(StubUser.CREATING_USER.copy(roles = listOf(role)))
    val request = aMinimalRequest()

    val created = testAPIClient.createAContactEmail(savedContactId, request)

    assertEqualsExcludingTimestamps(created, request)

    stubEvents.assertHasEvent(
      event = OutboundEvent.CONTACT_EMAIL_CREATED,
      additionalInfo = ContactEmailInfo(created.contactEmailId, Source.DPS, "created", "BXI"),
      personReference = PersonReference(dpsContactId = created.contactId),
    )

    assertCustomEvent(created, Source.DPS, User("created", "BXI"))
  }

  private fun assertEqualsExcludingTimestamps(email: ContactEmailDetails, request: CreateEmailRequest) {
    with(email) {
      assertThat(emailAddress).isEqualTo(request.emailAddress)
      assertThat(createdBy).isEqualTo("created")
      assertThat(createdTime).isNotNull()
    }
  }

  private fun assertCustomEvent(contactEmailDetails: ContactEmailDetails, source: Source, user: User) {
    verify(telemetryContactCustomEventService, times(1)).trackCreateContactEmailEvent(contactEmailDetails, source, user)
    verify(telemetryClient, times(1)).trackEvent(
      "contact-email-created",
      mapOf(
        "description" to "A contact email has been created",
        "source" to source.name,
        "username" to user.username,
        "active_caseload_id" to user.activeCaseLoadId,
        "contactId" to contactEmailDetails.contactId.toString(),
        "contact_email_id" to contactEmailDetails.contactEmailId.toString(),
      ),
      null,
    )
  }

  companion object {
    @JvmStatic
    fun allFieldConstraintViolations(): List<Arguments> = listOf(
      Arguments.of("emailAddress must be <= 240 characters", aMinimalRequest().copy(emailAddress = "".padStart(241, 'X'))),
    )

    private fun aMinimalRequest() = CreateEmailRequest(
      emailAddress = "test@example.com",
    )
  }
}
