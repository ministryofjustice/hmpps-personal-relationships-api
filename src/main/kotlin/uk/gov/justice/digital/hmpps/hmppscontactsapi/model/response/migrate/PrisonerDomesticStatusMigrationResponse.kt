package uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.migrate

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "Response object for domestic status migration")
data class PrisonerDomesticStatusMigrationResponse(

  @Schema(description = "The prisoner number", example = "A1234BC")
  val prisonerNumber: String,

  @Schema(description = "The current domestic status")
  val current: DomesticStatusDetailsResponse,

  @Schema(description = "Historical domestic status records")
  val history: List<DomesticStatusDetailsResponse>,
)

@Schema(description = "Details of a domestic status record")
data class DomesticStatusDetailsResponse(

  @Schema(description = "The unique identifier of the domestic status", example = "1")
  val id: Long,

  @Schema(description = "The domestic status code", example = "M")
  val domesticStatusCode: String,

  @Schema(description = "Username of the person who created the record", example = "JSMITH_ADM")
  val createdBy: String,

  @Schema(description = "Timestamp when the record was created")
  val createdTime: LocalDateTime,
)
