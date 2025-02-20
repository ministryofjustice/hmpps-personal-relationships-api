package uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.sync

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.PrisonerDomesticStatus
import java.time.LocalDateTime

data class SyncPrisonerDomesticStatusResponse(
  @Schema(description = "The unique identifier of the domestic status", example = "1")
  val id: Long,

  @Schema(description = "The prison number of the prisoner")
  val prisonerNumber: String,

  @Schema(description = "The domestic status value of the prisoner")
  val domesticStatusCode: String?,

  @Schema(description = "Is this the active domestic status code of the prisoner")
  val active: Boolean,

  @Schema(description = "Creation date and time")
  val createdTime: LocalDateTime? = null,

  @Schema(description = "Username of the creator")
  val createdBy: String? = null,
) {
  companion object {
    fun from(domesticStatus: PrisonerDomesticStatus) = SyncPrisonerDomesticStatusResponse(
      id = domesticStatus.prisonerDomesticStatusId,
      prisonerNumber = domesticStatus.prisonerNumber,
      domesticStatusCode = domesticStatus.domesticStatusCode,
      createdTime = domesticStatus.createdTime,
      createdBy = domesticStatus.createdBy,
      active = domesticStatus.active,
    )
  }
}
