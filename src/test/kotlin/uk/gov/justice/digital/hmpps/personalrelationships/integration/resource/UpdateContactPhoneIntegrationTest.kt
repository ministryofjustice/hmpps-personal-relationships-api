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
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.phone.UpdatePhoneRequest
import uk.gov.justice.digital.hmpps.personalrelationships.service.events.ContactPhoneInfo
import uk.gov.justice.digital.hmpps.personalrelationships.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.personalrelationships.service.events.PersonReference
import uk.gov.justice.digital.hmpps.personalrelationships.service.events.Source
import uk.gov.justice.digital.hmpps.personalrelationships.util.StubUser
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

class UpdateContactPhoneIntegrationTest : SecureAPIIntegrationTestBase() {
  private var savedContactId = 0L
  private var savedContactPhoneId = 0L

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
    savedContactPhoneId = testAPIClient.createAContactPhone(
      savedContactId,
      CreatePhoneRequest(
        phoneType = "MOB",
        phoneNumber = "07777777777",
        extNumber = "123456",
      ),

    ).contactPhoneId
    setCurrentUser(StubUser.UPDATING_USER)
  }

  override fun baseRequestBuilder(): WebTestClient.RequestHeadersSpec<*> = webTestClient.put()
    .uri("/contact/$savedContactId/phone/$savedContactPhoneId")
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
    val errors = webTestClient.put()
      .uri("/contact/$savedContactId/phone/$savedContactPhoneId")
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
    stubEvents.assertHasNoEvents(OutboundEvent.CONTACT_PHONE_UPDATED)
  }

  @ParameterizedTest
  @MethodSource("allFieldConstraintViolations")
  fun `should enforce field constraints`(expectedMessage: String, request: UpdatePhoneRequest) {
    val errors = webTestClient.put()
      .uri("/contact/$savedContactId/phone/$savedContactPhoneId")
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
    stubEvents.assertHasNoEvents(OutboundEvent.CONTACT_PHONE_UPDATED)
  }

  @ParameterizedTest
  @CsvSource(
    "Plus only at start,123+456",
    "Hash not allowed,#",
  )
  fun `should not update the phone if the phone number contains unsupported chars`(case: String, phoneNumber: String) {
    val request = UpdatePhoneRequest(
      phoneType = "MOB",
      phoneNumber = phoneNumber,
    )

    val errors = webTestClient.put()
      .uri("/contact/$savedContactId/phone/$savedContactPhoneId")
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
    stubEvents.assertHasNoEvents(OutboundEvent.CONTACT_PHONE_UPDATED)
  }

  @Test
  fun `should not update the phone if the type is not supported`() {
    val request = UpdatePhoneRequest(
      phoneType = "SATELLITE",
      phoneNumber = "+44777777777 (0123)",
    )

    val errors = webTestClient.put()
      .uri("/contact/$savedContactId/phone/$savedContactPhoneId")
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
    stubEvents.assertHasNoEvents(OutboundEvent.CONTACT_PHONE_UPDATED)
  }

  @Test
  fun `should not update the phone if the contact is not found`() {
    val request = aMinimalRequest()

    val errors = webTestClient.put()
      .uri("/contact/-321/phone/$savedContactPhoneId")
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
    stubEvents.assertHasNoEvents(OutboundEvent.CONTACT_PHONE_UPDATED)
  }

  @Test
  fun `should not update the phone if the phone is not found`() {
    val request = aMinimalRequest()

    val errors = webTestClient.put()
      .uri("/contact/$savedContactId/phone/-99")
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

    assertThat(errors.userMessage).isEqualTo("Entity not found : Contact phone (-99) not found")
    stubEvents.assertHasNoEvents(OutboundEvent.CONTACT_PHONE_UPDATED)
  }

  @Test
  fun `should update the phone with minimal fields`() {
    val request = UpdatePhoneRequest(
      phoneType = "MOB",
      phoneNumber = "+44777777777 (0123)",
      extNumber = null,
    )

    val updated = testAPIClient.updateAContactPhone(savedContactId, savedContactPhoneId, request)

    with(updated) {
      assertThat(phoneType).isEqualTo(request.phoneType)
      assertThat(phoneNumber).isEqualTo(request.phoneNumber)
      assertThat(extNumber).isNull()
      assertThat(createdBy).isEqualTo("created")
      assertThat(createdTime).isNotNull()
      assertThat(updatedBy).isEqualTo("updated")
      assertThat(updatedTime).isNotNull()
    }

    stubEvents.assertHasEvent(
      event = OutboundEvent.CONTACT_PHONE_UPDATED,
      additionalInfo = ContactPhoneInfo(savedContactPhoneId, Source.DPS, "updated", "BXI"),
      personReference = PersonReference(savedContactId),
    )
  }

  @ParameterizedTest
  @ValueSource(strings = ["ROLE_CONTACTS_ADMIN", "ROLE_CONTACTS__RW"])
  fun `should update the phone with all fields`(role: String) {
    setCurrentUser(StubUser.UPDATING_USER.copy(roles = listOf(role)))
    val request = UpdatePhoneRequest(
      phoneType = "MOB",
      phoneNumber = "+44777777777 (0123)",
      extNumber = "9999",
    )

    val updated = testAPIClient.updateAContactPhone(savedContactId, savedContactPhoneId, request)

    with(updated) {
      assertThat(phoneType).isEqualTo(request.phoneType)
      assertThat(phoneNumber).isEqualTo(request.phoneNumber)
      assertThat(extNumber).isEqualTo(request.extNumber)
      assertThat(createdBy).isEqualTo("created")
      assertThat(createdTime).isNotNull()
      assertThat(updatedBy).isEqualTo("updated")
      assertThat(updatedTime).isNotNull()
    }

    stubEvents.assertHasEvent(
      event = OutboundEvent.CONTACT_PHONE_UPDATED,
      additionalInfo = ContactPhoneInfo(savedContactPhoneId, Source.DPS, "updated", "BXI"),
      personReference = PersonReference(dpsContactId = savedContactId),
    )
  }

  companion object {
    @JvmStatic
    fun allFieldConstraintViolations(): List<Arguments> = listOf(
      Arguments.of("phoneType must be <= 12 characters", aMinimalRequest().copy(phoneType = "".padStart(13, 'X'))),
      Arguments.of("phoneNumber must be <= 40 characters", aMinimalRequest().copy(phoneNumber = "".padStart(41, '9'))),
      Arguments.of(
        "extNumber must be <= 7 characters",
        aMinimalRequest().copy(extNumber = "".padStart(8, 'X')),
      ),
    )

    private fun aMinimalRequest() = UpdatePhoneRequest(
      phoneType = "MOB",
      phoneNumber = "+44777777777 (0123)",
    )
  }
}
