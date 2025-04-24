package uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppscontactsapi.helpers.prisoner
import uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.SecureAPIIntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.CreateOrUpdatePrisonerDomesticStatusRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.PrisonerDomesticStatusResponse
import uk.gov.justice.digital.hmpps.hmppscontactsapi.util.StubUser

class GetPrisonerDomesticStatusIntegrationTest : SecureAPIIntegrationTestBase() {

  private val prisonerNumber = "A1234BC"
  private var domesticStatusId = 0L

  @BeforeEach
  fun setUp() {
    setCurrentUser(StubUser.READ_ONLY_USER)
  }

  override val allowedRoles: Set<String> = setOf("ROLE_CONTACTS_ADMIN", "ROLE_CONTACTS__RW", "ROLE_CONTACTS__R")

  override fun baseRequestBuilder(): WebTestClient.RequestHeadersSpec<*> = webTestClient.get()
    .uri("/prisoner/A1234BC/domestic-status")

  @Test
  fun `should return 404 when prisoner domestic status does not exist`() {
    webTestClient.get()
      .uri("/prisoner/A1234EE/domestic-status")
      .headers(setAuthorisationUsingCurrentUser())
      .exchange()
      .expectStatus()
      .isNotFound
  }

  @ParameterizedTest
  @ValueSource(strings = ["ROLE_CONTACTS_ADMIN", "ROLE_CONTACTS__R", "ROLE_CONTACTS__RW"])
  fun `should return domestic status when user has roles`(role: String) {
    setCurrentUser(StubUser.READ_WRITE_USER.copy(roles = listOf(role)))
    doWithTemporaryWritePermission { initialiseData() }
    val expectedResponse = PrisonerDomesticStatusResponse(
      id = 1L,
      domesticStatusCode = "M",
      domesticStatusDescription = "Married or in a civil partnership",
      active = true,
      createdBy = "read_write_user",
    )

    val response = webTestClient.get()
      .uri("/prisoner/A1234BC/domestic-status")
      .headers(setAuthorisationUsingCurrentUser())
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

  private fun initialiseData() {
    stubPrisonerSearch(
      prisoner(
        prisonerNumber = "A1234BC",
        prisonId = "MDI",
        firstName = "Joe",
        middleNames = "Middle",
        lastName = "Bloggs",
      ),
    )
    val request = CreateOrUpdatePrisonerDomesticStatusRequest(
      domesticStatusCode = "M",
    )

    val response = webTestClient.put()
      .uri("/prisoner/$prisonerNumber/domestic-status")
      .headers(setAuthorisationUsingCurrentUser())
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
}
