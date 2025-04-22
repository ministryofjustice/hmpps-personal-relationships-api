package uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min

@Schema(description = "Request to update prisoner number of children")
data class CreateOrUpdatePrisonerNumberOfChildrenRequest(

  @Schema(description = "The number of children", example = "1")
  @field:Min(0)
  @field:Max(99)
  val numberOfChildren: Int?,

)
