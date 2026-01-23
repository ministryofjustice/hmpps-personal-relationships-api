package uk.gov.justice.digital.hmpps.personalrelationships.model.response

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "Response object containing prisoner domestic status information")
data class PrisonerDomesticStatusResponse(
  @Schema(description = "The unique identifier of the domestic status", example = "1")
  val id: Long,

  @Schema(description = "The domestic status code of the prisoner")
  val domesticStatusCode: String?,

  @Schema(description = "The domestic status description of the prisoner")
  val domesticStatusDescription: String? = null,

  @Schema(description = "Is this the active domestic status code of the prisoner")
  val active: Boolean,

  @Schema(description = "Creation date and time")
  val createdTime: LocalDateTime? = null,

  @Schema(description = "Username of the creator")
  val createdBy: String? = null,
)
