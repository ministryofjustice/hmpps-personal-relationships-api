package uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.resource.migrate

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.PostgresIntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.migrate.MigratePrisonerNumberOfChildrenRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.migrate.NumberOfChildrenDetailsRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync.SyncUpdatePrisonerNumberOfChildrenRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.migrate.PrisonerNumberOfChildrenMigrationResponse
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.sync.SyncPrisonerNumberOfChildrenResponse
import uk.gov.justice.digital.hmpps.hmppscontactsapi.util.StubUser
import java.time.LocalDateTime

class MigratePrisonerNumberOfChildrenIntegrationTest : PostgresIntegrationTestBase() {

  @BeforeEach
  fun setUp() {
    setCurrentUser(StubUser.SYNC_AND_MIGRATE_USER)
  }

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
    setCurrentUser(StubUser.SYNC_AND_MIGRATE_USER.copy(roles = listOf(authRole)))
    webTestClient.post()
      .uri("/migrate/number-of-children")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(basicMigrationRequest())
      .headers(setAuthorisationUsingCurrentUser())
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
      .headers(setAuthorisationUsingCurrentUser())
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
      .headers(setAuthorisationUsingCurrentUser())
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(numberOfChildrenToMigrate)
      .exchange()
      .expectStatus()
      .isBadRequest
      .expectBody()
      .jsonPath("$.userMessage")
      .isEqualTo("Validation failure(s): current.numberOfChildren must be less than or equal to 50 characters")
  }

  @Test
  fun `should overwrite existing migration`() {
    // Given
    val prisonerNumber = "A1234BC"
    val numberOfChildrenToMigrate = basicMigrationRequest()
    val numberOfChildrenToSync = SyncUpdatePrisonerNumberOfChildrenRequest(
      numberOfChildren = "1",
      createdBy = "user",
      createdTime = LocalDateTime.now(),
    )
    // When
    webTestClient.put()
      .uri("/sync/$prisonerNumber/number-of-children")
      .headers(setAuthorisationUsingCurrentUser())
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(numberOfChildrenToSync)
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(SyncPrisonerNumberOfChildrenResponse::class.java)
      .returnResult().responseBody

    val response = webTestClient.post()
      .uri("/migrate/number-of-children")
      .headers(setAuthorisationUsingCurrentUser())
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

    val syncResponse = webTestClient.get()
      .uri("/sync/$prisonerNumber/number-of-children")
      .headers(setAuthorisationUsingCurrentUser())
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(SyncPrisonerNumberOfChildrenResponse::class.java)
      .returnResult().responseBody

    with(syncResponse!!) {
      assertThat(numberOfChildren).isEqualTo(numberOfChildrenToMigrate.current?.numberOfChildren)
      assertThat(createdBy).isEqualTo(numberOfChildrenToMigrate.current?.createdBy)
      assertThat(createdTime).isNotNull
    }
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
