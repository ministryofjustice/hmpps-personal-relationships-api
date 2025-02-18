package uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.resource


import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.SecureAPIIntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.UpdatePrisonerDomesticStatusRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.PrisonerDomesticStatusResponse

class CreateOrUpdatePrisonerDomesticStatusIntegrationTest : SecureAPIIntegrationTestBase() {

    private val prisonerNumber = "A1234BC"
    override val allowedRoles: Set<String> = setOf("ROLE_CONTACTS_ADMIN", "ROLE_CONTACTS__RW")

    override fun baseRequestBuilder(): WebTestClient.RequestHeadersSpec<*> = webTestClient.put()
        .uri("/prisoner/$prisonerNumber/domestic-status")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(createRequest())

    @Test
    fun `should create new domestic status when user has ROLE_CONTACTS__RW role`() {
        val request = createRequest()
        val expectedResponse = PrisonerDomesticStatusResponse(
            id = 2L,
            prisonerNumber = prisonerNumber,
            domesticStatusValue = "M",
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
        assertThat(response).usingRecursiveComparison()
            .ignoringFields("id", "createdTime")
            .isEqualTo(expectedResponse)
    }

    @Test
    fun `should update existing domestic status when user has ROLE_CONTACTS_ADMIN role`() {
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

        val expectedResponse = PrisonerDomesticStatusResponse(
            id = 2L,
            prisonerNumber = prisonerNumber,
            domesticStatusValue = "M",
            domesticStatusDescription = "Married or in civil partnership",
            active = true,
            createdBy = "test-user",
        )

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
            .isEqualTo(expectedResponse)
        //TODO: get by given id and validate if they have active and inactive two records
    }

    @Test
    fun `should return 400 when domestic status code is more than 1 character`() {
        webTestClient.put()
            .uri("/prisoner/$prisonerNumber/domestic-status")
            .headers(setAuthorisation(roles = listOf("ROLE_CONTACTS__RW")))
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(
                UpdatePrisonerDomesticStatusRequest(
                    domesticStatusCode = "AB",
                    prisonerNumber = prisonerNumber,
                    updatedBy = "test-user"
                )
            )
            .exchange()
            .expectStatus().isBadRequest
            .expectBody()
            .jsonPath("$.userMessage")
            .isEqualTo("Validation failure(s): domesticStatusCode must be exactly 1 character")
    }

    @Test
    fun `should return 400 when domestic status code is empty`() {
        webTestClient.put()
            .uri("/prisoner/$prisonerNumber/domestic-status")
            .headers(setAuthorisation(roles = listOf("ROLE_CONTACTS__RW")))
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(
                UpdatePrisonerDomesticStatusRequest(
                    domesticStatusCode = "",
                    prisonerNumber = prisonerNumber,
                    updatedBy = "test-user"
                )
            )
            .exchange()
            .expectStatus().isBadRequest
            .expectBody()
            .jsonPath("$.userMessage")
            .isEqualTo("Validation failure(s): domesticStatusCode must be exactly 1 character")
    }

    @Test
    fun `should return 404 when domestic status code is invalid`() {
        webTestClient.put()
            .uri("/prisoner/$prisonerNumber/domestic-status")
            .headers(setAuthorisation(roles = listOf("ROLE_CONTACTS__RW")))
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(
                UpdatePrisonerDomesticStatusRequest(
                    domesticStatusCode = "Q",
                    prisonerNumber = prisonerNumber,
                    updatedBy = "test-user"
                )
            )
            .exchange()
            .expectStatus().isNotFound
            .expectBody()
            .jsonPath("$.userMessage")
            .isEqualTo("Entity not found : No reference data found for groupCode: DOMESTIC_STS and code: Q")
    }

    private fun createRequest() = UpdatePrisonerDomesticStatusRequest(
        domesticStatusCode = "M",
        prisonerNumber = prisonerNumber,
        updatedBy = "test-user"
    )
}

