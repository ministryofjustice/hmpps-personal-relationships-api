package uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size

@Schema(description = "Request to create or update prisoner domestic status")
data class CreateOrUpdatePrisonerDomesticStatusRequest(

  @Schema(description = "The domestic status code", example = "M")
  @field:Size(max = 12, message = "domesticStatusCode must be less than or equal to 12 characters")
  val domesticStatusCode: String?,

  @Schema(description = "User who updated the entry", example = "admin")
  @field:Size(max = 100, message = "updatedBy must be <= 100 characters")
  val updatedBy: String,
)
