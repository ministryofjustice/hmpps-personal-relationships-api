package uk.gov.justice.digital.hmpps.personalrelationships.model.response.sync

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

data class SyncPrisonerDomesticStatusResponse(
  @Schema(description = "The unique identifier of the prisoner's domestic status", example = "1")
  val id: Long,

  @Schema(description = "The domestic status value of the prisoner")
  val domesticStatusCode: String?,

  @Schema(description = "Is this the active domestic status code of the prisoner")
  val active: Boolean,

  @Schema(description = "Creation date and time")
  val createdTime: LocalDateTime? = null,

  @Schema(description = "Username of the creator")
  val createdBy: String? = null,
)

data class SyncPrisonerDomesticStatusResponseData(

  val data: SyncPrisonerDomesticStatusResponse,

  val status: Status = Status.UNCHANGED,

  val updatedId: Long? = null,
)

enum class Status {
  CREATED,
  UPDATED,
  UNCHANGED,
}
