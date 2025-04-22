package uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size
import org.openapitools.jackson.nullable.JsonNullable

@Schema(description = "Request to update an existing relationship details")
data class PatchRelationshipRequest(

  @Schema(description = "The code representing the relationship type as social or official", example = "S", nullable = false, type = "string", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  val relationshipTypeCode: JsonNullable<String> = JsonNullable.undefined(),

  @Schema(description = "The relationship reference code between the prisoner and the contact", example = "FRI", nullable = false, type = "string", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  val relationshipToPrisonerCode: JsonNullable<String> = JsonNullable.undefined(),

  @Schema(description = "Whether they are the emergency contact for the prisoner", example = "true", nullable = false, type = "boolean", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty(required = true)
  val isEmergencyContact: JsonNullable<Boolean> = JsonNullable.undefined(),

  @Schema(description = "Whether they are approved to visit the prisoner", example = "true", nullable = false, type = "boolean", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty(required = true)
  val isApprovedVisitor: JsonNullable<Boolean> = JsonNullable.undefined(),

  @Schema(description = "Whether they are the next of kin for the prisoner", example = "true", nullable = false, type = "boolean", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty(required = true)
  val isNextOfKin: JsonNullable<Boolean> = JsonNullable.undefined(),

  @Schema(description = "Whether the relationship is active", example = "true", nullable = false, type = "boolean", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty(required = true)
  val isRelationshipActive: JsonNullable<Boolean> = JsonNullable.undefined(),

  @Schema(description = "Comments about the contacts relationship with the prisoner", example = "Some additional information", nullable = true, type = "string", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @field:Size(max = 240, message = "comments must be <= 240 characters")
  val comments: JsonNullable<String?> = JsonNullable.undefined(),

)
