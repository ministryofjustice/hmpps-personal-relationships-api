package uk.gov.justice.digital.hmpps.personalrelationships.integration

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary

@TestConfiguration
class TestConfiguration {

  @Primary
  @Bean
  fun stubOutboundEventsPublisher(): StubOutboundEventsPublisher = StubOutboundEventsPublisher()
}
