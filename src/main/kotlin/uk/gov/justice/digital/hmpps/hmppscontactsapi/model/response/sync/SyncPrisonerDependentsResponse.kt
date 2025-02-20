package uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.sync

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

data class SyncPrisonerDependentsResponse(
  @Schema(description = "The unique identifier of the prisoner's dependents", example = "1")
  val id: Long,

  @Schema(description = "The prison number of the prisoner")
  val prisonerNumber: String,

  @Schema(description = "The dependents count of the prisoner")
  val dependentsCount: String,

  @Schema(description = "Is this the active dependents code of the prisoner")
  val active: Boolean,

  @Schema(description = "Creation date and time")
  val createdTime: LocalDateTime? = null,

  @Schema(description = "Username of the creator")
  val createdBy: String? = null,
)
