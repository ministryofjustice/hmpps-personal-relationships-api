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
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.address.CreateContactAddressRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.phone.CreateMultiplePhoneNumbersRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.phone.PhoneNumber
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.ContactAddressPhoneInfo
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.PersonReference
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.Source
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

class CreateMultipleAddressPhoneIntegrationTest : SecureAPIIntegrationTestBase() {
  private var savedContactId = 0L
  private var savedAddressId = 0L

  override val allowedRoles: Set<String> = setOf("ROLE_CONTACTS_ADMIN", "ROLE_CONTACTS__RW")

  @BeforeEach
  fun initialiseData() {
    savedContactId = testAPIClient.createAContact(
      CreateContactRequest(
        lastName = "phone",
        firstName = "has",
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
    stubEvents.reset()
  }

  override fun baseRequestBuilder(): WebTestClient.RequestHeadersSpec<*> = webTestClient.post()
    .uri("/contact/$savedContactId/address/$savedAddressId/phones")
    .accept(MediaType.APPLICATION_JSON)
    .contentType(MediaType.APPLICATION_JSON)
    .bodyValue(aMinimalRequest())

  @ParameterizedTest
  @CsvSource(
    value = [
      "phoneNumbers[0].phoneType must not be null;{\"phoneNumbers\": [{ \"phoneType\": null, \"phoneNumber\": \"0123456789\"}], \"createdBy\": \"created\"}",
      "phoneNumbers[0].phoneType must not be null;{\"phoneNumbers\": [{\"phoneNumber\": \"0123456789\"}], \"createdBy\": \"created\"}",
      "phoneNumbers[0].phoneNumber must not be null;{\"phoneNumbers\": [{\"phoneType\": \"MOB\", \"phoneNumber\": null}], \"createdBy\": \"created\"}",
      "phoneNumbers[0].phoneNumber must not be null;{\"phoneNumbers\": [{\"phoneType\": \"MOB\"}], \"createdBy\": \"created\"}",
      "createdBy must not be null;{\"phoneNumbers\": [{\"phoneType\": \"MOB\", \"phoneNumber\": \"0123456789\"}], \"createdBy\": null}",
      "createdBy must not be null;{\"phoneNumbers\": [{\"phoneType\": \"MOB\", \"phoneNumber\": \"0123456789\"}]}",
    ],
    delimiter = ';',
  )
  fun `should return bad request if required fields are null`(expectedMessage: String, json: String) {
    val errors = webTestClient.post()
      .uri("/contact/$savedContactId/address/$savedAddressId/phones")
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
      event = OutboundEvent.CONTACT_ADDRESS_PHONE_CREATED,
    )
  }

  @ParameterizedTest
  @MethodSource("allFieldConstraintViolations")
  fun `should enforce field constraints`(expectedMessage: String, request: CreateMultiplePhoneNumbersRequest) {
    val errors = webTestClient.post()
      .uri("/contact/$savedContactId/address/$savedAddressId/phones")
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
    val request = CreateMultiplePhoneNumbersRequest(
      phoneNumbers = listOf(
        PhoneNumber(
          phoneType = "MOB",
          phoneNumber = phoneNumber,
        ),
      ),
      createdBy = "created",
    )

    val errors = webTestClient.post()
      .uri("/contact/$savedContactId/address/$savedAddressId/phones")
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
  fun `should not create the phone if the type is not supported`() {
    val request = CreateMultiplePhoneNumbersRequest(
      listOf(
        PhoneNumber(
          phoneType = "SATELLITE",
          phoneNumber = "+44777777777 (0123)",
        ),
      ),
      createdBy = "created",
    )

    val errors = webTestClient.post()
      .uri("/contact/$savedContactId/address/$savedAddressId/phones")
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

    assertThat(errors.userMessage).isEqualTo("Validation failure: Unsupported phone type (SATELLITE)")
    stubEvents.assertHasNoEvents(
      event = OutboundEvent.CONTACT_ADDRESS_PHONE_CREATED,
    )
  }

  @Test
  fun `should not create the phone if the contact is not found`() {
    val request = aMinimalRequest()

    val errors = webTestClient.post()
      .uri("/contact/-321/address/$savedAddressId/phones")
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
    val request = aMinimalRequest()

    val errors = webTestClient.post()
      .uri("/contact/$savedContactId/address/-321/phones")
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

    assertThat(errors.userMessage).isEqualTo("Entity not found : Contact address (-321) not found")
    stubEvents.assertHasNoEvents(
      event = OutboundEvent.CONTACT_ADDRESS_PHONE_CREATED,
    )
  }

  @ParameterizedTest
  @ValueSource(strings = ["ROLE_CONTACTS_ADMIN", "ROLE_CONTACTS__RW"])
  fun `should create multiple phones`(role: String) {
    val request = CreateMultiplePhoneNumbersRequest(
      listOf(
        PhoneNumber(
          phoneType = "MOB",
          phoneNumber = "+44777777777 (0123)",
        ),
        PhoneNumber(
          phoneType = "HOME",
          phoneNumber = "01234 567890",
          extNumber = null,
        ),
      ),
      createdBy = "created",
    )

    val created = testAPIClient.createMultipleContactAddressPhones(savedContactId, savedAddressId, request, role)

    val mobile = created.find { it.phoneType == "MOB" }
    assertThat(mobile).isNotNull()
    stubEvents.assertHasEvent(
      event = OutboundEvent.CONTACT_ADDRESS_PHONE_CREATED,
      additionalInfo = ContactAddressPhoneInfo(mobile!!.contactAddressPhoneId, savedAddressId, Source.DPS),
      personReference = PersonReference(dpsContactId = savedContactId),
    )

    val home = created.find { it.phoneType == "HOME" }
    assertThat(home).isNotNull()
    stubEvents.assertHasEvent(
      event = OutboundEvent.CONTACT_ADDRESS_PHONE_CREATED,
      additionalInfo = ContactAddressPhoneInfo(home!!.contactAddressPhoneId, savedAddressId, Source.DPS),
      personReference = PersonReference(dpsContactId = savedContactId),
    )
  }

  companion object {
    @JvmStatic
    fun allFieldConstraintViolations(): List<Arguments> = listOf(
      Arguments.of(
        "phoneNumbers[0].phoneType must be <= 12 characters",
        aMinimalRequest().copy(
          phoneNumbers = listOf(
            PhoneNumber(
              phoneType = "".padStart(13, 'X'),
              phoneNumber = "123",
            ),
          ),
        ),
      ),
      Arguments.of(
        "phoneNumbers[0].phoneNumber must be <= 40 characters",
        aMinimalRequest().copy(
          phoneNumbers = listOf(
            PhoneNumber(
              phoneType = "MOB",
              phoneNumber = "".padStart(41, 'X'),
            ),
          ),
        ),
      ),
      Arguments.of(
        "phoneNumbers[0].extNumber must be <= 7 characters",
        aMinimalRequest().copy(
          phoneNumbers = listOf(
            PhoneNumber(
              phoneType = "MOB",
              phoneNumber = "132",
              extNumber = "".padStart(8, 'X'),
            ),
          ),
        ),
      ),
      Arguments.of("phoneNumbers must have at least 1 item", aMinimalRequest().copy(phoneNumbers = emptyList())),
      Arguments.of("createdBy must be <= 100 characters", aMinimalRequest().copy(createdBy = "".padStart(101, 'X'))),
    )

    private fun aMinimalRequest() = CreateMultiplePhoneNumbersRequest(
      listOf(
        PhoneNumber(
          phoneType = "MOB",
          phoneNumber = "+44777777777 (0123)",
        ),
      ),
      createdBy = "created",
    )
  }
}
