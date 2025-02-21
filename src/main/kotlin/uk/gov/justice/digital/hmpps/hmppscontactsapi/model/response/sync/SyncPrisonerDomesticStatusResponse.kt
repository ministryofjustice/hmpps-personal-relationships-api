package uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.sync

import io.swagger.v3.oas.annotations.media.Schema

data class SyncPrisonerDomesticStatusResponse(
  @Schema(description = "The unique identifier of the prisoner's domestic status", example = "1")
  val id: Long,
)
