package uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.employment

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size

@Schema(description = "Request to create a new employment with an employer and whether it is active or inactive")
data class CreateEmploymentRequest(
  @Schema(description = "The organisation id", example = "123456789", nullable = false, required = true)
  val organisationId: Long,
  @Schema(description = "Whether this is a current employment or not", nullable = false, required = true)
  val isActive: Boolean,
  @Schema(description = "The id of the user who created the entry", example = "JD000001")
  @field:Size(max = 100, message = "createdBy must be <= 100 characters")
  val createdBy: String,
)
