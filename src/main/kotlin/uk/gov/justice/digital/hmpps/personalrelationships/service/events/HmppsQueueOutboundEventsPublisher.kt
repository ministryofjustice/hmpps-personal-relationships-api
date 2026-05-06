package uk.gov.justice.digital.hmpps.personalrelationships.service.events

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import software.amazon.awssdk.services.sns.model.MessageAttributeValue
import tools.jackson.databind.ObjectMapper
import uk.gov.justice.digital.hmpps.personalrelationships.config.Feature
import uk.gov.justice.digital.hmpps.personalrelationships.config.FeatureSwitches
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.publish
import java.util.function.Supplier

class HmppsQueueOutboundEventsPublisher(
  private val hmppsQueueService: HmppsQueueService,
  private val mapper: ObjectMapper,
  features: FeatureSwitches,
) : OutboundEventsPublisher {
  private val outboundEventsEnabled = features.isEnabled(Feature.OUTBOUND_EVENTS_ENABLED)

  companion object {
    const val TOPIC_ID = "domainevents"
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  init {
    log.info("Outbound SNS event publishing enabled = $outboundEventsEnabled")
  }

  private val domainEventsTopic by lazy {
    hmppsQueueService.findByTopicId(TOPIC_ID) ?: throw RuntimeException("Topic with name $TOPIC_ID doesn't exist")
  }

  override fun send(event: OutboundHMPPSDomainEvent) {
    if (outboundEventsEnabled) {
      try {
        domainEventsTopic.publish(
          eventType = event.eventType,
          event = mapper.writeValueAsString(event),
          attributes = metaData(event),
        )
      } catch (e: Throwable) {
        val message = "Failed (publishToDomainEventsTopic) to publish Event $event.eventType to $TOPIC_ID"
        log.error(message, e)
        throw PublishEventException(message, e)
      }
    }
    log.info("Ignoring publishing of event $event (feature switched off)")
  }

  private fun metaData(payload: OutboundHMPPSDomainEvent) = mapOf("eventType" to MessageAttributeValue.builder().dataType("String").stringValue(payload.eventType).build())
}

class PublishEventException(message: String? = null, cause: Throwable? = null) :
  RuntimeException(message, cause),
  Supplier<PublishEventException> {
  override fun get(): PublishEventException = PublishEventException(message, cause)
}
