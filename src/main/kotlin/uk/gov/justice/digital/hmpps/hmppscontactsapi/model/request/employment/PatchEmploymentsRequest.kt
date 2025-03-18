package uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.employment

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size

@Schema(description = "Request allowing several changes to employments in a single request.")
data class PatchEmploymentsRequest(
  @Schema(description = "List of new employments to create", required = true)
  val createEmployments: List<Employment>,

  @Schema(description = "List of updates to apply to existing employments", required = true)
  val updateEmployments: List<PatchEmploymentsUpdateEmployment>,

  @Schema(description = "List of ids for employments to delete", required = true)
  val deleteEmployments: List<Long>,

  @Schema(description = "The id of the user requesting the changes. Will become created by for new employments and updated by for updated employments", required = true)
  @field:Size(max = 100, message = "requestedBy must be <= 100 characters")
  val requestedBy: String,
)
