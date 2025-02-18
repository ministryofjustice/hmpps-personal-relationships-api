package uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.resource.migrate

import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
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
        domesticStatusCode = "MARRIED",
        createdBy = "XXXXX",
        createdTime = LocalDateTime.now(),
      ),
      history = listOf(
        DomesticStatusDetailsRequest(
          domesticStatusCode = "DIVORCED",
          createdBy = "XXXXX",
          createdTime = LocalDateTime.now(),
        ),
      ),

    )
    // When
    val response = webTestClient.post()
      .uri("/migrate/domestic-status")
      .headers(setAuthorisation(roles = listOf("ROLE_MIGRATE_CONTACTS")))
      .contentType(MediaType.APPLICATION_JSON)
      .body(BodyInserters.fromValue(domesticStatusToMigrate))
      .exchange()

    // Then
    response.expectStatus().isCreated
      .expectBody()
      .jsonPath("$.id").isEqualTo(123456)
      .jsonPath("$.personId").isEqualTo("A1234BC")
      .jsonPath("$.status").isEqualTo("MARRIED")
  }

  @Test
  fun `migrate domestic status - unauthorized without role`() {
    // Given
    val domesticStatusToMigrate = MigratePrisonerDomesticStatusRequest(
      prisonerNumber = "A1234BC",
      current = DomesticStatusDetailsRequest(
        domesticStatusCode = "MARRIED",
        createdBy = "XXXXX",
        createdTime = LocalDateTime.now(),
      ),
      history = listOf(
        DomesticStatusDetailsRequest(
          domesticStatusCode = "DIVORCED",
          createdBy = "XXXXX",
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
    response.expectStatus().isUnauthorized
  }

  @Test
  fun `migrate domestic status - bad request when invalid data`() {
    // Given
    val invalidDomesticStatus = MigratePrisonerDomesticStatusRequest(
      prisonerNumber = "A1234BC",
      current = DomesticStatusDetailsRequest(
        domesticStatusCode = "MARRIED",
        createdBy = "XXXXX",
        createdTime = LocalDateTime.now(),
      ),
      history = listOf(
        DomesticStatusDetailsRequest(
          domesticStatusCode = "DIVORCED",
          createdBy = "XXXXX",
          createdTime = LocalDateTime.now(),
        ),
      ),

    )

    // When
    val response = webTestClient.post()
      .uri("/migrate/domestic-status")
      .headers(setAuthorisation(roles = listOf("ROLE_MIGRATE_CONTACTS")))
      .contentType(MediaType.APPLICATION_JSON)
      .body(BodyInserters.fromValue(invalidDomesticStatus))
      .exchange()

    // Then
    response.expectStatus().isBadRequest
  }

  @Test
  fun `migrate domestic status - conflict when already exists`() {
    // Given
    val existingDomesticStatus = MigratePrisonerDomesticStatusRequest(
      prisonerNumber = "A1234BC",
      current = DomesticStatusDetailsRequest(
        domesticStatusCode = "MARRIED",
        createdBy = "XXXXX",
        createdTime = LocalDateTime.now(),
      ),
      history = listOf(
        DomesticStatusDetailsRequest(
          domesticStatusCode = "DIVORCED",
          createdBy = "XXXXX",
          createdTime = LocalDateTime.now(),
        ),
      ),

    )

    // First request to create
    webTestClient.post()
      .uri("/migrate/domestic-status")
      .headers(setAuthorisation(roles = listOf("ROLE_MIGRATE_CONTACTS")))
      .contentType(MediaType.APPLICATION_JSON)
      .body(BodyInserters.fromValue(existingDomesticStatus))
      .exchange()

    // When - Second request with same ID
    val response = webTestClient.post()
      .uri("/migrate/domestic-status")
      .headers(setAuthorisation(roles = listOf("ROLE_MIGRATE_CONTACTS")))
      .contentType(MediaType.APPLICATION_JSON)
      .body(BodyInserters.fromValue(existingDomesticStatus))
      .exchange()

    // Then
    response.expectStatus().isEqualTo(HttpStatus.CONFLICT)
  }
}
