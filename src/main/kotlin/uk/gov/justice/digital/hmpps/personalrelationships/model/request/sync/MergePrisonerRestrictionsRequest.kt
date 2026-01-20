package uk.gov.justice.digital.hmpps.personalrelationships.model.request.sync

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.migrate.PrisonerRestrictionDetailsRequest

@Schema(description = "The request to merge restrictions from one prisoner to another, resetting the retained prisoner's restrictions to those in the request and deleting all from the removed prisoner.")
data class MergePrisonerRestrictionsRequest(
  @Schema(description = "The prisoner number to retain restrictions for", example = "A1234BC", required = true)
  @field:NotBlank
  val keepingPrisonerNumber: String,

  @Schema(description = "The prisoner number to remove restrictions from", example = "A1234BD", required = true)
  @field:NotBlank
  val removingPrisonerNumber: String,

  @Schema(description = "Restriction records to be set for the retained prisoner", required = true)
  @field:Valid
  val restrictions: List<PrisonerRestrictionDetailsRequest>,
)
