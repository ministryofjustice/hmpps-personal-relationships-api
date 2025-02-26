package uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.email

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.Size

@Schema(description = "Request to create a new email address")
data class CreateMultipleEmailsRequest(

  @Schema(description = "Email addresses")
  @field:Valid
  @field:Size(min = 1, message = "emailAddresses must have at least 1 item")
  val emailAddresses: List<EmailAddress>,

  @Schema(description = "User who created the entry", example = "admin")
  @field:Size(max = 100, message = "createdBy must be <= 100 characters")
  val createdBy: String,
)
