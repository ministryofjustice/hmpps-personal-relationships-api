package uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Assess if a prisoner contact relationship can be deleted, has restrictions or can DOB be deleted")
data class RelationshipDeletePlan(
  @Schema(description = "Indicates if deleting the relationship will also delete the contact's date of birth")
  val willAlsoDeleteContactDob: Boolean,
  @Schema(description = "Indicates if the relationship has restrictions")
  val hasRestrictions: Boolean,
)
