package uk.gov.justice.digital.hmpps.hmppscontactsapi.service.telemetry

import com.microsoft.applicationinsights.TelemetryClient
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class TelemetryServiceTest {

  private val telemetryClient: TelemetryClient = mock()
  private val telemetryService = TelemetryService(telemetryClient)

  @Test
  fun `should raise a standard telemetry event`() {
    val standardEvent = object : StandardTelemetryEvent("FAKE-standard-event") {
      override fun properties() = mapOf("standard key" to "standard value")
    }

    telemetryService.track(standardEvent)

    verify(telemetryClient).trackEvent("FAKE-standard-event", mapOf("standard key" to "standard value"), null)
  }

  @Test
  fun `should raise a metric telemetry event`() {
    val standardEvent = object : MetricTelemetryEvent("FAKE-metric-event") {
      override fun properties() = mapOf("metric property key" to "metric property value")
      override fun metrics() = mapOf("metric metric key" to 1.0)
    }

    telemetryService.track(standardEvent)

    verify(telemetryClient).trackEvent("FAKE-metric-event", mapOf("metric property key" to "metric property value"), mapOf("metric metric key" to 1.0))
  }
}
