package uk.gov.justice.digital.hmpps.hmppscontactsapi.config

import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.sdk.testing.junit5.OpenTelemetryExtension
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.PostgresIntegrationTestBase

class ClientTrackingConfigurationTest : PostgresIntegrationTestBase() {
  @Autowired
  private lateinit var clientTrackingInterceptor: ClientTrackingInterceptor

  private val tracer: Tracer = otelTesting.openTelemetry.getTracer("test")

  @Test
  fun `should add client and username to telemetry`() {
    val token = jwtAuthHelper.createJwtAccessToken(clientId = "hmpps-contacts-ui-client", username = "bob")
    val req = MockHttpServletRequest()
    req.addHeader(HttpHeaders.AUTHORIZATION, "Bearer $token")
    val res = MockHttpServletResponse()
    tracer.spanBuilder("span").startSpan().run {
      makeCurrent().use { clientTrackingInterceptor.preHandle(req, res, "null") }
      end()
    }
    otelTesting.assertTraces().hasTracesSatisfyingExactly({ t ->
      t.hasSpansSatisfyingExactly({
        it.hasAttribute(AttributeKey.stringKey("username"), "bob")
        it.hasAttribute(AttributeKey.stringKey("clientId"), "hmpps-contacts-ui-client")
      })
    })
  }

  @Test
  fun `should add only clientId if username null to insight telemetry`() {
    val token = jwtAuthHelper.createJwtAccessToken(clientId = "hmpps-contacts-ui-client", username = null)
    val req = MockHttpServletRequest()
    req.addHeader(HttpHeaders.AUTHORIZATION, "Bearer $token")
    val res = MockHttpServletResponse()
    tracer.spanBuilder("span").startSpan().run {
      makeCurrent().use { clientTrackingInterceptor.preHandle(req, res, "null") }
      end()
    }
    otelTesting.assertTraces().hasTracesSatisfyingExactly({ t ->
      t.hasSpansSatisfyingExactly({
        it.hasAttribute(AttributeKey.stringKey("clientId"), "hmpps-contacts-ui-client")
      })
    })
  }

  private companion object {
    @JvmStatic
    @RegisterExtension
    private val otelTesting: OpenTelemetryExtension = OpenTelemetryExtension.create()
  }
}
