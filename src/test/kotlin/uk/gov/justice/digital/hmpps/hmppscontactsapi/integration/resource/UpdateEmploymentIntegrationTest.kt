package uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.SecureAPIIntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.CreateContactRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.employment.CreateEmploymentRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.employment.UpdateEmploymentRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.EmploymentInfo
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.PersonReference
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.Source
import uk.gov.justice.digital.hmpps.hmppscontactsapi.util.StubUser
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

class UpdateEmploymentIntegrationTest : SecureAPIIntegrationTestBase() {
  private var savedContactId = 0L
  private var savedEmploymentId = 0L

  override val allowedRoles: Set<String> = setOf("ROLE_CONTACTS_ADMIN", "ROLE_CONTACTS__RW")

  @BeforeEach
  fun initialiseData() {
    setCurrentUser(StubUser.CREATING_USER)
    savedContactId = testAPIClient.createAContact(
      CreateContactRequest(
        lastName = "employment",
        firstName = "has",
      ),
    ).id
    stubOrganisationSummary(999)
    stubOrganisationSummary(666)
    savedEmploymentId = testAPIClient.createAnEmployment(
      savedContactId,
      CreateEmploymentRequest(
        organisationId = 999,
        isActive = true,
      ),
    ).employmentId
    setCurrentUser(StubUser.UPDATING_USER)
  }

  override fun baseRequestBuilder(): WebTestClient.RequestHeadersSpec<*> = webTestClient.put()
    .uri("/contact/$savedContactId/employment/$savedEmploymentId")
    .accept(MediaType.APPLICATION_JSON)
    .contentType(MediaType.APPLICATION_JSON)
    .bodyValue(aMinimalRequest())

  @ParameterizedTest
  @CsvSource(
    value = [
      "organisationId must not be null;{\"organisationId\": null, \"isActive\": true}",
      "organisationId must not be null;{\"isActive\": true}",
      "isActive must not be null;{\"organisationId\": 99, \"isActive\": null}",
      "isActive must not be null;{\"organisationId\": 99}",
    ],
    delimiter = ';',
  )
  fun `should return bad request if required fields are null`(expectedMessage: String, json: String) {
    val errors = webTestClient.put()
      .uri("/contact/$savedContactId/employment/$savedEmploymentId")
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
  }

  @Test
  fun `should not update the employment if the contact is not found`() {
    val request = aMinimalRequest()

    val errors = webTestClient.put()
      .uri("/contact/-321/employment/$savedEmploymentId")
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
    stubEvents.assertHasNoEvents(event = OutboundEvent.EMPLOYMENT_UPDATED)
  }

  @Test
  fun `should not update the employment if the employment is not found`() {
    val request = aMinimalRequest()

    val errors = webTestClient.put()
      .uri("/contact/$savedContactId/employment/-321")
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

    assertThat(errors.userMessage).isEqualTo("Entity not found : Employment (-321) not found")

    stubEvents.assertHasNoEvents(event = OutboundEvent.EMPLOYMENT_UPDATED)
  }

  @ParameterizedTest
  @ValueSource(strings = ["ROLE_CONTACTS_ADMIN", "ROLE_CONTACTS__RW"])
  fun `should update the employment`(role: String) {
    setCurrentUser(StubUser.UPDATING_USER.copy(roles = listOf(role)))
    val request = aMinimalRequest()

    val updated = testAPIClient.updateAnEmployment(savedContactId, savedEmploymentId, request)

    stubEvents.assertHasEvent(
      event = OutboundEvent.EMPLOYMENT_UPDATED,
      additionalInfo = EmploymentInfo(updated.employmentId, Source.DPS, "updated"),
      personReference = PersonReference(dpsContactId = updated.contactId),
    )
  }

  companion object {
    private fun aMinimalRequest() = UpdateEmploymentRequest(
      organisationId = 666,
      isActive = false,
    )
  }
}
