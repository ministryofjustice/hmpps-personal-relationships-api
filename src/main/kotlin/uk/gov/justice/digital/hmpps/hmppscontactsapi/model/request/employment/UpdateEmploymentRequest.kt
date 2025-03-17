package uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.employment

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size

@Schema(description = "Request to update an existing employment's employer or active flag.")
data class UpdateEmploymentRequest(
  @Schema(description = "The organisation id", example = "123456789", nullable = false, required = true)
  val organisationId: Long,
  @Schema(description = "Whether this is a current employment or not", nullable = false, required = true)
  val isActive: Boolean,
  @Schema(description = "The id of the user who updated the entry", example = "JD000001")
  @field:Size(max = 100, message = "updatedBy must be <= 100 characters")
  val updatedBy: String,
)
