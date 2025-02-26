package uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.email

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size

@Schema(description = "Request to update an email address")
data class UpdateEmailRequest(
  @Schema(description = "Email address", example = "test@example.com")
  @field:Size(max = 240, message = "emailAddress must be <= 240 characters")
  val emailAddress: String,

  @Schema(description = "User who updated the entry", example = "admin")
  @field:Size(max = 100, message = "updatedBy must be <= 100 characters")
  val updatedBy: String,
)
