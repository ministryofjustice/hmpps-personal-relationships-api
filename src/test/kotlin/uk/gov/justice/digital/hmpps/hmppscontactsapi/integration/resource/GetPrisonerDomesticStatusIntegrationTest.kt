package uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.SecureAPIIntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.UpdatePrisonerDomesticStatusRequest
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

    @Test
    fun `should return domestic status when user has ROLE_CONTACTS__R role`() {
        val expectedResponse = PrisonerDomesticStatusResponse(
            id = 1L,
            prisonerNumber = "A1234BC",
            domesticStatusValue = "SINGLE",
            active = true
        )

        val response = webTestClient.get()
            .uri("/prisoner/A1234BC/domestic-status")
            .headers(setAuthorisation(roles = listOf("ROLE_CONTACTS__R")))
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentType(MediaType.APPLICATION_JSON_VALUE)
            .expectBody(PrisonerDomesticStatusResponse::class.java)
            .returnResult().responseBody

        assertThat(response).isNotNull
        assertThat(response).usingRecursiveComparison().isEqualTo(expectedResponse)
    }

    @Test
    fun `should return domestic status when user has ROLE_CONTACTS__RW role`() {
        val expectedResponse = PrisonerDomesticStatusResponse(
            id = 1L,
            prisonerNumber = "A1234BC",
            domesticStatusValue = "SINGLE",
            active = true
        )

        val response = webTestClient.get()
            .uri("/prisoner/A1234BC/domestic-status")
            .headers(setAuthorisation(roles = listOf("ROLE_CONTACTS__RW")))
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentType(MediaType.APPLICATION_JSON_VALUE)
            .expectBody(PrisonerDomesticStatusResponse::class.java)
            .returnResult().responseBody

        assertThat(response).isNotNull
        assertThat(response).usingRecursiveComparison().isEqualTo(expectedResponse)
    }

    @Test
    fun `should return domestic status when user has ROLE_CONTACTS_ADMIN role`() {
        val expectedResponse = PrisonerDomesticStatusResponse(
            id = 1L,
            prisonerNumber = "A1234BC",
            domesticStatusValue = "SINGLE",
            active = true
        )

        val response = webTestClient.get()
            .uri("/prisoner/A1234BC/domestic-status")
            .headers(setAuthorisation(roles = listOf("ROLE_CONTACTS_ADMIN")))
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentType(MediaType.APPLICATION_JSON_VALUE)
            .expectBody(PrisonerDomesticStatusResponse::class.java)
            .returnResult().responseBody

        assertThat(response).isNotNull
        assertThat(response).usingRecursiveComparison().isEqualTo(expectedResponse)
    }

    private fun createRequest() = UpdatePrisonerDomesticStatusRequest(
        domesticStatusCode = "M",
        prisonerNumber = prisonerNumber,
        updatedBy = "test-user"
    )
}
