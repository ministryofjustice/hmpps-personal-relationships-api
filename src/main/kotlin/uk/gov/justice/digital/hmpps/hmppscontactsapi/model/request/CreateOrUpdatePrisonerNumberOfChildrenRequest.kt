package uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Size

@Schema(description = "Request to update prisoner number of children")
data class CreateOrUpdatePrisonerNumberOfChildrenRequest(

  @Schema(description = "The number of children", example = "1")
  @field:Min(0)
  @field:Max(99)
  val numberOfChildren: Int?,

  @Schema(description = "User who requesting to create or update", example = "admin")
  @field:Size(max = 100, message = "requestedBy must be <= 100 characters")
  val requestedBy: String,
)
