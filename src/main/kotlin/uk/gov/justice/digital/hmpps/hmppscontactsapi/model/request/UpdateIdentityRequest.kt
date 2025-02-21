package uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size

@Schema(description = "Request to update an existing contact identity")
data class UpdateIdentityRequest(
  @Schema(description = "Type of identity", example = "DL")
  @field:Size(max = 12, message = "identityType must be <= 12 characters")
  val identityType: String,

  @Schema(description = "The identity value such as driving licence number", example = "DL123456789")
  @field:Size(max = 20, message = "identityValue must be <= 20 characters")
  val identityValue: String,

  @Schema(description = "The authority who issued the identity", example = "DVLA", nullable = true)
  @field:Size(max = 40, message = "issuingAuthority must be <= 40 characters")
  val issuingAuthority: String? = null,

  @Schema(description = "User who updated the entry", example = "admin")
  @field:Size(max = 100, message = "updatedBy must be <= 100 characters")
  val updatedBy: String,
)
