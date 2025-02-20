package uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.migrate

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Response object for prisoner's dependents migration")
data class PrisonerDependentsMigrationResponse(

  @Schema(description = "The prisoner number", example = "A1234BC")
  val prisonerNumber: String,

  @Schema(description = "The current dependents")
  val current: DependentsDetailsResponse,

  @Schema(description = "Historical dependents records")
  val history: List<DependentsDetailsResponse>,
)

@Schema(description = "Details of a prisoner's dependents record")
data class DependentsDetailsResponse(

  @Schema(description = "The unique identifier of the prisoner's dependents record", example = "1")
  val id: Long,

)
