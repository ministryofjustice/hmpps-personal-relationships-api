package uk.gov.justice.digital.hmpps.hmppscontactsapi.integration

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppscontactsapi.util.StubUser

class NotFoundTest : PostgresIntegrationTestBase() {

  @Test
  fun `Resources that aren't found should return 404 - test of the exception handler`() {
    setCurrentUser(StubUser.READ_ONLY_USER)
    webTestClient.get().uri("/some-url-not-found")
      .headers(setAuthorisationUsingCurrentUser())
      .exchange()
      .expectStatus().isNotFound
  }
}
