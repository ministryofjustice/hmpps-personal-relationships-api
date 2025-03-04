package uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "Response object containing prisoner number of children information")
data class PrisonerNumberOfChildrenResponse(
  @Schema(description = "The unique identifier of the number of children", example = "1")
  val id: Long,

  @Schema(description = "The number of children of the prisoner")
  val numberOfChildren: String?,

  @Schema(description = "Is this the active number of children of the prisoner")
  val active: Boolean,

  @Schema(description = "Creation date and time")
  val createdTime: LocalDateTime? = null,

  @Schema(description = "Username of the creator")
  val createdBy: String? = null,
)
