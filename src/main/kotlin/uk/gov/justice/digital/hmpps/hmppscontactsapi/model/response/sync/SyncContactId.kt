package uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.sync

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Response object for sync reconciliation")
data class SyncContactId(
  @Schema(description = "The ID for an contact", example = "111111")
  val contactId: Long,
)
