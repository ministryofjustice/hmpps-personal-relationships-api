package uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.email

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size

@Schema(description = "A single email address")
data class EmailAddress(
  @field:Size(max = 240, message = "must be <= 240 characters")
  val emailAddress: String,
)
