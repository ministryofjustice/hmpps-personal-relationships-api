package uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.migrate.PrisonerRestrictionDetailsRequest

@Schema(description = "The request to remove all restrictions for a prisoner and add new ones")
data class ResetPrisonerRestrictionsRequest(
  @Schema(description = "The prisoner number to reset restrictions for")
  @field:NotBlank
  val prisonerNumber: String,

  @Schema(description = "Restriction records to be added")
  @field:Valid
  @field:Size(min = 1, message = "restrictions must contain at least one record")
  val restrictions: List<PrisonerRestrictionDetailsRequest>,
)
