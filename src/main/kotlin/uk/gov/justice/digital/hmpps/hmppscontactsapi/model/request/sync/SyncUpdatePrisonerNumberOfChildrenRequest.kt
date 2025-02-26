package uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

@Schema(description = "Request to update a prisoner's number of children")
data class SyncUpdatePrisonerNumberOfChildrenRequest(

  @Schema(description = "The number of children", example = "1")
  @field:Size(max = 50, message = "numberOfChildren must be less than or equal to 50 characters")
  val numberOfChildren: String?,

  @Schema(description = "Username of the person who created the record", example = "JSMITH_ADM")
  val createdBy: String,

  @Schema(description = "Timestamp when the record was created")
  val createdTime: LocalDateTime,

)
