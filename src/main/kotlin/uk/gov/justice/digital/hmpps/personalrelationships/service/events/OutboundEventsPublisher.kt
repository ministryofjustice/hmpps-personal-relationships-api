package uk.gov.justice.digital.hmpps.personalrelationships.service.events

interface OutboundEventsPublisher {
  fun send(event: OutboundHMPPSDomainEvent)
}
