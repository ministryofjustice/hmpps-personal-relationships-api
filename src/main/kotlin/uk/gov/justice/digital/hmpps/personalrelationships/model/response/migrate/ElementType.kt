package uk.gov.justice.digital.hmpps.personalrelationships.model.response.migrate

/**
 * Describes the valid type values for an IdPair object
 */
enum class ElementType(val elementType: String) {
  CONTACT("Contact"),
  PHONE("Phone"),
  EMAIL("Email"),
  ADDRESS("Address"),
  ADDRESS_PHONE("AddressPhone"),
  IDENTITY("Identity"),
  RESTRICTION("Restriction"),
  PRISONER_CONTACT("PrisonerContact"),
  PRISONER_CONTACT_RESTRICTION("PrisonerContactRestriction"),
  EMPLOYMENT("Employment"),
  ORGANISATION("Organisation"),
  WEB_ADDRESS("WebAddress"),
}
