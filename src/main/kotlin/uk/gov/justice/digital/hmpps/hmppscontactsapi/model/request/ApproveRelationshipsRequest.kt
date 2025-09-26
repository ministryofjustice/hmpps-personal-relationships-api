package uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request

import io.swagger.v3.oas.annotations.media.Schema

data class ApproveRelationshipsRequest(
  @Schema(description = "A list of usernames identifying who created the relationships", required = true)
  val createdBy: List<String>,

  @Schema(description = "How many days to look back from today", required = true)
  val daysAgo: Long,
)
