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
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.CreateContactAddressPhoneRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.CreateContactAddressRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.CreateContactRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.ContactAddressPhoneDetails
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.ContactAddressPhoneInfo
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.PersonReference
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.Source
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

class CreateContactAddressPhoneIntegrationTest : SecureAPIIntegrationTestBase() {
  private var savedContactId = 0L
  private var savedAddressId = 0L

  override val allowedRoles: Set<String> = setOf("ROLE_CONTACTS_ADMIN", "ROLE_CONTACTS__RW")

  @BeforeEach
  fun initialiseData() {
    savedContactId = testAPIClient.createAContact(
      CreateContactRequest(
        lastName = "address-phone",
        firstName = "has",
        createdBy = "created",
      ),
    ).id

    savedAddressId = testAPIClient.createAContactAddress(
      savedContactId,
      CreateContactAddressRequest(
        addressType = "HOME",
        primaryAddress = true,
        property = "27",
        street = "Hello Road",
        createdBy = "created",
      ),
    ).contactAddressId
  }

  override fun baseRequestBuilder(): WebTestClient.RequestHeadersSpec<*> = webTestClient.post()
    .uri("/contact/$savedContactId/address/$savedAddressId/phone")
    .accept(MediaType.APPLICATION_JSON)
    .contentType(MediaType.APPLICATION_JSON)
    .bodyValue(aMinimalRequest().copy(contactAddressId = savedAddressId))

  @ParameterizedTest
  @MethodSource("allFieldConstraintViolations")
  fun `should enforce field constraints`(expectedMessage: String, request: CreateContactAddressPhoneRequest) {
    val errors = webTestClient.post()
      .uri("/contact/$savedContactId/address/$savedAddressId/phone")
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
      event = OutboundEvent.CONTACT_ADDRESS_PHONE_CREATED,
    )
  }

  @ParameterizedTest
  @CsvSource(
    "Plus only at start,123+456",
    "Hash not allowed,#",
  )
  fun `should not create the phone if the phone number contains unsupported chars`(case: String, phoneNumber: String) {
    val request = aMinimalRequest().copy(
      contactAddressId = savedAddressId,
      phoneNumber = phoneNumber,
    )

    val errors = webTestClient.post()
      .uri("/contact/$savedContactId/address/$savedAddressId/phone")
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

    assertThat(errors.userMessage).isEqualTo("Validation failure: Phone number invalid, it can only contain numbers, () and whitespace with an optional + at the start")
    stubEvents.assertHasNoEvents(
      event = OutboundEvent.CONTACT_ADDRESS_PHONE_CREATED,
    )
  }

  @Test
  fun `should not create the phone if the contact is not found`() {
    val request = aMinimalRequest().copy(contactAddressId = savedAddressId)

    val errors = webTestClient.post()
      .uri("/contact/-321/address/$savedAddressId/phone")
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
      event = OutboundEvent.CONTACT_ADDRESS_PHONE_CREATED,
    )
  }

  @Test
  fun `should not create the phone if the address is not found`() {
    val request = aMinimalRequest().copy(contactAddressId = -400)

    val errors = webTestClient.post()
      .uri("/contact/$savedContactId/address/-400/phone")
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

    assertThat(errors.userMessage).isEqualTo("Entity not found : Contact address (-400) not found")
    stubEvents.assertHasNoEvents(
      event = OutboundEvent.CONTACT_ADDRESS_PHONE_CREATED,
    )
  }

  @ParameterizedTest
  @ValueSource(strings = ["ROLE_CONTACTS_ADMIN", "ROLE_CONTACTS__RW"])
  fun `should create the address-specific phone number`(role: String) {
    val request = aMinimalRequest().copy(contactAddressId = savedAddressId)

    val created = testAPIClient.createAContactAddressPhone(
      savedContactId,
      savedAddressId,
      request,
      role,
    )

    assertEqualsExcludingTimestamps(created, request)

    stubEvents.assertHasEvent(
      event = OutboundEvent.CONTACT_ADDRESS_PHONE_CREATED,
      additionalInfo = ContactAddressPhoneInfo(created.contactAddressPhoneId, created.contactAddressId, Source.DPS),
      personReference = PersonReference(dpsContactId = created.contactId),
    )
  }

  private fun assertEqualsExcludingTimestamps(
    response: ContactAddressPhoneDetails,
    request: CreateContactAddressPhoneRequest,
  ) {
    with(response) {
      assertThat(contactId).isEqualTo(savedContactId)
      assertThat(contactAddressId).isEqualTo(savedAddressId)
      assertThat(contactAddressPhoneId).isGreaterThan(0L)
      assertThat(contactPhoneId).isGreaterThan(0L)
      assertThat(phoneType).isEqualTo(request.phoneType)
      assertThat(phoneNumber).isEqualTo(request.phoneNumber)
      assertThat(extNumber).isEqualTo(request.extNumber)
      assertThat(createdBy).isEqualTo(request.createdBy)
      assertThat(createdTime).isNotNull()
    }
  }

  companion object {
    @JvmStatic
    fun allFieldConstraintViolations(): List<Arguments> = listOf(
      Arguments.of("phoneType must be <= 12 characters", aMinimalRequest().copy(phoneType = "".padStart(13))),
      Arguments.of("phoneNumber must be <= 240 characters", aMinimalRequest().copy(phoneNumber = "".padStart(241))),
      Arguments.of(
        "extNumber must be <= 7 characters",
        aMinimalRequest().copy(extNumber = "".padStart(8)),
      ),
      Arguments.of(
        "createdBy must be <= 100 characters",
        aMinimalRequest().copy(createdBy = "".padStart(101)),
      ),
    )

    private fun aMinimalRequest() = CreateContactAddressPhoneRequest(
      contactAddressId = 1L,
      phoneType = "HOME",
      phoneNumber = "123456789",
      extNumber = "111",
      createdBy = "CREATED",
    )
  }
}
