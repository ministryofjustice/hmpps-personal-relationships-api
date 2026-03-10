package uk.gov.justice.digital.hmpps.personalrelationships.service.telemetry

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

enum class TelemetryCustomEventType(val eventName: String, val description: String) {
  CONTACT_CREATED_EVENT("contact-created", "A contact has been created"),
  CONTACT_UPDATED_EVENT("contact-updated", "A contact has been updated"),
  CONTACT_DELETED_EVENT("contact-deleted", "A contact has been deleted"),
  CONTACT_ADDRESS_CREATED_EVENT("contact-address-created", "A contact address has been created"),
  CONTACT_ADDRESS_UPDATED_EVENT("contact-address-updated", "A contact address has been updated"),
  CONTACT_ADDRESS_DELETED_EVENT("contact-address-deleted", "A contact address has been deleted"),
  CONTACT_ADDRESS_PHONE_CREATED_EVENT("contact-address-phone-created", "A contact address has been created"),
  CONTACT_ADDRESS_PHONE_UPDATED_EVENT("contact-address-phone-updated", "A contact address has been updated"),
  CONTACT_ADDRESS_PHONE_DELETED_EVENT("contact-address-phone-deleted", "A contact address has been deleted"),
  PRISONER_CONTACT_CREATED_EVENT("prisoner-contact-created", "A prisoner contact has been created"),
  PRISONER_CONTACT_UPDATED_EVENT("prisoner-contact-updated", "A prisoner contact has been updated"),
  PRISONER_CONTACT_DELETED_EVENT("prisoner-contact-deleted", "A prisoner contact has been deleted"),
  CONTACT_IDENTITY_CREATED_EVENT("contact-identity-created", "A contact has been created"),
  CONTACT_IDENTITY_UPDATED_EVENT("contact-identity-updated", "A contact has been updated"),
  CONTACT_IDENTITY_DELETED_EVENT("contact-identity-deleted", "A contact has been deleted"),
  CONTACT_PHONE_CREATED_EVENT("contact-phone-created", "A contact address has been created"),
  CONTACT_PHONE_UPDATED_EVENT("contact-phone-updated", "A contact address has been updated"),
  CONTACT_PHONE_DELETED_EVENT("contact-phone-deleted", "A contact address has been deleted"),
  CONTACT_EMAIL_CREATED_EVENT("contact-email-created", "A contact address has been created"),
  CONTACT_EMAIL_UPDATED_EVENT("contact-email-updated", "A contact address has been updated"),
  CONTACT_EMAIL_DELETED_EVENT("contact-email-deleted", "A contact address has been deleted"),
  CONTACT_EMPLOYMENT_CREATED_EVENT("contact-email-created", "A contact employment has been created"),
  CONTACT_EMPLOYMENT_UPDATED_EVENT("contact-email-updated", "A contact employment has been updated"),
  CONTACT_EMPLOYMENT_DELETED_EVENT("contact-email-deleted", "A contact employment has been deleted"),
  CONTACT_RESTRICTION_CREATED_EVENT("contact-restriction-created", "A contact restriction has been created"),
  CONTACT_RESTRICTION_UPDATED_EVENT("contact-restriction-updated", "A contact restriction has been updated"),
  CONTACT_RESTRICTION_DELETED_EVENT("contact-restriction-deleted", "A contact restriction has been deleted"),
  PRISONER_CONTACT_RESTRICTION_CREATED_EVENT("contact-restriction-created", "A prisoner contact restriction has been created"),
  PRISONER_CONTACT_RESTRICTION_UPDATED_EVENT("contact-restriction-created", "A prisoner contact restriction has been updated"),
  PRISONER_CONTACT_RESTRICTION_DELETED_EVENT("contact-restriction-created", "A prisoner contact restriction has been deleted"),
  PRISONER_NUMBER_OF_CHILDREN_CREATED_EVENT("number-of-children-created","A number of children record has been created"),
  PRISONER_NUMBER_OF_CHILDREN_UPDATED_EVENT("number-of-children-updated","A number of children record has been updated"),
  PRISONER_DOMESTIC_STATUS_CREATED_EVENT("personal-relationships-api.domestic-status-created", "A domestic status record has been created"),
  PRISONER_DOMESTIC_STATUS_UPDATED_EVENT("personal-relationships-api.domestic-status-updated", "A domestic status record has been updated"),
  PRISONER_RESTRICTION_CREATED_EVENT("prisoner-restriction-created", "A prisoner restriction has been created"),
  PRISONER_RESTRICTION_UPDATED_EVENT("prisoner-restriction-updated", "A prisoner restriction has been updated"),
  PRISONER_RESTRICTION_DELETED_EVENT("prisoner-restriction-deleted", "A prisoner restriction has been deleted")
}
