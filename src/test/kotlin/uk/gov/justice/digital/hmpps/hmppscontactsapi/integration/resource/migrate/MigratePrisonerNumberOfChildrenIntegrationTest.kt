package uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.resource.migrate

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.PostgresIntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.migrate.MigratePrisonerNumberOfChildrenRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.migrate.NumberOfChildrenDetailsRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.migrate.PrisonerNumberOfChildrenMigrationResponse
import java.time.LocalDateTime

class MigratePrisonerNumberOfChildrenIntegrationTest : PostgresIntegrationTestBase() {

  @Test
  fun `should return unauthorized if no token provided`() {
    webTestClient.post()
      .uri("/migrate/number-of-children")
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
      .uri("/migrate/number-of-children")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(basicMigrationRequest())
      .headers(setAuthorisation(roles = listOf(authRole)))
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `should migrate number of children successfully`() {
    // Given
    val numberOfChildrenToMigrate = basicMigrationRequest()
    // When
    val response = webTestClient.post()
      .uri("/migrate/number-of-children")
      .headers(setAuthorisation(roles = listOf("PERSONAL_RELATIONSHIPS_MIGRATION")))
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(numberOfChildrenToMigrate)
      .exchange()
      .expectStatus()
      .isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(PrisonerNumberOfChildrenMigrationResponse::class.java)
      .returnResult().responseBody!!

    // Then
    with(response) {
      assertThat(prisonerNumber).isEqualTo("A1234BC")
      assertThat(current).isGreaterThan(0)
      assertThat(history[0]).isGreaterThan(0)
    }
  }

  @Test
  fun `should not migrate number of children when invalid data`() {
    // Given
    val numberOfChildrenToMigrate = MigratePrisonerNumberOfChildrenRequest(
      prisonerNumber = "A1234BC",
      current = NumberOfChildrenDetailsRequest(
        numberOfChildren = "".padStart(55, 'X'),
        createdBy = "Admin",
        createdTime = LocalDateTime.now(),
      ),
      history = listOf(
        NumberOfChildrenDetailsRequest(
          numberOfChildren = "1",
          createdBy = "Admin",
          createdTime = LocalDateTime.now(),
        ),
      ),

    )

    // When
    webTestClient.post()
      .uri("/migrate/number-of-children")
      .headers(setAuthorisation(roles = listOf("PERSONAL_RELATIONSHIPS_MIGRATION")))
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(numberOfChildrenToMigrate)
      .exchange()
      .expectStatus()
      .isBadRequest
      .expectBody()
      .jsonPath("$.userMessage")
      .isEqualTo("Validation failure(s): current.numberOfChildren must be less than or equal to 50 characters")
  }

  private fun basicMigrationRequest() = MigratePrisonerNumberOfChildrenRequest(
    prisonerNumber = "A1234BC",
    current = NumberOfChildrenDetailsRequest(
      numberOfChildren = "1",
      createdBy = "Admin",
      createdTime = LocalDateTime.now(),
    ),
    history = listOf(
      NumberOfChildrenDetailsRequest(
        numberOfChildren = "1",
        createdBy = "Admin",
        createdTime = LocalDateTime.now(),
      ),
    ),

  )
}
