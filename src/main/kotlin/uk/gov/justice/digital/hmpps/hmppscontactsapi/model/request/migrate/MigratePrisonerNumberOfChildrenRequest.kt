package uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.migrate

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "Request to migrate a prisoner's number of children")
data class MigratePrisonerNumberOfChildrenRequest(

  @Schema(description = "The prisoner number")
  val prisonerNumber: String,

  @Schema(description = "Current number of children details")
  val current: NumberOfChildrenDetailsRequest? = null,

  @Schema(description = "Historical number of children records")
  val history: List<NumberOfChildrenDetailsRequest> = emptyList(),
)

@Schema(description = "Details of a number of children record")
data class NumberOfChildrenDetailsRequest(

  @Schema(description = "The number of children", example = "1")
  val numberOfChildren: String,

  @Schema(description = "Username of the person who created the record", example = "JSMITH_ADM")
  val createdBy: String,

  @Schema(description = "Timestamp when the record was created")
  val createdTime: LocalDateTime,
)
