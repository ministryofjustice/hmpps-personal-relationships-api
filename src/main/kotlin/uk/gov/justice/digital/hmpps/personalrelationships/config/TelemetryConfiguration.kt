package uk.gov.justice.digital.hmpps.personalrelationships.config

import com.microsoft.applicationinsights.TelemetryClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class TelemetryConfiguration {
  @Bean
  fun telemetryClient() = TelemetryClient()
}
