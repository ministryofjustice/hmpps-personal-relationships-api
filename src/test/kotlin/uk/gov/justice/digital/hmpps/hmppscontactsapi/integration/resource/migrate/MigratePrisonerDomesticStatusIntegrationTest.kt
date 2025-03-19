package uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.resource.migrate

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.PostgresIntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.migrate.DomesticStatusDetailsRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.migrate.MigratePrisonerDomesticStatusRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync.SyncUpdatePrisonerDomesticStatusRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.migrate.PrisonerDomesticStatusMigrationResponse
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.sync.SyncPrisonerDomesticStatusResponse
import java.time.LocalDateTime

class MigratePrisonerDomesticStatusIntegrationTest : PostgresIntegrationTestBase() {

  @Test
  fun `should return unauthorized if no token provided`() {
    webTestClient.post()
      .uri("/migrate/domestic-status")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(basicMigrationRequest())
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @ParameterizedTest
  @ValueSource(strings = ["ROLE_CONTACTS_ADMIN", "ROLE_CONTACTS__R", "ROLE_CONTACTS__RW"])
  fun `should return forbidden without an authorised role on the token`(authRole: String) {
    webTestClient.post()
      .uri("/migrate/domestic-status")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(basicMigrationRequest())
      .headers(setAuthorisation(roles = listOf(authRole)))
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `should migrate domestic status successfully`() {
    // Given
    val domesticStatusToMigrate = basicMigrationRequest()
    // When
    val response = webTestClient.post()
      .uri("/migrate/domestic-status")
      .headers(setAuthorisation(roles = listOf("PERSONAL_RELATIONSHIPS_MIGRATION")))
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(domesticStatusToMigrate)
      .exchange()
      .expectStatus()
      .isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(PrisonerDomesticStatusMigrationResponse::class.java)
      .returnResult().responseBody!!

    // Then
    with(response) {
      assertThat(prisonerNumber).isEqualTo("A1234BC")
      assertThat(current).isGreaterThan(0)
      assertThat(history[0]).isGreaterThan(0)
    }
  }

  @Test
  fun `should overwrite existing migration`() {
    // Given
    val prisonerNumber = "A1234BC"
    val domesticStatusToMigrate = basicMigrationRequest()
    // When
    val domesticStatusToSync = SyncUpdatePrisonerDomesticStatusRequest(
      domesticStatusCode = "D",
      createdBy = "ADMIN",
      createdTime = LocalDateTime.now(),
    )

    // When
    webTestClient.put()
      .uri("/sync/$prisonerNumber/domestic-status")
      .headers(setAuthorisation(roles = listOf("PERSONAL_RELATIONSHIPS_MIGRATION")))
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(domesticStatusToSync)
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(SyncPrisonerDomesticStatusResponse::class.java)
      .returnResult().responseBody

    val response = webTestClient.post()
      .uri("/migrate/domestic-status")
      .headers(setAuthorisation(roles = listOf("PERSONAL_RELATIONSHIPS_MIGRATION")))
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(domesticStatusToMigrate)
      .exchange()
      .expectStatus()
      .isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(PrisonerDomesticStatusMigrationResponse::class.java)
      .returnResult().responseBody!!

    // Then
    with(response) {
      assertThat(this.prisonerNumber).isEqualTo(prisonerNumber)
      assertThat(this.current).isGreaterThan(0)
      assertThat(history[0]).isGreaterThan(0)
    }

    val domesticStatus = webTestClient.get()
      .uri("/sync/$prisonerNumber/domestic-status")
      .headers(setAuthorisation(roles = listOf("PERSONAL_RELATIONSHIPS_MIGRATION")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(SyncPrisonerDomesticStatusResponse::class.java)
      .returnResult().responseBody

    with(domesticStatus!!) {
      assertThat(prisonerNumber).isEqualTo(prisonerNumber)
      assertThat(domesticStatusCode).isEqualTo(domesticStatusToMigrate.current?.domesticStatusCode)
      assertThat(createdBy).isEqualTo(domesticStatusToMigrate.current?.createdBy)
      assertThat(createdTime).isNotNull
    }
  }

  @Test
  fun `should not migrate domestic status when invalid data`() {
    // Given
    val invalidDomesticStatus = MigratePrisonerDomesticStatusRequest(
      prisonerNumber = "A1234BC",
      current = DomesticStatusDetailsRequest(
        domesticStatusCode = "LONGER THAN 12 CHARACTERS",
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
      .bodyValue(invalidDomesticStatus)
      .exchange()
      .expectStatus()
      .isBadRequest
      .expectBody()
      .jsonPath("$.userMessage")
      .isEqualTo("Validation failure(s): current.domesticStatusCode must be less than or equal to 12 characters")
  }

  private fun basicMigrationRequest(): MigratePrisonerDomesticStatusRequest {
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
    return domesticStatusToMigrate
  }
}
