package uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size

data class ContactRelationship(

  @Schema(description = "Prisoner number (NOMS ID)", example = "A1234BC")
  val prisonerNumber: String,

  @Schema(description = "The relationship between the prisoner and the contact - for social(S) or official(O)", example = "S")
  val relationshipType: String,

  @Schema(description = "The relationship to the prisoner. A code from SOCIAL_RELATIONSHIP or OFFICIAL_RELATIONSHIP reference data groups depending on the relationship type.", example = "FRI")
  val relationshipToPrisoner: String,

  @Schema(description = "Whether they are the next of kin for the prisoner", example = "true", required = true)
  @JsonProperty(required = true)
  val isNextOfKin: Boolean,

  @Schema(description = "Whether they are the emergency contact for the prisoner", example = "true", required = true)
  @JsonProperty(required = true)
  val isEmergencyContact: Boolean,

  @Schema(description = "Comments about the contacts relationship with the prisoner", example = "Some additional information", nullable = true)
  @field:Size(max = 240, message = "comments must be <= 240 characters")
  val comments: String? = null,

)
