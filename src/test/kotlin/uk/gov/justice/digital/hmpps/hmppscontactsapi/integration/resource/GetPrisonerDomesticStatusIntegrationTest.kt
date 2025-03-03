package uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.SecureAPIIntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.CreateOrUpdatePrisonerDomesticStatusRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.PrisonerDomesticStatusResponse

class GetPrisonerDomesticStatusIntegrationTest : SecureAPIIntegrationTestBase() {

  private val prisonerNumber = "A1234BC"
  private var domesticStatusId = 0L

  override val allowedRoles: Set<String> = setOf("ROLE_CONTACTS_ADMIN", "ROLE_CONTACTS__RW", "ROLE_CONTACTS__R")

  override fun baseRequestBuilder(): WebTestClient.RequestHeadersSpec<*> = webTestClient.get()
    .uri("/prisoner/A1234BC/domestic-status")

  var expectedResponse: PrisonerDomesticStatusResponse? = null

  @BeforeEach
  fun initialiseData() {
    val request = createRequest()
    expectedResponse = PrisonerDomesticStatusResponse(
      id = 2L,
      domesticStatusCode = "M",
      domesticStatusDescription = "Married or in civil partnership",
      active = true,
      createdBy = "test-user",
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
    domesticStatusId = response!!.id
  }

  @Test
  fun `should return 404 when prisoner domestic status does not exist`() {
    webTestClient.get()
      .uri("/prisoner/A1234EE/domestic-status")
      .headers(setAuthorisation(roles = listOf("ROLE_CONTACTS_ADMIN")))
      .exchange()
      .expectStatus()
      .isNotFound
  }

  @ParameterizedTest
  @ValueSource(strings = ["ROLE_CONTACTS_ADMIN", "ROLE_CONTACTS__R", "ROLE_CONTACTS__RW"])
  fun `should return domestic status when user has roles`(role: String) {
    val expectedResponse = PrisonerDomesticStatusResponse(
      id = 1L,
      domesticStatusCode = "M",
      domesticStatusDescription = "Married or in civil partnership",
      active = true,
      createdBy = "test-user",
    )

    val response = webTestClient.get()
      .uri("/prisoner/A1234BC/domestic-status")
      .headers(setAuthorisation(roles = listOf(role)))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON_VALUE)
      .expectBody(PrisonerDomesticStatusResponse::class.java)
      .returnResult().responseBody

    assertThat(response).isNotNull
    assertThat(response).usingRecursiveComparison()
      .ignoringFields("id", "createdTime")
      .isEqualTo(expectedResponse)
  }

  private fun createRequest() = CreateOrUpdatePrisonerDomesticStatusRequest(
    domesticStatusCode = "M",
    updatedBy = "test-user",
  )
}
