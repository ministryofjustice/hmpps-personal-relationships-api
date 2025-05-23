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
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.restrictions.CreateContactRestrictionRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.restrictions.UpdateContactRestrictionRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.ContactRestrictionInfo
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.PersonReference
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.Source
import uk.gov.justice.digital.hmpps.hmppscontactsapi.util.StubUser
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.time.LocalDate

class UpdateContactRestrictionIntegrationTest : SecureAPIIntegrationTestBase() {
  private var savedContactId = 0L
  private var savedContactRestrictionId = 0L

  override val allowedRoles: Set<String> = setOf("ROLE_CONTACTS_ADMIN", "ROLE_CONTACTS__RW")

  @BeforeEach
  fun initialiseData() {
    setCurrentUser(StubUser.CREATING_USER)
    savedContactId = testAPIClient.createAContact(
      CreateContactRequest(
        lastName = "last",
        firstName = "first",
      ),

    ).id
    savedContactRestrictionId = testAPIClient.createContactGlobalRestriction(
      savedContactId,
      CreateContactRestrictionRequest(
        restrictionType = "BAN",
        startDate = LocalDate.of(2020, 1, 1),
        expiryDate = LocalDate.of(2022, 2, 2),
        comments = "Some comments",
      ),

    ).contactRestrictionId
    setCurrentUser(StubUser.UPDATING_USER)
  }

  override fun baseRequestBuilder(): WebTestClient.RequestHeadersSpec<*> = webTestClient.put()
    .uri("/contact/$savedContactId/restriction/$savedContactRestrictionId")
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
      .uri("/contact/$savedContactId/restriction/$savedContactRestrictionId")
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
      event = OutboundEvent.CONTACT_RESTRICTION_UPDATED,
    )
  }

  @ParameterizedTest
  @MethodSource("allFieldConstraintViolations")
  fun `should enforce field constraints`(expectedMessage: String, request: UpdateContactRestrictionRequest) {
    val errors = webTestClient.put()
      .uri("/contact/$savedContactId/restriction/$savedContactRestrictionId")
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
  }

  @Test
  fun `should not update the restriction if the contact is not found`() {
    val request = aMinimalRequest()

    val errors = webTestClient.put()
      .uri("/contact/-321/restriction/$savedContactRestrictionId")
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

    assertThat(errors.userMessage).isEqualTo("Entity not found : Contact (-321) could not be found")
  }

  @Test
  fun `should not update the restriction if the contact restriction is not found`() {
    val request = aMinimalRequest()

    val errors = webTestClient.put()
      .uri("/contact/$savedContactId/restriction/-321")
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

    assertThat(errors.userMessage).isEqualTo("Entity not found : Contact restriction (-321) could not be found")
  }

  @Test
  fun `should not update the restriction if the type is not valid`() {
    val request = aMinimalRequest().copy(restrictionType = "FOO")

    val errors = webTestClient.put()
      .uri("/contact/$savedContactId/restriction/$savedContactRestrictionId")
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
  }

  @Test
  fun `should update the restriction with minimal fields`() {
    val request = UpdateContactRestrictionRequest(
      restrictionType = "CCTV",
      startDate = LocalDate.of(1990, 1, 1),
      expiryDate = null,
      comments = null,
    )

    val updated = testAPIClient.updateContactGlobalRestriction(
      savedContactId,
      savedContactRestrictionId,
      request,

    )

    with(updated) {
      assertThat(contactRestrictionId).isEqualTo(savedContactRestrictionId)
      assertThat(contactId).isEqualTo(savedContactId)
      assertThat(restrictionType).isEqualTo(request.restrictionType)
      assertThat(startDate).isEqualTo(request.startDate)
      assertThat(expiryDate).isNull()
      assertThat(comments).isNull()
      assertThat(enteredByUsername).isEqualTo("updated")
      assertThat(enteredByDisplayName).isEqualTo("Updated")
      assertThat(createdBy).isEqualTo("created")
      assertThat(createdTime).isNotNull()
      assertThat(updatedBy).isEqualTo("updated")
      assertThat(updatedTime).isNotNull()
    }

    stubEvents.assertHasEvent(
      event = OutboundEvent.CONTACT_RESTRICTION_UPDATED,
      additionalInfo = ContactRestrictionInfo(updated.contactRestrictionId, Source.DPS, "updated", "BXI"),
      personReference = PersonReference(dpsContactId = updated.contactId),
    )
  }

  @ParameterizedTest
  @ValueSource(strings = ["ROLE_CONTACTS_ADMIN", "ROLE_CONTACTS__RW"])
  fun `should update the restriction with all fields`(role: String) {
    setCurrentUser(StubUser.UPDATING_USER.copy(roles = listOf(role)))
    val request = UpdateContactRestrictionRequest(
      restrictionType = "CCTV",
      startDate = LocalDate.of(1990, 1, 1),
      expiryDate = LocalDate.of(1992, 2, 2),
      comments = "Updated comments",
    )

    val updated = testAPIClient.updateContactGlobalRestriction(
      savedContactId,
      savedContactRestrictionId,
      request,
    )

    with(updated) {
      assertThat(contactRestrictionId).isEqualTo(savedContactRestrictionId)
      assertThat(contactId).isEqualTo(savedContactId)
      assertThat(restrictionType).isEqualTo(request.restrictionType)
      assertThat(startDate).isEqualTo(request.startDate)
      assertThat(expiryDate).isEqualTo(request.expiryDate)
      assertThat(comments).isEqualTo(request.comments)
      assertThat(enteredByUsername).isEqualTo("updated")
      assertThat(enteredByDisplayName).isEqualTo("Updated")
      assertThat(createdBy).isEqualTo("created")
      assertThat(createdTime).isNotNull()
      assertThat(updatedBy).isEqualTo("updated")
      assertThat(updatedTime).isNotNull()
    }

    stubEvents.assertHasEvent(
      event = OutboundEvent.CONTACT_RESTRICTION_UPDATED,
      additionalInfo = ContactRestrictionInfo(updated.contactRestrictionId, Source.DPS, "updated", "BXI"),
      personReference = PersonReference(dpsContactId = updated.contactId),
    )
  }

  companion object {
    @JvmStatic
    fun allFieldConstraintViolations(): List<Arguments> = listOf(
      Arguments.of("comments must be <= 240 characters", aMinimalRequest().copy(comments = "".padStart(241, 'X'))),
    )

    private fun aMinimalRequest() = UpdateContactRestrictionRequest(
      restrictionType = "CCTV",
      startDate = LocalDate.of(1990, 1, 1),
      expiryDate = null,
      comments = null,
    )
  }
}
