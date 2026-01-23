package uk.gov.justice.digital.hmpps.personalrelationships.integration.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.personalrelationships.helpers.prisoner
import uk.gov.justice.digital.hmpps.personalrelationships.integration.SecureAPIIntegrationTestBase
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.CreateOrUpdatePrisonerNumberOfChildrenRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.PrisonerNumberOfChildrenResponse
import uk.gov.justice.digital.hmpps.personalrelationships.util.StubUser

class GetPrisonerNumberOfChildrenIntegrationTest : SecureAPIIntegrationTestBase() {

  private val prisonerNumber = "A1234BC"
  private var numberOfChildrenId = 0L

  override val allowedRoles: Set<String> = setOf("ROLE_CONTACTS_ADMIN", "ROLE_CONTACTS__RW", "ROLE_CONTACTS__R")

  @BeforeEach
  fun setUp() {
    setCurrentUser(StubUser.READ_ONLY_USER)
  }

  override fun baseRequestBuilder(): WebTestClient.RequestHeadersSpec<*> = webTestClient.get()
    .uri("/prisoner/A1234BC/number-of-children")

  @Test
  fun `should return 404 when prisoner number of children does not exist`() {
    webTestClient.get()
      .uri("/prisoner/A1234EE/number-of-children")
      .headers(setAuthorisationUsingCurrentUser())
      .exchange()
      .expectStatus()
      .isNotFound
  }

  @Test
  fun `should return number of children`() {
    initialiseData()
    val expectedResponse = PrisonerNumberOfChildrenResponse(
      id = 1L,
      numberOfChildren = "1",
      active = true,
      createdBy = "read_write_user",
    )

    val response = webTestClient.get()
      .uri("/prisoner/A1234BC/number-of-children")
      .headers(setAuthorisationUsingCurrentUser())
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON_VALUE)
      .expectBody(PrisonerNumberOfChildrenResponse::class.java)
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
    val request = CreateOrUpdatePrisonerNumberOfChildrenRequest(
      numberOfChildren = 1,
    )

    val response = doWithTemporaryWritePermission {
      webTestClient.put()
        .uri("/prisoner/$prisonerNumber/number-of-children")
        .headers(setAuthorisationUsingCurrentUser())
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(request)
        .exchange()
        .expectStatus().isOk
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBody(PrisonerNumberOfChildrenResponse::class.java)
        .returnResult().responseBody
    }
    assertThat(response).isNotNull
    numberOfChildrenId = response!!.id
  }
}
