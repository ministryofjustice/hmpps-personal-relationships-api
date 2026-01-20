package uk.gov.justice.digital.hmpps.personalrelationships.model.request.sync

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

@Schema(description = "Request to update a prisoner's domestic status")
data class SyncUpdatePrisonerDomesticStatusRequest(

  @Schema(description = "The domestic status code value", example = "1")
  @field:Size(max = 12, message = "domesticStatusCode must be less than or equal to 12 characters")
  val domesticStatusCode: String?,

  @Schema(description = "Username of the person who created the record", example = "JSMITH_ADM")
  val createdBy: String,

  @Schema(description = "Timestamp when the record was created")
  val createdTime: LocalDateTime,

)
