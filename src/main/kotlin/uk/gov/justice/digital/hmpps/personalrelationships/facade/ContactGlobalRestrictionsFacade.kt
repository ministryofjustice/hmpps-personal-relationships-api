package uk.gov.justice.digital.hmpps.personalrelationships.facade

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.personalrelationships.config.User
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.restrictions.CreateContactRestrictionRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.restrictions.UpdateContactRestrictionRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.ContactRestrictionDetails
import uk.gov.justice.digital.hmpps.personalrelationships.service.RestrictionsService
import uk.gov.justice.digital.hmpps.personalrelationships.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.personalrelationships.service.events.OutboundEventsService

@Service
class ContactGlobalRestrictionsFacade(
  private val restrictionsService: RestrictionsService,
  private val outboundEventsService: OutboundEventsService,
) {

  fun getGlobalRestrictionsForContact(contactId: Long): List<ContactRestrictionDetails> = restrictionsService.getGlobalRestrictionsForContact(contactId)

  fun createContactGlobalRestriction(
    contactId: Long,
    request: CreateContactRestrictionRequest,
    user: User,
  ): ContactRestrictionDetails = restrictionsService.createContactGlobalRestriction(contactId, request, user).also {
    outboundEventsService.send(
      outboundEvent = OutboundEvent.CONTACT_RESTRICTION_CREATED,
      identifier = it.contactRestrictionId,
      contactId = contactId,
      user = user,
    )
  }

  fun updateContactGlobalRestriction(
    contactId: Long,
    contactRestrictionId: Long,
    request: UpdateContactRestrictionRequest,
    user: User,
  ): ContactRestrictionDetails = restrictionsService.updateContactGlobalRestriction(contactId, contactRestrictionId, request, user).also {
    outboundEventsService.send(
      outboundEvent = OutboundEvent.CONTACT_RESTRICTION_UPDATED,
      identifier = contactRestrictionId,
      contactId = contactId,
      user = user,
    )
  }
}
