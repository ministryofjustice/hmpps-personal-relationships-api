package uk.gov.justice.digital.hmpps.personalrelationships.integration

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import uk.gov.justice.digital.hmpps.personalrelationships.integration.container.PostgresContainer

@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test", "local-postgres")
abstract class PostgresIntegrationTestBase : IntegrationTestBase() {

  companion object {
    private val pgContainer = PostgresContainer.instance

    @JvmStatic
    @DynamicPropertySource
    fun properties(registry: DynamicPropertyRegistry) {
      pgContainer?.run {
        registry.add("spring.datasource.url") { PostgresContainer.jdbcUrl }
        registry.add("spring.datasource.username") { PostgresContainer.dbUsername }
        registry.add("spring.datasource.password") { PostgresContainer.dbPassword }
      }
    }
  }
}
