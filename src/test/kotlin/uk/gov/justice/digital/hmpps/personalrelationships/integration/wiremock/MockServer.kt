package uk.gov.justice.digital.hmpps.personalrelationships.integration.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.kotlinModule

/**
 * It is important you match the servers port number with that of the port number in the application-test.yml file.
 */
abstract class MockServer(port: Int, private val urlPrefix: String = "") : WireMockServer(port) {

  protected val mapper: ObjectMapper = JsonMapper.builder()
    .addModule(kotlinModule())
    .build()

  fun stubHealthPing(status: Int) {
    stubFor(
      WireMock.get("$urlPrefix/health/ping").willReturn(
        WireMock.aResponse()
          .withHeader("Content-Type", "application/json")
          .withBody(if (status == 200) """{"status":"UP"}""" else """{"status":"DOWN"}""")
          .withStatus(status),
      ),
    )
  }
}
