package uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.sync

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

data class SyncPrisonerNumberOfChildrenResponse(
  @Schema(description = "The unique identifier of the prisoner's number of children", example = "1")
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

data class SyncPrisonerNumberOfChildrenData(

  val data: SyncPrisonerNumberOfChildrenResponse,

  val status: Status = Status.UNCHANGED,

  val updatedId: Long? = null,
)

enum class Status {
  CREATED,
  UPDATED,
  UNCHANGED,
}
