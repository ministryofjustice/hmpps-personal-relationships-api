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
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.email.CreateEmailRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.email.UpdateEmailRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.ContactEmailInfo
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.PersonReference
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.Source
import uk.gov.justice.digital.hmpps.hmppscontactsapi.util.StubUser
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

class UpdateContactEmailIntegrationTest : SecureAPIIntegrationTestBase() {
  private var savedContactId = 0L
  private var savedContactEmailId = 0L
  private val initialEmailAddress = "test@example.com"

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
    savedContactEmailId = testAPIClient.createAContactEmail(
      savedContactId,
      CreateEmailRequest(
        emailAddress = initialEmailAddress,
      ),

    ).contactEmailId
    setCurrentUser(StubUser.UPDATING_USER)
  }

  override fun baseRequestBuilder(): WebTestClient.RequestHeadersSpec<*> = webTestClient.put()
    .uri("/contact/$savedContactId/email/$savedContactEmailId")
    .accept(MediaType.APPLICATION_JSON)
    .contentType(MediaType.APPLICATION_JSON)
    .bodyValue(aMinimalRequest())

  @ParameterizedTest
  @CsvSource(
    value = [
      "emailAddress must not be null;{\"emailAddress\": null, \"emailValue\": \"0123456789\"}",
      "emailAddress must not be null;{\"emailValue\": \"test@example.com\"}",
    ],
    delimiter = ';',
  )
  fun `should return bad request if required fields are null`(expectedMessage: String, json: String) {
    val errors = webTestClient.put()
      .uri("/contact/$savedContactId/email/$savedContactEmailId")
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
      event = OutboundEvent.CONTACT_EMAIL_UPDATED,
      additionalInfo = ContactEmailInfo(savedContactEmailId, Source.DPS, "updated", "BXI"),
    )
  }

  @ParameterizedTest
  @MethodSource("allFieldConstraintViolations")
  fun `should enforce field constraints`(expectedMessage: String, request: UpdateEmailRequest) {
    val errors = webTestClient.put()
      .uri("/contact/$savedContactId/email/$savedContactEmailId")
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
      event = OutboundEvent.CONTACT_EMAIL_UPDATED,
      additionalInfo = ContactEmailInfo(savedContactEmailId, Source.DPS, "updated", "BXI"),
    )
  }

  @Test
  fun `should not update the email if the contact is not found`() {
    val request = aMinimalRequest()

    val errors = webTestClient.put()
      .uri("/contact/-321/email/$savedContactEmailId")
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
      event = OutboundEvent.CONTACT_EMAIL_UPDATED,
      additionalInfo = ContactEmailInfo(savedContactEmailId, Source.DPS, "updated", "BXI"),
    )
  }

  @Test
  fun `should not update the email if the email is not found`() {
    val request = aMinimalRequest()

    val errors = webTestClient.put()
      .uri("/contact/$savedContactId/email/-99")
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

    assertThat(errors.userMessage).isEqualTo("Entity not found : Contact email (-99) not found")

    stubEvents.assertHasNoEvents(
      event = OutboundEvent.CONTACT_EMAIL_UPDATED,
      additionalInfo = ContactEmailInfo(-99, Source.DPS, "updated", "BXI"),
    )
  }

  @Test
  fun `should not update the email if the email address is invalid`() {
    val request = UpdateEmailRequest(
      emailAddress = "@example.com",
    )

    val errors = webTestClient.put()
      .uri("/contact/$savedContactId/email/$savedContactEmailId")
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

    stubEvents.assertHasNoEvents(
      event = OutboundEvent.CONTACT_EMAIL_UPDATED,
      additionalInfo = ContactEmailInfo(-99, Source.DPS, "updated", "BXI"),
    )
  }

  @ParameterizedTest
  @ValueSource(strings = ["ROLE_CONTACTS_ADMIN", "ROLE_CONTACTS__RW"])
  fun `should update the email`(role: String) {
    setCurrentUser(StubUser.UPDATING_USER.copy(roles = listOf(role)))
    val request = UpdateEmailRequest(
      emailAddress = "updated@example.com",
    )
    val updated = testAPIClient.updateAContactEmail(savedContactId, savedContactEmailId, request)

    with(updated) {
      assertThat(emailAddress).isEqualTo("updated@example.com")
      assertThat(createdBy).isEqualTo("created")
      assertThat(createdTime).isNotNull()
      assertThat(updatedBy).isEqualTo("updated")
      assertThat(updatedTime).isNotNull()
    }

    stubEvents.assertHasEvent(
      event = OutboundEvent.CONTACT_EMAIL_UPDATED,
      additionalInfo = ContactEmailInfo(savedContactEmailId, Source.DPS, "updated", "BXI"),
      personReference = PersonReference(dpsContactId = savedContactId),
    )
  }

  @Test
  fun `should not be able to update the email to an existing email address leading to a duplicate`() {
    doWithTemporaryWritePermission { testAPIClient.createAContactEmail(savedContactId, CreateEmailRequest("foo@example.com")) }

    val request = UpdateEmailRequest(
      emailAddress = "FOO@EXAMPLE.COM",
    )

    val errors = webTestClient.put()
      .uri("/contact/$savedContactId/email/$savedContactEmailId")
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

    assertThat(errors.userMessage).isEqualTo("Contact already has an email address matching \"FOO@EXAMPLE.COM\"")

    stubEvents.assertHasNoEvents(
      event = OutboundEvent.CONTACT_EMAIL_UPDATED,
      additionalInfo = ContactEmailInfo(-99, Source.DPS, "updated", "BXI"),
    )
  }

  @Test
  fun `should be able to update the email to the same email address`() {
    val request = UpdateEmailRequest(
      emailAddress = initialEmailAddress,
    )
    val updated = testAPIClient.updateAContactEmail(savedContactId, savedContactEmailId, request)

    with(updated) {
      assertThat(emailAddress).isEqualTo(initialEmailAddress)
      assertThat(createdBy).isEqualTo("created")
      assertThat(createdTime).isNotNull()
      assertThat(updatedBy).isEqualTo("updated")
      assertThat(updatedTime).isNotNull()
    }

    stubEvents.assertHasEvent(
      event = OutboundEvent.CONTACT_EMAIL_UPDATED,
      additionalInfo = ContactEmailInfo(savedContactEmailId, Source.DPS, "updated", "BXI"),
      personReference = PersonReference(dpsContactId = savedContactId),
    )
  }

  companion object {
    @JvmStatic
    fun allFieldConstraintViolations(): List<Arguments> = listOf(
      Arguments.of("emailAddress must be <= 240 characters", aMinimalRequest().copy(emailAddress = "".padStart(241, 'X'))),
    )

    private fun aMinimalRequest() = UpdateEmailRequest(
      emailAddress = "updated@example.com",
    )
  }
}
