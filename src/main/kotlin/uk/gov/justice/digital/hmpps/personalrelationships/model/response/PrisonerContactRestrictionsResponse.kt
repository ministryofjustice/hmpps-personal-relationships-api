package uk.gov.justice.digital.hmpps.personalrelationships.model.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Restriction related to a specific relationship between a prisoner and contact")
data class PrisonerContactRestrictionsResponse(
  @Schema(description = "Relationship specific restrictions")
  val prisonerContactRestrictions: List<PrisonerContactRestrictionDetails>,
  @Schema(description = "Global (estate-wide) restrictions for the contact")
  val contactGlobalRestrictions: List<ContactRestrictionDetails>,
)
