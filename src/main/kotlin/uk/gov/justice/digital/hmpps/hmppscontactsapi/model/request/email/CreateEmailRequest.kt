package uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.email

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size

@Schema(description = "Request to create a new email address")
data class CreateEmailRequest(
  @Schema(description = "Email address", example = "test@example.com")
  @field:Size(max = 240, message = "emailAddress must be <= 240 characters")
  val emailAddress: String,
)
