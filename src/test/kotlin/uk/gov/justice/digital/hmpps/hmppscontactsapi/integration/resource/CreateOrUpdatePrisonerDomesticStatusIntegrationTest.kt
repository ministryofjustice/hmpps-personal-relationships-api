package uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppscontactsapi.helpers.prisoner
import uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.SecureAPIIntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.CreateOrUpdatePrisonerDomesticStatusRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.PrisonerDomesticStatusResponse
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.PersonReference
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.PrisonerDomesticStatus
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.Source

class CreateOrUpdatePrisonerDomesticStatusIntegrationTest : SecureAPIIntegrationTestBase() {

  private val prisonerNumber = "A1234BC"
  private val prisoner1 = prisoner(
    prisonerNumber = "A1234BC",
    prisonId = "MDI",
    firstName = "Joe",
    middleNames = "Middle",
    lastName = "Bloggs",
  )
  override val allowedRoles: Set<String> = setOf("ROLE_CONTACTS_ADMIN", "ROLE_CONTACTS__RW")

  override fun baseRequestBuilder(): WebTestClient.RequestHeadersSpec<*> = webTestClient.put()
    .uri("/prisoner/$prisonerNumber/domestic-status")
    .accept(MediaType.APPLICATION_JSON)
    .contentType(MediaType.APPLICATION_JSON)
    .bodyValue(createRequest())

  @ParameterizedTest
  @ValueSource(strings = ["ROLE_CONTACTS_ADMIN", "ROLE_CONTACTS__RW"])
  fun `should create new domestic status`(role: String) {
    stubPrisonerSearch(prisoner1)
    val request = createRequest()

    val response = webTestClient.put()
      .uri("/prisoner/$prisonerNumber/domestic-status")
      .headers(setAuthorisation(roles = listOf(role)))
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(request)
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(PrisonerDomesticStatusResponse::class.java)
      .returnResult().responseBody

    assertThat(response).isNotNull
    assertThat(response).usingRecursiveComparison()
      .ignoringFields("id", "createdTime")
      .isEqualTo(
        PrisonerDomesticStatusResponse(
          id = 2L,
          domesticStatusCode = "M",
          domesticStatusDescription = "Married or in a civil partnership",
          active = true,
          createdBy = "test-user",
        ),
      )
    stubEvents.assertHasEvent(
      event = OutboundEvent.PRISONER_DOMESTIC_STATUS_CREATED,
      additionalInfo = PrisonerDomesticStatus(response!!.id, Source.DPS),
      personReference = PersonReference(nomsNumber = prisonerNumber),
    )
  }

  @Test
  fun `should allow null for domestic status`() {
    stubPrisonerSearch(prisoner1)
    val request = CreateOrUpdatePrisonerDomesticStatusRequest(
      domesticStatusCode = null,
      requestedBy = "test-user",
    )

    val response = webTestClient.put()
      .uri("/prisoner/$prisonerNumber/domestic-status")
      .headers(setAuthorisation(roles = listOf("ROLE_CONTACTS__RW")))
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(request)
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(PrisonerDomesticStatusResponse::class.java)
      .returnResult().responseBody

    assertThat(response).isNotNull
    assertThat(response).usingRecursiveComparison()
      .ignoringFields("id", "createdTime")
      .isEqualTo(
        PrisonerDomesticStatusResponse(
          id = 2L,
          domesticStatusCode = null,
          domesticStatusDescription = null,
          active = true,
          createdBy = "test-user",
        ),
      )
    stubEvents.assertHasEvent(
      event = OutboundEvent.PRISONER_DOMESTIC_STATUS_CREATED,
      additionalInfo = PrisonerDomesticStatus(response!!.id, Source.DPS),
      personReference = PersonReference(nomsNumber = prisonerNumber),
    )
  }

  @Test
  fun `should update existing domestic status`() {
    stubPrisonerSearch(prisoner1)
    val request = createRequest()
    val response = webTestClient.put()
      .uri("/prisoner/$prisonerNumber/domestic-status")
      .headers(setAuthorisation(roles = listOf("ROLE_CONTACTS_ADMIN")))
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(request)
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(PrisonerDomesticStatusResponse::class.java)
      .returnResult().responseBody

    assertThat(response).isNotNull

    val updateResponse = webTestClient.put()
      .uri("/prisoner/$prisonerNumber/domestic-status")
      .headers(setAuthorisation(roles = listOf("ROLE_CONTACTS__RW")))
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(request)
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(PrisonerDomesticStatusResponse::class.java)
      .returnResult().responseBody

    assertThat(updateResponse).isNotNull
    assertThat(updateResponse!!.id).isGreaterThan(response!!.id)
    assertThat(updateResponse).usingRecursiveComparison()
      .ignoringFields("id", "createdTime")
      .isEqualTo(
        PrisonerDomesticStatusResponse(
          id = 1L,
          domesticStatusCode = "M",
          domesticStatusDescription = "Married or in a civil partnership",
          active = true,
          createdBy = "test-user",
        ),
      )
    stubEvents.assertHasEvent(
      event = OutboundEvent.PRISONER_DOMESTIC_STATUS_CREATED,
      additionalInfo = PrisonerDomesticStatus(response.id, Source.DPS),
      personReference = PersonReference(nomsNumber = prisonerNumber),
    )
  }

  @Test
  fun `should return 400 when domestic status code is more than 12 characters`() {
    webTestClient.put()
      .uri("/prisoner/$prisonerNumber/domestic-status")
      .headers(setAuthorisation(roles = listOf("ROLE_CONTACTS__RW")))
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(
        CreateOrUpdatePrisonerDomesticStatusRequest(
          domesticStatusCode = "MORE THAN 12 CHARACTERS",
          requestedBy = "test-user",
        ),
      )
      .exchange()
      .expectStatus().isBadRequest
      .expectBody()
      .jsonPath("$.userMessage")
      .isEqualTo("Validation failure(s): domesticStatusCode must be less than or equal to 12 characters")
    stubEvents.assertHasNoEvents(
      event = OutboundEvent.PRISONER_DOMESTIC_STATUS_CREATED,
    )
  }

  @Test
  fun `should return 404 when domestic status code is empty`() {
    stubPrisonerSearch(prisoner1)
    webTestClient.put()
      .uri("/prisoner/$prisonerNumber/domestic-status")
      .headers(setAuthorisation(roles = listOf("ROLE_CONTACTS__RW")))
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(
        CreateOrUpdatePrisonerDomesticStatusRequest(
          domesticStatusCode = "",
          requestedBy = "test-user",
        ),
      )
      .exchange()
      .expectStatus().isNotFound
      .expectBody()
      .jsonPath("$.userMessage")
      .isEqualTo("Entity not found : No reference data found for groupCode: DOMESTIC_STS and code: ")
    stubEvents.assertHasNoEvents(
      event = OutboundEvent.PRISONER_DOMESTIC_STATUS_CREATED,
    )
  }

  @Test
  fun `should return 404 when domestic status code is invalid`() {
    stubPrisonerSearch(prisoner1)
    webTestClient.put()
      .uri("/prisoner/$prisonerNumber/domestic-status")
      .headers(setAuthorisation(roles = listOf("ROLE_CONTACTS__RW")))
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(
        CreateOrUpdatePrisonerDomesticStatusRequest(
          domesticStatusCode = "Q",
          requestedBy = "test-user",
        ),
      )
      .exchange()
      .expectStatus().isNotFound
      .expectBody()
      .jsonPath("$.userMessage")
      .isEqualTo("Entity not found : No reference data found for groupCode: DOMESTIC_STS and code: Q")
    stubEvents.assertHasNoEvents(
      event = OutboundEvent.PRISONER_DOMESTIC_STATUS_CREATED,
    )
  }

  private fun createRequest() = CreateOrUpdatePrisonerDomesticStatusRequest(
    domesticStatusCode = "M",
    requestedBy = "test-user",
  )
}
