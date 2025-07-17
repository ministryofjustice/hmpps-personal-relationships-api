package uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.sync

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Response object for a list of prisoner restriction IDs for a given prisoner")
data class PrisonerRestrictionIdsResponse(
  @Schema(description = "The prisoner number", example = "A1234BC")
  val prisonerNumber: String,

  @Schema(description = "List of restriction IDs", example = "[123, 456, 789]")
  val restrictionIds: List<Long>,
)
