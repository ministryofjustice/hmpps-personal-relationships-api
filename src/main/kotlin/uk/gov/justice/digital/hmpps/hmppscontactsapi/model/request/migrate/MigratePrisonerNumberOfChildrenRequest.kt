package uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.migrate

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

@Schema(description = "Request to migrate a prisoner's number of children")
data class MigratePrisonerNumberOfChildrenRequest(
  @Schema(description = "The prisoner number", example = "A1234BC")
  val prisonerNumber: String,

  @Schema(description = "Current number of children details")
  @field:Valid
  val current: NumberOfChildrenDetailsRequest? = null,

  @Schema(description = "Historical number of children records")
  @field:Valid
  val history: List<NumberOfChildrenDetailsRequest> = emptyList(),
)

@Schema(description = "Details of a number of children record")
data class NumberOfChildrenDetailsRequest(

  @Schema(description = "The number of children", example = "1")
  @field:Size(min = 1, max = 50, message = "numberOfChildren must be less than 50 characters")
  val numberOfChildren: String,

  @Schema(description = "Username of the person who created the record", example = "JSMITH_ADM")
  val createdBy: String,

  @Schema(description = "Timestamp when the record was created")
  val createdTime: LocalDateTime,
)
