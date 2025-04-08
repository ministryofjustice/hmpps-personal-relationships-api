package uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "A count of a prisoners contact relationships")
data class PrisonerContactRelationshipCount(
  @Schema(description = "The number of relationships with active status")
  val active: Long,
  @Schema(description = "The number of relationships with inactive status")
  val inactive: Long,
)
