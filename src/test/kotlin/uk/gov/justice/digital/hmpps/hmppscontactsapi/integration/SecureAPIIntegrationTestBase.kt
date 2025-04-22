package uk.gov.justice.digital.hmpps.hmppscontactsapi.integration

import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppscontactsapi.util.StubUser

abstract class SecureAPIIntegrationTestBase : PostgresIntegrationTestBase() {

  private val allPossibleRoles = setOf(
    "ROLE_CONTACTS_ADMIN",
    "ROLE_CONTACTS__R",
    "ROLE_CONTACTS__RW",
    "PERSONAL_RELATIONSHIPS_MIGRATION",
    "ROLE_WRONG",
  )

  @Test
  fun `should return unauthorized if no token`() {
    setCurrentUser(null)
    baseRequestBuilder()
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `should return forbidden if no role`() {
    setCurrentUser(StubUser.USER_WITH_NO_ROLES)
    baseRequestBuilder()
      .headers(setAuthorisationUsingCurrentUser())
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @TestFactory
  fun `should return forbidden for roles without access`(): Iterable<DynamicTest> {
    val forbiddenRoles = allPossibleRoles - allowedRoles
    return forbiddenRoles.map { forbiddenRole ->
      DynamicTest.dynamicTest("Requests with role ($forbiddenRole) should be forbidden") {
        setCurrentUser(StubUser("SECURE_TEST", "Secure test", listOf(forbiddenRole)))
        baseRequestBuilder()
          .headers(setAuthorisationUsingCurrentUser())
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }
  }

  abstract fun baseRequestBuilder(): WebTestClient.RequestHeadersSpec<*>
  abstract val allowedRoles: Set<String>
}
