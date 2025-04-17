package uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.SecureAPIIntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.CreateContactRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.employment.CreateEmploymentRequest
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

class GetEmploymentIntegrationTest : SecureAPIIntegrationTestBase() {
  private var savedContactId = 0L
  private var savedEmploymentId = 0L

  override val allowedRoles: Set<String> = setOf("ROLE_CONTACTS_ADMIN", "ROLE_CONTACTS__RW", "ROLE_CONTACTS__R")

  @BeforeEach
  fun initialiseData() {
    savedContactId = testAPIClient.createAContact(
      CreateContactRequest(
        lastName = "employment",
        firstName = "has",
      ),
    ).id
    stubOrganisationSummary(999)
    savedEmploymentId = testAPIClient.createAnEmployment(
      savedContactId,
      CreateEmploymentRequest(
        organisationId = 999,
        isActive = true,
        createdBy = "created",
      ),
    ).employmentId
  }

  override fun baseRequestBuilder(): WebTestClient.RequestHeadersSpec<*> = webTestClient.get()
    .uri("/contact/$savedContactId/employment/$savedEmploymentId")
    .accept(MediaType.APPLICATION_JSON)

  @Test
  fun `should return not found if contact missing`() {
    val errors = webTestClient.get()
      .uri("/contact/-321/employment/$savedEmploymentId")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CONTACTS_ADMIN")))
      .exchange()
      .expectStatus()
      .isNotFound
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody!!

    assertThat(errors.userMessage).isEqualTo("Entity not found : Contact (-321) not found")
  }

  @Test
  fun `should return not found if employment missing`() {
    val errors = webTestClient.get()
      .uri("/contact/$savedContactId/employment/-321")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CONTACTS_ADMIN")))
      .exchange()
      .expectStatus()
      .isNotFound
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody!!

    assertThat(errors.userMessage).isEqualTo("Entity not found : Employment (-321) not found")
  }

  @ParameterizedTest
  @ValueSource(strings = ["ROLE_CONTACTS_ADMIN", "ROLE_CONTACTS__R", "ROLE_CONTACTS__RW"])
  fun `should get the employment`(role: String) {
    val details = testAPIClient.getAnEmployment(savedContactId, savedEmploymentId, role)
    assertThat(details.employmentId).isEqualTo(savedEmploymentId)
    assertThat(details.employer.organisationId).isEqualTo(999)
    assertThat(details.isActive).isTrue()
  }
}
