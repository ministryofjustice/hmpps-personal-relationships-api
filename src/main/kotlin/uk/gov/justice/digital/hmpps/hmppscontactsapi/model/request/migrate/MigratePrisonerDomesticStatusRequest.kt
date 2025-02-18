package uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.migrate

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "Request to migrate a prisoner's domestic status")
data class MigratePrisonerDomesticStatusRequest(

  @Schema(description = "The prisoner number", example = "A1234BC")
  val prisonerNumber: String,

  @Schema(description = "Current domestic status details")
  val current: DomesticStatusDetailsRequest? = null,

  @Schema(description = "Historical domestic status records")
  val history: List<DomesticStatusDetailsRequest>,
)

@Schema(description = "Details of a domestic status record")
data class DomesticStatusDetailsRequest(

  @Schema(description = "The domestic status code", example = "M")
  val domesticStatusCode: String,

  @Schema(description = "Username of the person who created the record", example = "JSMITH_ADM")
  val createdBy: String,

  @Schema(description = "Timestamp when the record was created")
  val createdTime: LocalDateTime,
)
