package uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.migrate.PrisonerRestrictionDetailsRequest

@Schema(description = "The request to remove all restrictions for a prisoner and add new ones")
data class ResetPrisonerRestrictionsRequest(
  @Schema(description = "The prisoner number to reset restrictions for", example = "A1234BC", required = true)
  @field:NotBlank
  val prisonerNumber: String,

  @Schema(description = "Restriction records to be added", required = true)
  @field:Valid
  val restrictions: List<PrisonerRestrictionDetailsRequest>,
)
