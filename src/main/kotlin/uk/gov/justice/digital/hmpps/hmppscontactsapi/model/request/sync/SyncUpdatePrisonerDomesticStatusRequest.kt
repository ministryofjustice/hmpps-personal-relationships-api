package uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

@Schema(description = "Request to update a prisoner's domestic status")
data class SyncUpdatePrisonerDomesticStatusRequest(

  @field:Size(min = 1, max = 1, message = "domesticStatusCode must be exactly 1 character")
  val domesticStatusCode: String?,

  @Schema(description = "Username of the person who created the record", example = "JSMITH_ADM")
  val createdBy: String,

  @Schema(description = "Timestamp when the record was created")
  val createdTime: LocalDateTime,

)
