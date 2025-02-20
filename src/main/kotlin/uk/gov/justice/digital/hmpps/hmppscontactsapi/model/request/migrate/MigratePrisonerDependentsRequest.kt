package uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.migrate

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "Request to migrate a prisoner's dependents")
data class MigratePrisonerDependentsRequest(

  @Schema(description = "Current dependents details")
  val current: DependentsDetailsRequest? = null,

  @Schema(description = "Historical dependents records")
  val history: List<DependentsDetailsRequest> = emptyList(),
)

@Schema(description = "Details of a dependents record")
data class DependentsDetailsRequest(

  @Schema(description = "The dependents count", example = "1")
  val dependentsCount: String,

  @Schema(description = "Username of the person who created the record", example = "JSMITH_ADM")
  val createdBy: String,

  @Schema(description = "Timestamp when the record was created")
  val createdTime: LocalDateTime,
)
