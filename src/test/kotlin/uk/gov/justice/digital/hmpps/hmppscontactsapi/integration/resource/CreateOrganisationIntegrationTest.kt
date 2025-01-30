package uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.SecureAPIIntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.CreateOrganisationRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.OrganisationDetails
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.time.LocalDateTime

class CreateOrganisationIntegrationTest : SecureAPIIntegrationTestBase() {

  override val allowedRoles: Set<String> = setOf("ROLE_CONTACTS_ADMIN", "ROLE_CONTACTS__RW")

  override fun baseRequestBuilder(): WebTestClient.RequestHeadersSpec<*> = webTestClient.post()
    .uri("/organisation")
    .bodyValue(createValidOrganisationRequest())

  @Test
  fun `should return bad request when organisation name exceeds 40 characters`() {
    val request = createValidOrganisationRequest().copy(
      organisationName = "A".repeat(41),
    )

    val errors = webTestClient.post()
      .uri("/organisation")
      .headers(setAuthorisation(roles = listOf("ROLE_CONTACTS__RW")))
      .bodyValue(request)
      .exchange()
      .expectStatus()
      .isBadRequest
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody!!

    assertThat(errors.userMessage).isEqualTo("Validation failure(s): organisationName must be <= 40 characters")
  }

  @Test
  fun `should return bad request when programme number exceeds 40 characters`() {
    val request = createValidOrganisationRequest().copy(
      programmeNumber = "A".repeat(41),
    )

    val errors = webTestClient.post()
      .uri("/organisation")
      .headers(setAuthorisation(roles = listOf("ROLE_CONTACTS__RW")))
      .bodyValue(request)
      .exchange()
      .expectStatus()
      .isBadRequest
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody!!

    assertThat(errors.userMessage).isEqualTo("Validation failure(s): programmeNumber must be <= 40 characters")
  }

  @Test
  fun `should return bad request when VAT number exceeds 12 characters`() {
    val request = createValidOrganisationRequest().copy(
      vatNumber = "A".repeat(13),
    )

    val errors = webTestClient.post()
      .uri("/organisation")
      .headers(setAuthorisation(roles = listOf("ROLE_CONTACTS__RW")))
      .bodyValue(request)
      .exchange()
      .expectStatus()
      .isBadRequest
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody!!

    assertThat(errors.userMessage).isEqualTo("Validation failure(s): vatNumber must be <= 12 characters")
  }

  @Test
  fun `should return bad request when caseload ID exceeds 6 characters`() {
    val request = createValidOrganisationRequest().copy(
      caseloadId = "A".repeat(7),
    )

    val errors = webTestClient.post()
      .uri("/organisation")
      .headers(setAuthorisation(roles = listOf("ROLE_CONTACTS__RW")))
      .bodyValue(request)
      .exchange()
      .expectStatus()
      .isBadRequest
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody!!

    assertThat(errors.userMessage).isEqualTo("Validation failure(s): caseloadId must be <= 6 characters")
  }

  @Test
  fun `should return bad request when comments exceed 240 characters`() {
    val request = createValidOrganisationRequest().copy(
      comments = "A".repeat(241),
    )

    val errors = webTestClient.post()
      .uri("/organisation")
      .headers(setAuthorisation(roles = listOf("ROLE_CONTACTS__RW")))
      .bodyValue(request)
      .exchange()
      .expectStatus()
      .isBadRequest
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody!!

    assertThat(errors.userMessage).isEqualTo("Validation failure(s): comments must be <= 240 characters")
  }

  @Test
  fun `should return bad request when required fields are missing`() {
    val request = mapOf(
      "active" to true,
      "createdTime" to LocalDateTime.now(),
    )

    webTestClient.post()
      .uri("/organisation")
      .headers(setAuthorisation(roles = listOf("ROLE_CONTACTS__RW")))
      .bodyValue(request)
      .exchange()
      .expectStatus()
      .isBadRequest
  }

  @Test
  fun `should create organisation successfully with valid data`() {
    val request = createValidOrganisationRequest()

    val response = webTestClient.post()
      .uri("/organisation")
      .headers(setAuthorisation(roles = listOf("ROLE_CONTACTS__RW")))
      .bodyValue(request)
      .exchange()
      .expectStatus()
      .isCreated
      .expectBody(OrganisationDetails::class.java)
      .returnResult()
      .responseBody

    assertThat(response).isNotNull
    with(response!!) {
      assertThat(organisationName).isEqualTo(request.organisationName)
      assertThat(programmeNumber).isEqualTo(request.programmeNumber)
      assertThat(vatNumber).isEqualTo(request.vatNumber)
      assertThat(caseloadId).isEqualTo(request.caseloadId)
      assertThat(comments).isEqualTo(request.comments)
      assertThat(active).isEqualTo(request.active)
      assertThat(deactivatedDate).isEqualTo(request.deactivatedDate)
      assertThat(createdBy).isEqualTo(request.createdBy)
      assertThat(createdTime).isNotNull()
      assertThat(organisationId).isNotNull()
    }
  }

  companion object {
    fun createValidOrganisationRequest() = CreateOrganisationRequest(
      organisationName = "Test Organisation",
      programmeNumber = "TEST01",
      vatNumber = "GB123456789",
      caseloadId = "TEST1",
      comments = "Test comments",
      active = true,
      deactivatedDate = null,
      createdBy = "test-user",
      createdTime = LocalDateTime.now(),
      updatedBy = null,
      updatedTime = null,
    )
  }
}
