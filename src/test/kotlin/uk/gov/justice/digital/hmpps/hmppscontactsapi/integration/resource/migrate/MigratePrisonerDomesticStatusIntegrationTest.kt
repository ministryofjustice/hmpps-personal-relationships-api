package uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.resource.migrate

import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.PostgresIntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.migrate.DomesticStatusDetailsRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.migrate.MigratePrisonerDomesticStatusRequest
import java.time.LocalDateTime

class MigratePrisonerDomesticStatusIntegrationTest : PostgresIntegrationTestBase() {

  @Test
  fun `migrate domestic status - success`() {
    // Given
    val domesticStatusToMigrate = MigratePrisonerDomesticStatusRequest(
      prisonerNumber = "A1234BC",
      current = DomesticStatusDetailsRequest(
        domesticStatusCode = "M",
        createdBy = "Admin",
        createdTime = LocalDateTime.now(),
      ),
      history = listOf(
        DomesticStatusDetailsRequest(
          domesticStatusCode = "D",
          createdBy = "Admin",
          createdTime = LocalDateTime.now(),
        ),
      ),

    )
    // When
    val response = webTestClient.post()
      .uri("/migrate/domestic-status")
      .headers(setAuthorisation(roles = listOf("PERSONAL_RELATIONSHIPS_MIGRATION")))
      .contentType(MediaType.APPLICATION_JSON)
      .body(BodyInserters.fromValue(domesticStatusToMigrate))
      .exchange()

    // Then
    response.expectBody()
      .jsonPath("$.prisonerNumber").isEqualTo("A1234BC")
      .jsonPath("$.current.domesticStatusCode").isEqualTo("M")
      .jsonPath("$.current.createdBy").isEqualTo("Admin")
      .jsonPath("$.history[0].domesticStatusCode").isEqualTo("D")
      .jsonPath("$.history[0].createdBy").isEqualTo("Admin")
  }

  @Test
  fun `migrate domestic status - unauthorized without role`() {
    // Given
    val domesticStatusToMigrate = MigratePrisonerDomesticStatusRequest(
      prisonerNumber = "A1234BC",
      current = DomesticStatusDetailsRequest(
        domesticStatusCode = "M",
        createdBy = "Admin",
        createdTime = LocalDateTime.now(),
      ),
      history = listOf(
        DomesticStatusDetailsRequest(
          domesticStatusCode = "D",
          createdBy = "Admin",
          createdTime = LocalDateTime.now(),
        ),
      ),

    )

    // When
    val response = webTestClient.post()
      .uri("/migrate/domestic-status")
      .headers(setAuthorisation(roles = listOf()))
      .contentType(MediaType.APPLICATION_JSON)
      .body(BodyInserters.fromValue(domesticStatusToMigrate))
      .exchange()

    // Then
    response.expectStatus().isForbidden
  }

  @Test
  fun `migrate domestic status - bad request when invalid data`() {
    // Given
    val invalidDomesticStatus = MigratePrisonerDomesticStatusRequest(
      prisonerNumber = "A1234BC",
      current = DomesticStatusDetailsRequest(
        domesticStatusCode = "INVALID",
        createdBy = "Admin",
        createdTime = LocalDateTime.now(),
      ),
      history = listOf(
        DomesticStatusDetailsRequest(
          domesticStatusCode = "D",
          createdBy = "Admin",
          createdTime = LocalDateTime.now(),
        ),
      ),

    )

    // When
    webTestClient.post()
      .uri("/migrate/domestic-status")
      .headers(setAuthorisation(roles = listOf("PERSONAL_RELATIONSHIPS_MIGRATION")))
      .contentType(MediaType.APPLICATION_JSON)
      .body(BodyInserters.fromValue(invalidDomesticStatus))
      .exchange()
      .expectStatus()
      .isBadRequest
      .expectBody()
      .jsonPath("$.userMessage")
      .isEqualTo("Validation failure(s): domesticStatusCode must be exactly 1 character")
  }
}
