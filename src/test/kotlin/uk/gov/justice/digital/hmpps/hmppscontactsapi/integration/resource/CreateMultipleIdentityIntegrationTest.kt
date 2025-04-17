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
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.identity.CreateMultipleIdentitiesRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.identity.IdentityDocument
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.ContactIdentityInfo
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.PersonReference
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.Source
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

class CreateMultipleIdentityIntegrationTest : SecureAPIIntegrationTestBase() {
  private var savedContactId = 0L

  override val allowedRoles: Set<String> = setOf("ROLE_CONTACTS_ADMIN", "ROLE_CONTACTS__RW")

  @BeforeEach
  fun initialiseData() {
    savedContactId = testAPIClient.createAContact(
      CreateContactRequest(
        lastName = "identity",
        firstName = "has",
      ),
    ).id
  }

  override fun baseRequestBuilder(): WebTestClient.RequestHeadersSpec<*> = webTestClient.post()
    .uri("/contact/$savedContactId/identities")
    .accept(MediaType.APPLICATION_JSON)
    .contentType(MediaType.APPLICATION_JSON)
    .bodyValue(aMinimalRequest())

  @ParameterizedTest
  @CsvSource(
    value = [
      "identities[0].identityType must not be null;{\"identities\": [{\"identityType\": null, \"identityValue\": \"0123456789\"}], \"createdBy\": \"created\"}",
      "identities[0].identityType must not be null;{\"identities\": [{\"identityValue\": \"0123456789\"}], \"createdBy\": \"created\"}",
      "identities[0].identityValue must not be null;{\"identities\": [{\"identityType\": \"DL\", \"identityValue\": null}], \"createdBy\": \"created\"}",
      "identities[0].identityValue must not be null;{\"identities\": [{\"identityType\": \"DL\"}], \"createdBy\": \"created\"}",
      "createdBy must not be null;{\"identities\": [{\"identityType\": \"DL\", \"identityValue\": \"0123456789\"}], \"createdBy\": null}",
      "createdBy must not be null;{\"identities\": [{\"identityType\": \"DL\", \"identityValue\": \"0123456789\"}]}",
      "identities must not be null;{\"identities\": null, \"createdBy\": \"created\"}",
      "identities must not be null;{\"createdBy\": \"created\"}",
    ],
    delimiter = ';',
  )
  fun `should return bad request if required fields are null`(expectedMessage: String, json: String) {
    val errors = webTestClient.post()
      .uri("/contact/$savedContactId/identities")
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
  fun `should enforce field constraints`(expectedMessage: String, request: CreateMultipleIdentitiesRequest) {
    val errors = webTestClient.post()
      .uri("/contact/$savedContactId/identities")
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
    val request = aMinimalRequest().copy(
      identities = listOf(
        IdentityDocument(
          identityType = "PNC",
          identityValue = "1923/1Z34567A",
        ),
      ),
    )
    val errors = webTestClient.post()
      .uri("/contact/$savedContactId/identities")
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
  fun `should not create the identities if the type is unknown`() {
    val request = CreateMultipleIdentitiesRequest(
      identities = listOf(
        IdentityDocument(
          identityType = "MACRO CARD",
          identityValue = "DL123456789",
        ),
      ),
      createdBy = "created",
    )

    val errors = webTestClient.post()
      .uri("/contact/$savedContactId/identities")
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
    val request = CreateMultipleIdentitiesRequest(
      identities = listOf(
        IdentityDocument(
          identityType = "NHS",
          identityValue = "Is active is false",
        ),
      ),
      createdBy = "created",
    )

    val errors = webTestClient.post()
      .uri("/contact/$savedContactId/identities")
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
      .uri("/contact/-321/identities")
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

  @ParameterizedTest
  @ValueSource(strings = ["ROLE_CONTACTS_ADMIN", "ROLE_CONTACTS__RW"])
  fun `should create all the identity documents`(role: String) {
    val request = CreateMultipleIdentitiesRequest(
      identities = listOf(
        IdentityDocument(
          identityType = "DL",
          identityValue = "DL123456789",
          issuingAuthority = "DVLA",
        ),
        IdentityDocument(
          identityType = "PASS",
          identityValue = "P897654312",
          issuingAuthority = null,
        ),
      ),
      createdBy = "created",
    )

    val created = testAPIClient.createMultipleContactIdentityDocuments(savedContactId, request, role)

    val drivingLicence = created.find { it.identityType == "DL" }!!

    with(drivingLicence) {
      assertThat(identityType).isEqualTo("DL")
      assertThat(identityTypeDescription).isEqualTo("Driving Licence")
      assertThat(identityValue).isEqualTo("DL123456789")
      assertThat(issuingAuthority).isEqualTo("DVLA")
      assertThat(createdBy).isEqualTo(request.createdBy)
      assertThat(createdTime).isNotNull()
      stubEvents.assertHasEvent(
        event = OutboundEvent.CONTACT_IDENTITY_CREATED,
        additionalInfo = ContactIdentityInfo(contactIdentityId, Source.DPS),
        personReference = PersonReference(savedContactId),
      )
    }

    val passport = created.find { it.identityType == "PASS" }!!
    with(passport) {
      assertThat(identityType).isEqualTo("PASS")
      assertThat(identityTypeDescription).isEqualTo("Passport Number")
      assertThat(identityValue).isEqualTo("P897654312")
      assertThat(issuingAuthority).isNull()
      assertThat(createdBy).isEqualTo(request.createdBy)
      assertThat(createdTime).isNotNull()
      stubEvents.assertHasEvent(
        event = OutboundEvent.CONTACT_IDENTITY_CREATED,
        additionalInfo = ContactIdentityInfo(contactIdentityId, Source.DPS),
        personReference = PersonReference(savedContactId),
      )
    }
  }

  companion object {
    @JvmStatic
    fun allFieldConstraintViolations(): List<Arguments> {
      val baseDocument = IdentityDocument(
        identityType = "DL",
        identityValue = "DL123456789",
        issuingAuthority = "DVLA",
      )
      val baseRequest = CreateMultipleIdentitiesRequest(
        identities = listOf(baseDocument),
        createdBy = "created",
      )
      return listOf(
        Arguments.of(
          "identities[0].identityType must be <= 12 characters",
          baseRequest.copy(identities = listOf(baseDocument.copy("".padStart(13, 'X')))),
        ),
        Arguments.of(
          "identities[0].identityValue must be <= 20 characters",
          baseRequest.copy(identities = listOf(baseDocument.copy(identityValue = "".padStart(21, 'X')))),
        ),
        Arguments.of(
          "identities[0].issuingAuthority must be <= 40 characters",
          baseRequest.copy(identities = listOf(baseDocument.copy(issuingAuthority = "".padStart(41, 'X')))),
        ),
        Arguments.of(
          "createdBy must be <= 100 characters",
          baseRequest.copy(createdBy = "".padStart(101, 'X')),
        ),
        Arguments.of(
          "identities must have at least 1 item",
          baseRequest.copy(identities = emptyList()),
        ),
      )
    }

    private fun aMinimalRequest() = CreateMultipleIdentitiesRequest(
      identities = listOf(
        IdentityDocument(
          identityType = "DL",
          identityValue = "DL123456789",
        ),
      ),
      createdBy = "created",
    )
  }
}
