package uk.gov.justice.digital.hmpps.personalrelationships.integration.wiremock

import com.github.tomakehurst.wiremock.client.WireMock
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import uk.gov.justice.digital.hmpps.personalrelationships.client.organisationsapi.model.OrganisationSummary

class OrganisationsApiMockServer : MockServer(8094) {

  fun stubOrganisationSummary(organisation: OrganisationSummary) {
    stubFor(
      WireMock.get("/organisation/${organisation.organisationId}/summary")
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(
              mapper.writeValueAsString(organisation),
            )
            .withStatus(200),
        ),
    )
  }

  fun stubOrganisationSummaryNotFound(organisationId: Long) {
    stubFor(
      WireMock.get("/organisation/$organisationId/summary")
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(404),
        ),
    )
  }
}

class OrganisationsApiExtension :
  BeforeAllCallback,
  AfterAllCallback,
  BeforeEachCallback {
  companion object {
    @JvmField
    val organisationsApiMockServer = OrganisationsApiMockServer()
  }

  override fun beforeAll(context: ExtensionContext) {
    organisationsApiMockServer.start()
  }

  override fun beforeEach(context: ExtensionContext) {
    organisationsApiMockServer.resetAll()
  }

  override fun afterAll(context: ExtensionContext) {
    organisationsApiMockServer.stop()
  }
}
