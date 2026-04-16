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
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.personalrelationships.client.manage.users.UserDetails
import uk.gov.justice.digital.hmpps.personalrelationships.config.User
import uk.gov.justice.digital.hmpps.personalrelationships.integration.SecureAPIIntegrationTestBase
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.ContactRelationship
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.CreateContactRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.restrictions.CreatePrisonerContactRestrictionRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.restrictions.UpdatePrisonerContactRestrictionRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.PrisonerContactRestrictionDetails
import uk.gov.justice.digital.hmpps.personalrelationships.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.personalrelationships.service.events.PersonReference
import uk.gov.justice.digital.hmpps.personalrelationships.service.events.PrisonerContactRestrictionInfo
import uk.gov.justice.digital.hmpps.personalrelationships.service.events.Source
import uk.gov.justice.digital.hmpps.personalrelationships.util.StubUser
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.time.LocalDate

class UpdatePrisonerContactRestrictionIntegrationTest : SecureAPIIntegrationTestBase() {
  private var savedContactId = 0L
  private var savedPrisonerContactId = 0L
  private var savedPrisonerContactRestrictionId = 0L
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
    savedPrisonerContactRestrictionId = testAPIClient.createPrisonerContactRestriction(
      savedPrisonerContactId,
      CreatePrisonerContactRestrictionRequest(
        restrictionType = "BAN",
        startDate = LocalDate.of(2020, 1, 1),
        expiryDate = LocalDate.of(2022, 2, 2),
        comments = "Some comments",
      ),

    ).prisonerContactRestrictionId
    setCurrentUser(StubUser.UPDATING_USER)
  }

  override fun baseRequestBuilder(): WebTestClient.RequestHeadersSpec<*> = webTestClient.put()
    .uri("/prisoner-contact/$savedPrisonerContactId/restriction/$savedPrisonerContactRestrictionId")
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
    val errors = webTestClient.put()
      .uri("/prisoner-contact/$savedPrisonerContactId/restriction/$savedPrisonerContactRestrictionId")
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
      event = OutboundEvent.PRISONER_CONTACT_RESTRICTION_UPDATED,
    )
  }

  @ParameterizedTest
  @MethodSource("allFieldConstraintViolations")
  fun `should enforce field constraints`(expectedMessage: String, request: UpdatePrisonerContactRestrictionRequest) {
    val errors = webTestClient.put()
      .uri("/prisoner-contact/$savedPrisonerContactId/restriction/$savedPrisonerContactRestrictionId")
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
      event = OutboundEvent.PRISONER_CONTACT_RESTRICTION_UPDATED,
    )
  }

  @Test
  fun `should not update the restriction if the prisoner contact relationship is not found`() {
    val request = aMinimalRequest()

    val errors = webTestClient.put()
      .uri("/prisoner-contact/-321/restriction/$savedPrisonerContactRestrictionId")
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
    stubEvents.assertHasNoEvents(
      event = OutboundEvent.PRISONER_CONTACT_RESTRICTION_UPDATED,
    )
  }

  @Test
  fun `should not update the restriction if the prisoner contact restriction is not found`() {
    val request = aMinimalRequest()

    val errors = webTestClient.put()
      .uri("/prisoner-contact/$savedPrisonerContactId/restriction/-321")
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

    assertThat(errors.userMessage).isEqualTo("Entity not found : Prisoner contact restriction (-321) could not be found")
    stubEvents.assertHasNoEvents(
      event = OutboundEvent.PRISONER_CONTACT_RESTRICTION_UPDATED,
    )
  }

  @Test
  fun `should not update the restriction if the type is not valid`() {
    val request = aMinimalRequest().copy(restrictionType = "FOO")

    val errors = webTestClient.put()
      .uri("/prisoner-contact/$savedPrisonerContactId/restriction/$savedPrisonerContactRestrictionId")
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
    stubEvents.assertHasNoEvents(
      event = OutboundEvent.PRISONER_CONTACT_RESTRICTION_UPDATED,
    )
  }

  @Test
  fun `should update the restriction with minimal fields`() {
    stubGetUserByUsername(UserDetails("updated", "Updated User", "BXI"))
    val request = UpdatePrisonerContactRestrictionRequest(
      restrictionType = "CCTV",
      startDate = LocalDate.of(1990, 1, 1),
      expiryDate = null,
      comments = null,
    )

    val updated = testAPIClient.updatePrisonerContactRestriction(
      savedPrisonerContactId,
      savedPrisonerContactRestrictionId,
      request,

    )

    with(updated) {
      assertThat(prisonerContactRestrictionId).isEqualTo(savedPrisonerContactRestrictionId)
      assertThat(contactId).isEqualTo(savedContactId)
      assertThat(prisonerContactId).isEqualTo(prisonerContactId)
      assertThat(prisonerNumber).isEqualTo(prisonerNumberCreatedAgainst)
      assertThat(restrictionType).isEqualTo(request.restrictionType)
      assertThat(startDate).isEqualTo(request.startDate)
      assertThat(expiryDate).isNull()
      assertThat(comments).isNull()
      assertThat(enteredByUsername).isEqualTo("updated")
      assertThat(enteredByDisplayName).isEqualTo("Updated User")
      assertThat(createdBy).isEqualTo("created")
      assertThat(createdTime).isNotNull()
      assertThat(updatedBy).isEqualTo("updated")
      assertThat(updatedTime).isNotNull()
    }

    stubEvents.assertHasEvent(
      event = OutboundEvent.PRISONER_CONTACT_RESTRICTION_UPDATED,
      additionalInfo = PrisonerContactRestrictionInfo(prisonerContactRestrictionId = updated.prisonerContactRestrictionId, prisonerContactId = savedPrisonerContactId, Source.DPS, "updated", "BXI"),
      personReference = PersonReference(dpsContactId = savedContactId, nomsNumber = prisonerNumberCreatedAgainst),
    )

    assertCustomEvent(updated, Source.DPS, User("updated", "BXI"), "CCTV")
  }

  @ParameterizedTest
  @ValueSource(strings = ["ROLE_CONTACTS_ADMIN", "ROLE_CONTACTS__RW"])
  fun `should update the restriction with all fields`(role: String) {
    setCurrentUser(StubUser.UPDATING_USER.copy(roles = listOf(role)))
    stubGetUserByUsername(UserDetails("updated", "Updated User", "BXI"))
    val request = UpdatePrisonerContactRestrictionRequest(
      restrictionType = "CCTV",
      startDate = LocalDate.of(1990, 1, 1),
      expiryDate = LocalDate.of(1992, 2, 2),
      comments = "Updated comments",
    )

    val updated = testAPIClient.updatePrisonerContactRestriction(
      savedPrisonerContactId,
      savedPrisonerContactRestrictionId,
      request,
    )

    with(updated) {
      assertThat(prisonerContactRestrictionId).isEqualTo(savedPrisonerContactRestrictionId)
      assertThat(contactId).isEqualTo(savedContactId)
      assertThat(prisonerContactId).isEqualTo(prisonerContactId)
      assertThat(prisonerNumber).isEqualTo(prisonerNumberCreatedAgainst)
      assertThat(restrictionType).isEqualTo(request.restrictionType)
      assertThat(startDate).isEqualTo(request.startDate)
      assertThat(expiryDate).isEqualTo(request.expiryDate)
      assertThat(comments).isEqualTo(request.comments)
      assertThat(enteredByUsername).isEqualTo("updated")
      assertThat(enteredByDisplayName).isEqualTo("Updated User")
      assertThat(createdBy).isEqualTo("created")
      assertThat(createdTime).isNotNull()
      assertThat(updatedBy).isEqualTo("updated")
      assertThat(updatedTime).isNotNull()
    }

    stubEvents.assertHasEvent(
      event = OutboundEvent.PRISONER_CONTACT_RESTRICTION_UPDATED,
      additionalInfo = PrisonerContactRestrictionInfo(prisonerContactRestrictionId = updated.prisonerContactRestrictionId, prisonerContactId = savedPrisonerContactId, source = Source.DPS, username = "updated", activeCaseLoadId = "BXI"),
      personReference = PersonReference(dpsContactId = savedContactId, nomsNumber = prisonerNumberCreatedAgainst),
    )

    assertCustomEvent(updated, Source.DPS, User("updated", "BXI"), "CCTV")
  }

  companion object {
    @JvmStatic
    fun allFieldConstraintViolations(): List<Arguments> = listOf(
      Arguments.of("comments must be <= 240 characters", aMinimalRequest().copy(comments = "".padStart(241, 'X'))),
    )

    private fun aMinimalRequest() = UpdatePrisonerContactRestrictionRequest(
      restrictionType = "CCTV",
      startDate = LocalDate.of(1990, 1, 1),
      expiryDate = null,
      comments = null,
    )
  }

  private fun assertCustomEvent(updatedPrisonerContactRestrictionDetails: PrisonerContactRestrictionDetails, source: Source, user: User, restrictionType: String) {
    verify(telemetryContactCustomEventService, times(1)).trackUpdatePrisonerContactRestrictionEvent(updatedPrisonerContactRestrictionDetails, source, user)
    verify(telemetryClient, times(1)).trackEvent(
      "prisoner-contact-restriction-updated",
      mapOf(
        "description" to "A prisoner contact restriction has been updated",
        "source" to source.name,
        "username" to user.username,
        "active_caseload_id" to user.activeCaseLoadId,
        "contact_id" to updatedPrisonerContactRestrictionDetails.contactId.toString(),
        "restriction_code" to restrictionType,
        "prisoner_contact_restriction_id" to updatedPrisonerContactRestrictionDetails.prisonerContactRestrictionId.toString(),
        "prisoner_number" to updatedPrisonerContactRestrictionDetails.prisonerNumber,
      ),
      null,
    )
  }
}
