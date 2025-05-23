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
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.ContactRelationship
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.CreateContactRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.restrictions.CreatePrisonerContactRestrictionRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.PersonReference
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.PrisonerContactRestrictionInfo
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.Source
import uk.gov.justice.digital.hmpps.hmppscontactsapi.util.StubUser
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.time.LocalDate

class CreatePrisonerContactRestrictionIntegrationTest : SecureAPIIntegrationTestBase() {
  private var savedPrisonerContactId = 0L
  private var savedContactId = 0L
  private val prisonerNumberCreatedAgainst = "A1234AA"

  override val allowedRoles: Set<String> = setOf("ROLE_CONTACTS_ADMIN", "ROLE_CONTACTS__RW")

  @BeforeEach
  fun initialiseData() {
    setCurrentUser(StubUser.CREATING_USER)
    stubPrisonSearchWithResponse(prisonerNumberCreatedAgainst)
    val created = testAPIClient.createAContactWithARelationship(
      CreateContactRequest(
        lastName = "last",
        firstName = "first",
        relationship = ContactRelationship(
          prisonerNumber = prisonerNumberCreatedAgainst,
          relationshipTypeCode = "S",
          relationshipToPrisonerCode = "FRI",
          isNextOfKin = true,
          isEmergencyContact = true,
          isApprovedVisitor = false,
          comments = "Some comments",
        ),
      ),
    )
    savedPrisonerContactId = created.createdRelationship!!.prisonerContactId
    savedContactId = created.createdContact.id
  }

  override fun baseRequestBuilder(): WebTestClient.RequestHeadersSpec<*> = webTestClient.post()
    .uri("/prisoner-contact/$savedPrisonerContactId/restriction")
    .accept(MediaType.APPLICATION_JSON)
    .contentType(MediaType.APPLICATION_JSON)
    .bodyValue(aMinimalRequest())

  @ParameterizedTest
  @CsvSource(
    value = [
      "restrictionType must not be null;{\"restrictionType\": null, \"startDate\": \"2020-01-01\"}",
      "restrictionType must not be null;{\"startDate\": \"2020-01-01\"}",
      "startDate must not be null;{\"restrictionType\": \"BAN\", \"startDate\": null}",
      "startDate must not be null;{\"restrictionType\": \"BAN\"}",
    ],
    delimiter = ';',
  )
  fun `should return bad request if required fields are null`(expectedMessage: String, json: String) {
    val errors = webTestClient.post()
      .uri("/prisoner-contact/$savedPrisonerContactId/restriction")
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
    stubEvents.assertHasNoEvents(event = OutboundEvent.PRISONER_CONTACT_RESTRICTION_CREATED)
  }

  @ParameterizedTest
  @MethodSource("allFieldConstraintViolations")
  fun `should enforce field constraints`(expectedMessage: String, request: CreatePrisonerContactRestrictionRequest) {
    val errors = webTestClient.post()
      .uri("/prisoner-contact/$savedPrisonerContactId/restriction")
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
    stubEvents.assertHasNoEvents(event = OutboundEvent.PRISONER_CONTACT_RESTRICTION_CREATED)
  }

  @Test
  fun `should not create the restriction if the prisoner contact is not found`() {
    val request = aMinimalRequest()

    val errors = webTestClient.post()
      .uri("/prisoner-contact/-321/restriction")
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

    assertThat(errors.userMessage).isEqualTo("Entity not found : Prisoner contact (-321) could not be found")
    stubEvents.assertHasNoEvents(event = OutboundEvent.PRISONER_CONTACT_RESTRICTION_CREATED)
  }

  @Test
  fun `should not create the restriction if the type is not valid`() {
    val request = aMinimalRequest().copy(restrictionType = "FOO")

    val errors = webTestClient.post()
      .uri("/prisoner-contact/$savedPrisonerContactId/restriction")
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

    assertThat(errors.userMessage).isEqualTo("Validation failure: Unsupported restriction type (FOO)")
    stubEvents.assertHasNoEvents(event = OutboundEvent.PRISONER_CONTACT_RESTRICTION_CREATED)
  }

  @Test
  fun `should create the restriction with minimal fields`() {
    val request = CreatePrisonerContactRestrictionRequest(
      restrictionType = "BAN",
      startDate = LocalDate.of(2020, 1, 1),
      expiryDate = null,
      comments = null,
    )

    val created = testAPIClient.createPrisonerContactRestriction(savedPrisonerContactId, request)

    with(created) {
      assertThat(prisonerContactRestrictionId).isGreaterThan(0)
      assertThat(prisonerContactId).isEqualTo(savedPrisonerContactId)
      assertThat(contactId).isEqualTo(savedContactId)
      assertThat(prisonerNumber).isEqualTo(prisonerNumberCreatedAgainst)
      assertThat(restrictionType).isEqualTo(request.restrictionType)
      assertThat(startDate).isEqualTo(request.startDate)
      assertThat(expiryDate).isNull()
      assertThat(comments).isNull()
      assertThat(enteredByUsername).isEqualTo("created")
      assertThat(enteredByDisplayName).isEqualTo("Created")
      assertThat(createdBy).isEqualTo("created")
      assertThat(createdTime).isNotNull()
    }

    stubEvents.assertHasEvent(
      event = OutboundEvent.PRISONER_CONTACT_RESTRICTION_CREATED,
      additionalInfo = PrisonerContactRestrictionInfo(created.prisonerContactRestrictionId, Source.DPS, "created", "BXI"),
      personReference = PersonReference(dpsContactId = savedContactId, nomsNumber = prisonerNumberCreatedAgainst),
    )
  }

  @ParameterizedTest
  @ValueSource(strings = ["ROLE_CONTACTS_ADMIN", "ROLE_CONTACTS__RW"])
  fun `should create the restriction with all fields`(role: String) {
    setCurrentUser(StubUser.CREATING_USER.copy(roles = listOf(role)))
    val request = CreatePrisonerContactRestrictionRequest(
      restrictionType = "BAN",
      startDate = LocalDate.of(2020, 1, 1),
      expiryDate = LocalDate.of(2021, 2, 2),
      comments = "Some comments",
    )

    val created = testAPIClient.createPrisonerContactRestriction(savedPrisonerContactId, request)

    with(created) {
      assertThat(prisonerContactRestrictionId).isGreaterThan(0)
      assertThat(prisonerContactId).isEqualTo(savedPrisonerContactId)
      assertThat(contactId).isEqualTo(savedContactId)
      assertThat(prisonerNumber).isEqualTo(prisonerNumberCreatedAgainst)
      assertThat(restrictionType).isEqualTo(request.restrictionType)
      assertThat(startDate).isEqualTo(request.startDate)
      assertThat(expiryDate).isEqualTo(request.expiryDate)
      assertThat(comments).isEqualTo(request.comments)
      assertThat(enteredByUsername).isEqualTo("created")
      assertThat(enteredByDisplayName).isEqualTo("Created")
      assertThat(createdBy).isEqualTo("created")
      assertThat(createdTime).isNotNull()
    }

    stubEvents.assertHasEvent(
      event = OutboundEvent.PRISONER_CONTACT_RESTRICTION_CREATED,
      additionalInfo = PrisonerContactRestrictionInfo(created.prisonerContactRestrictionId, Source.DPS, "created", "BXI"),
      personReference = PersonReference(dpsContactId = savedContactId, nomsNumber = prisonerNumberCreatedAgainst),
    )
  }

  companion object {
    @JvmStatic
    fun allFieldConstraintViolations(): List<Arguments> = listOf(
      Arguments.of("comments must be <= 240 characters", aMinimalRequest().copy(comments = "".padStart(241, 'X'))),
    )

    private fun aMinimalRequest() = CreatePrisonerContactRestrictionRequest(
      restrictionType = "BAN",
      startDate = LocalDate.of(2020, 1, 1),
      expiryDate = null,
      comments = null,
    )
  }
}
