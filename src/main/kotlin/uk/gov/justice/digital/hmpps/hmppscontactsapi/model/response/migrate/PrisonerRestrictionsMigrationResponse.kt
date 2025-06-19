package uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.migrate

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Response after migrating prisoner restrictions")
data class PrisonerRestrictionsMigrationResponse(
  @Schema(description = "The prisoner number", example = "A1234BC")
  val prisonerNumber: String,

  @Schema(description = "IDs of the migrated restriction records")
  val restrictionIds: List<Long>,
)
