package uk.gov.justice.digital.hmpps.personalrelationships.model.response.migrate

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Response after migrating prisoner restriction")
data class PrisonerRestrictionMigrationResponse(
  @Schema(description = "The prisoner number", example = "A1234BC")
  val prisonerNumber: String,

  @Schema(description = "ID of the migrated restriction record")
  val prisonerRestrictionId: Long,
)
