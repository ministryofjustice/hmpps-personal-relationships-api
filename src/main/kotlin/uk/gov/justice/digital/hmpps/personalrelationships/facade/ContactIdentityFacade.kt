package uk.gov.justice.digital.hmpps.personalrelationships.facade

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.personalrelationships.config.User
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.identity.CreateIdentityRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.identity.CreateMultipleIdentitiesRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.identity.UpdateIdentityRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.ContactIdentityDetails
import uk.gov.justice.digital.hmpps.personalrelationships.service.ContactIdentityService
import uk.gov.justice.digital.hmpps.personalrelationships.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.personalrelationships.service.events.OutboundEventsService
import uk.gov.justice.digital.hmpps.personalrelationships.service.events.Source
import uk.gov.justice.digital.hmpps.personalrelationships.service.telemetry.TelemetryContactCustomEventService

@Service
class ContactIdentityFacade(
  private val contactIdentityService: ContactIdentityService,
  private val outboundEventsService: OutboundEventsService,
  private val telemetryContactCustomEventService: TelemetryContactCustomEventService,
) {
  fun create(contactId: Long, request: CreateIdentityRequest, user: User): ContactIdentityDetails = contactIdentityService.create(contactId, request, user).also {
    outboundEventsService.send(
      outboundEvent = OutboundEvent.CONTACT_IDENTITY_CREATED,
      identifier = it.contactIdentityId,
      contactId = contactId,
      user = user,
    )
  }.also {
    telemetryContactCustomEventService.trackCreateContactIdentityEvent(it, source = Source.DPS, user = user)
  }

  fun createMultiple(contactId: Long, request: CreateMultipleIdentitiesRequest, user: User): List<ContactIdentityDetails> {
    val contactIdentityDetails = contactIdentityService.createMultiple(contactId, request, user).onEach {
      outboundEventsService.send(
        outboundEvent = OutboundEvent.CONTACT_IDENTITY_CREATED,
        identifier = it.contactIdentityId,
        contactId = contactId,
        user = user,
      )
    }

    contactIdentityDetails.forEach {
      telemetryContactCustomEventService.trackCreateContactIdentityEvent(it, source = Source.DPS, user = user)
    }
    return contactIdentityDetails
  }

  fun update(contactId: Long, contactIdentityId: Long, request: UpdateIdentityRequest, user: User): ContactIdentityDetails = contactIdentityService.update(contactId, contactIdentityId, request, user).also {
    outboundEventsService.send(
      outboundEvent = OutboundEvent.CONTACT_IDENTITY_UPDATED,
      identifier = contactIdentityId,
      contactId = contactId,
      user = user,
    )
  }.also {
    telemetryContactCustomEventService.trackUpdateContactIdentityEvent(it, source = Source.DPS, user = user)
  }

  fun get(contactId: Long, contactIdentityId: Long): ContactIdentityDetails = contactIdentityService.get(contactId, contactIdentityId) ?: throw EntityNotFoundException("Contact identity with id ($contactIdentityId) not found for contact ($contactId)")

  fun delete(contactId: Long, contactIdentityId: Long, user: User) {
    contactIdentityService.delete(contactId, contactIdentityId).also {
      outboundEventsService.send(
        outboundEvent = OutboundEvent.CONTACT_IDENTITY_DELETED,
        identifier = contactIdentityId,
        contactId = contactId,
        user = user,
      )
    }.also {
      telemetryContactCustomEventService.trackDeleteContactIdentityEvent(contactId = contactId, contactIdentityId = contactIdentityId, source = Source.DPS, user = user)
    }
  }
}
