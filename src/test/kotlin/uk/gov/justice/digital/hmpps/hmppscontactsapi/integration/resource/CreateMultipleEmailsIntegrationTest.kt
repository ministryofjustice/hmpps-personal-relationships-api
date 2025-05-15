package uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.http.HttpStatus.CONFLICT
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.SecureAPIIntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.CreateContactRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.email.CreateMultipleEmailsRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.email.EmailAddress
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.ContactEmailInfo
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.PersonReference
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.Source
import uk.gov.justice.digital.hmpps.hmppscontactsapi.util.StubUser
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

class CreateMultipleEmailsIntegrationTest : SecureAPIIntegrationTestBase() {
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
    .uri("/contact/$savedContactId/emails")
    .accept(MediaType.APPLICATION_JSON)
    .contentType(MediaType.APPLICATION_JSON)
    .bodyValue(aMinimalRequest())

  @ParameterizedTest
  @CsvSource(
    value = [
      "emailAddresses must not be null;{\"emailAddresses\": null}",
      "emailAddresses must not be null;{}",
    ],
    delimiter = ';',
  )
  fun `should return bad request if required fields are null`(expectedMessage: String, json: String) {
    val errors = webTestClient.post()
      .uri("/contact/$savedContactId/emails")
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
  fun `should enforce field constraints`(expectedMessage: String, request: CreateMultipleEmailsRequest) {
    val errors = webTestClient.post()
      .uri("/contact/$savedContactId/emails")
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
  fun `should not create the emails if the contact is not found`() {
    val request = aMinimalRequest()

    val errors = webTestClient.post()
      .uri("/contact/-321/emails")
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
    testAPIClient.createContactEmails(
      savedContactId,
      CreateMultipleEmailsRequest(
        emailAddresses = listOf(EmailAddress("test@example.com")),
      ),
    )

    val request = CreateMultipleEmailsRequest(
      emailAddresses = listOf(EmailAddress("other@example.com"), EmailAddress("test@example.com")),
    )

    val errors = webTestClient.post()
      .uri("/contact/$savedContactId/emails")
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
  fun `should not allow duplicate email addresses in the same request`() {
    val request = CreateMultipleEmailsRequest(
      emailAddresses = listOf(EmailAddress("foo@example.com"), EmailAddress("FOO@EXAMPLE.COM")),
    )

    val errors = webTestClient.post()
      .uri("/contact/$savedContactId/emails")
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

    assertThat(errors.userMessage).isEqualTo("Validation failure: Request contains duplicate email addresses")
  }

  @Test
  fun `should not create the emails if any email is not valid`() {
    val request = CreateMultipleEmailsRequest(
      emailAddresses = listOf(EmailAddress("@example.com"), EmailAddress("good@example.com")),
    )

    val errors = webTestClient.post()
      .uri("/contact/$savedContactId/emails")
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

    assertThat(testAPIClient.getContact(savedContactId).emailAddresses).isEmpty()
  }

  @ParameterizedTest
  @ValueSource(strings = ["ROLE_CONTACTS_ADMIN", "ROLE_CONTACTS__RW"])
  fun `should create the emails`(role: String) {
    setCurrentUser(StubUser.CREATING_USER.copy(roles = listOf(role)))
    val request = CreateMultipleEmailsRequest(
      emailAddresses = listOf(EmailAddress("test@example.com"), EmailAddress("another@example.com")),
    )

    val created = testAPIClient.createContactEmails(savedContactId, request)

    request.emailAddresses.forEach { requestedEmailAddress ->
      val createdEmailAddress = created.find { it.emailAddress == requestedEmailAddress.emailAddress }
      assertThat(createdEmailAddress).isNotNull

      stubEvents.assertHasEvent(
        event = OutboundEvent.CONTACT_EMAIL_CREATED,
        additionalInfo = ContactEmailInfo(createdEmailAddress!!.contactEmailId, Source.DPS, "created", "BXI"),
        personReference = PersonReference(dpsContactId = savedContactId),
      )
    }
  }

  companion object {
    @JvmStatic
    fun allFieldConstraintViolations(): List<Arguments> = listOf(
      Arguments.of(
        "emailAddresses[0].emailAddress must be <= 240 characters",
        aMinimalRequest().copy(emailAddresses = listOf(EmailAddress("".padStart(241, 'X')))),
      ),
      Arguments.of("emailAddresses must have at least 1 item", aMinimalRequest().copy(emailAddresses = emptyList())),
    )

    private fun aMinimalRequest() = CreateMultipleEmailsRequest(
      emailAddresses = listOf(EmailAddress("test@example.com")),
    )
  }
}
