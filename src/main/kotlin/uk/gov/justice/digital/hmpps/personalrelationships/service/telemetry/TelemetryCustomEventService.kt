package uk.gov.justice.digital.hmpps.personalrelationships.service.telemetry

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.personalrelationships.config.User
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.ContactAddressDetails
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.ContactAddressPhoneDetails
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.ContactCreationResult
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.ContactDetails
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.ContactEmailDetails
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.ContactIdentityDetails
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.ContactPhoneDetails
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.EmploymentDetails
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.PrisonerContactRelationshipDetails
import uk.gov.justice.digital.hmpps.personalrelationships.model.telemetry.ContactAddressCreatedCustomEvent
import uk.gov.justice.digital.hmpps.personalrelationships.model.telemetry.ContactAddressPhoneCreatedCustomEvent
import uk.gov.justice.digital.hmpps.personalrelationships.model.telemetry.ContactCreatedCustomEvent
import uk.gov.justice.digital.hmpps.personalrelationships.model.telemetry.ContactEmailCreatedCustomEvent
import uk.gov.justice.digital.hmpps.personalrelationships.model.telemetry.ContactEmploymentCreatedCustomEvent
import uk.gov.justice.digital.hmpps.personalrelationships.model.telemetry.ContactIdentityCreatedCustomEvent
import uk.gov.justice.digital.hmpps.personalrelationships.model.telemetry.ContactPhoneCreatedCustomEvent
import uk.gov.justice.digital.hmpps.personalrelationships.model.telemetry.PrisonerContactCreatedCustomEvent
import uk.gov.justice.digital.hmpps.personalrelationships.service.events.Source

@Service
class TelemetryCustomEventService(private val telemetryService: TelemetryService) {
  fun trackCreateContactCreationCustomEvent(contactCreationResult: ContactCreationResult, source: Source, user: User) {
    trackCreateContactCustomEvent(contactCreationResult.createdContact, source, user)
    contactCreationResult.createdRelationship?.let { createdRelationship ->
      trackCreatePrisonerContactCustomEvent(createdRelationship, source, user)
    }

    contactCreationResult.createdContact.identities.forEach { identityDetails ->
      trackCreateContactIdentityCustomEvent(identityDetails, source, user)
    }

    contactCreationResult.createdContact.addresses.forEach { addressDetails ->
      trackCreateContactAddressCustomEvent(addressDetails, source, user)
      addressDetails.phoneNumbers.forEach { phoneNumberDetails ->
        trackCreateContactAddressPhoneCustomEvent(phoneNumberDetails, source, user)
      }
    }

    contactCreationResult.createdContact.phoneNumbers.forEach { phoneNumberDetails ->
      trackCreateContactPhoneCustomEvent(phoneNumberDetails, source, user)
    }

    contactCreationResult.createdContact.emailAddresses.forEach { emailAddressDetails ->
      trackCreateContactEmailCustomEvent(emailAddressDetails, source, user)
    }

    contactCreationResult.createdContact.employments.forEach { employmentDetails ->
      trackCreateContactEmploymentCustomEvent(employmentDetails, source, user)
    }
  }

  private fun trackCreateContactCustomEvent(contact: ContactDetails, source: Source, user: User) {
    val event = ContactCreatedCustomEvent(contact, source, user)
    telemetryService.track(event)
  }

  fun trackCreatePrisonerContactCustomEvent(prisonerContactRelationship: PrisonerContactRelationshipDetails, source: Source, user: User) {
    val event = PrisonerContactCreatedCustomEvent(prisonerContactRelationship, source, user)
    telemetryService.track(event)
  }

  private fun trackCreateContactIdentityCustomEvent(contactIdentity: ContactIdentityDetails, source: Source, user: User) {
    val event = ContactIdentityCreatedCustomEvent(contactIdentity, source, user)
    telemetryService.track(event)
  }

  private fun trackCreateContactAddressCustomEvent(contactAddress: ContactAddressDetails, source: Source, user: User) {
    val event = ContactAddressCreatedCustomEvent(contactAddress, source, user)
    telemetryService.track(event)
  }


  private fun trackCreateContactAddressPhoneCustomEvent(contactAddressPhone: ContactAddressPhoneDetails, source: Source, user: User) {
    val event = ContactAddressPhoneCreatedCustomEvent(contactAddressPhone, source, user)
    telemetryService.track(event)
  }

  private fun trackCreateContactPhoneCustomEvent(contactPhoneDetails: ContactPhoneDetails, source: Source, user: User) {
    val event = ContactPhoneCreatedCustomEvent(contactPhoneDetails, source, user)
    telemetryService.track(event)
  }

  private fun trackCreateContactEmailCustomEvent(contactEmailDetails: ContactEmailDetails, source: Source, user: User) {
    val event = ContactEmailCreatedCustomEvent(contactEmailDetails, source, user)
    telemetryService.track(event)
  }

  private fun trackCreateContactEmploymentCustomEvent(employmentDetails: EmploymentDetails, source: Source, user: User) {
    val event = ContactEmploymentCreatedCustomEvent(employmentDetails, source, user)
    telemetryService.track(event)
  }
}
