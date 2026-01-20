package uk.gov.justice.digital.hmpps.personalrelationships.model.request.sync

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "Request to update a contact email address by sync with NOMIS")
data class SyncUpdateContactEmailRequest(
  @Schema(description = "Unique identifier for the contact", example = "123")
  val contactId: Long,

  @Schema(description = "Email address", example = "test@example.com")
  val emailAddress: String,

  @Schema(description = "The id of the user who updated the contact email", example = "JD000001")
  val updatedBy: String,

  @Schema(description = "The timestamp of when the contact email was changed", example = "2024-01-01T00:00:00Z")
  val updatedTime: LocalDateTime,
)
