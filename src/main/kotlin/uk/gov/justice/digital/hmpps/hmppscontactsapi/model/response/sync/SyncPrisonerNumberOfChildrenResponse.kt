package uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.sync

import io.swagger.v3.oas.annotations.media.Schema

data class SyncPrisonerNumberOfChildrenResponse(
  @Schema(description = "The unique identifier of the prisoner's number of children", example = "1")
  val id: Long,
)
