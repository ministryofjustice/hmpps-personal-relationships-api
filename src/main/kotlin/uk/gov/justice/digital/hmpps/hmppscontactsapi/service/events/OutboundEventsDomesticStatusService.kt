package uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppscontactsapi.config.FeatureSwitches
import java.time.LocalDateTime

@Service
class OutboundEventsDomesticStatusService(
  private val publisher: OutboundEventsPublisher,
  private val featureSwitches: FeatureSwitches,
) {
  companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun send(
    prisonerNumber: String,
    additionalInformation: AdditionalInformation,
  ) {
    if (featureSwitches.isEnabled(OutboundEvent.PRISONER_DOMESTIC_STATUS_CREATED)) {
      log.info("Sending domestic status event for prisonerNumber $prisonerNumber with status $additionalInformation")

      sendSafely(
        OutboundEvent.PRISONER_DOMESTIC_STATUS_CREATED,
        additionalInformation,
      )
    } else {
      log.warn("Domestic status event feature is configured off")
    }
  }

  private fun sendSafely(
    outboundEvent: OutboundEvent,
    additionalInformation: AdditionalInformation,
  ) {
    try {
      publisher.send(outboundEvent.event(additionalInformation))
    } catch (e: Exception) {
      log.error(
        "Unable to send event with type {}, info {}",
        outboundEvent,
        additionalInformation,
        e,
      )
    }
  }
}

data class DomesticStatusInfo(
  val id: Long,
  val status: String,
  val createdDateTime: LocalDateTime,
  override val source: Source = Source.DPS,
) : AdditionalInformation(source)
