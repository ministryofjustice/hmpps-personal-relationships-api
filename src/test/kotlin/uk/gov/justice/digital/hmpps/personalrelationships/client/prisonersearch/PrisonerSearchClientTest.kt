package uk.gov.justice.digital.hmpps.personalrelationships.client.prisonersearch

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.personalrelationships.helpers.prisoner
import uk.gov.justice.digital.hmpps.personalrelationships.integration.helper.isEqualTo
import uk.gov.justice.digital.hmpps.personalrelationships.integration.wiremock.PrisonerSearchApiMockServer

class PrisonerSearchClientTest {

  private val server = PrisonerSearchApiMockServer().also { it.start() }
  private val client = PrisonerSearchClient(WebClient.create("http://localhost:${server.port()}"))

  @Test
  fun `should get matching prisoner`() {
    server.stubGetPrisoner("123456", "RSI")

    client.getPrisoner("123456") isEqualTo prisoner("123456", "RSI")
  }

  @AfterEach
  fun after() {
    server.stop()
  }
}
