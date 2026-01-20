package uk.gov.justice.digital.hmpps.personalrelationships.model.request.employment

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Request allowing several changes to employments in a single request.")
data class PatchEmploymentsRequest(
  @Schema(description = "List of new employments to create", required = true)
  val createEmployments: List<Employment>,

  @Schema(description = "List of updates to apply to existing employments", required = true)
  val updateEmployments: List<PatchEmploymentsUpdateEmployment>,

  @Schema(description = "List of ids for employments to delete", required = true)
  val deleteEmployments: List<Long>,
)
