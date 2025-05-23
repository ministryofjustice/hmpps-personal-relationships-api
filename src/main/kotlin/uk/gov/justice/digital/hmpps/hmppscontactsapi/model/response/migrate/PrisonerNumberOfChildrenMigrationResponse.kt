package uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.migrate

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Response object for prisoner's number of children migration")
data class PrisonerNumberOfChildrenMigrationResponse(

  @Schema(description = "The prisoner number", example = "A1234BC")
  val prisonerNumber: String,

  @Schema(description = "The current number of children")
  val current: Long?,

  @Schema(description = "Historical number of children records")
  val history: List<Long> = emptyList(),
)
