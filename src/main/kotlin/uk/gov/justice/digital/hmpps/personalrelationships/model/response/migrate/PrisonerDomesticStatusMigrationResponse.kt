package uk.gov.justice.digital.hmpps.personalrelationships.model.response.migrate

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Response object for prisoner's domestic status migration")
data class PrisonerDomesticStatusMigrationResponse(

  @Schema(description = "The prisoner number", example = "A1234BC")
  val prisonerNumber: String,

  @Schema(description = "The current domestic status")
  val current: Long?,

  @Schema(description = "Historical domestic status records")
  val history: List<Long> = emptyList(),
)
