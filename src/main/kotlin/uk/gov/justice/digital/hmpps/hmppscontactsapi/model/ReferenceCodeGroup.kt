package uk.gov.justice.digital.hmpps.hmppscontactsapi.model

import io.swagger.v3.oas.annotations.media.Schema

@Schema(enumAsRef = true)
enum class ReferenceCodeGroup(val displayName: String, val isDocumented: Boolean) {
  DOMESTIC_STS("domestic status", true),
  OFFICIAL_RELATIONSHIP("official relationship", true),
  ID_TYPE("identity type", true),
  LANGUAGE("language", true),
  GENDER("gender", true),
  SOCIAL_RELATIONSHIP("social relationship", true),
  CITY("city", true),
  COUNTY("county", true),
  RELATIONSHIP_TYPE("contact type", true),
  COUNTRY("country", true),
  ADDRESS_TYPE("address type", true),
  PHONE_TYPE("phone type", true),
  RESTRICTION("restriction type", true),
  TITLE("title", true),
  TEST_TYPE("test type", false),
}
