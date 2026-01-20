package uk.gov.justice.digital.hmpps.personalrelationships.model.request.sync

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "Request to update a new contact identity ")
data class SyncUpdateContactIdentityRequest(
  @Schema(description = "Unique identifier for the contact", example = "123")
  val contactId: Long,

  @Schema(description = "Type of identity", example = "DL")
  val identityType: String,

  @Schema(description = "Identity ", example = "S99PH898989L")
  val identityValue: String,

  @Schema(description = "Issuing authority", example = "DVLA", nullable = true, maxLength = 40)
  val issuingAuthority: String?,

  @Schema(description = "The id of the user who updated the contact identity", example = "JD000001")
  val updatedBy: String,

  @Schema(description = "The timestamp of when the contact identity was changed", example = "2024-01-01T00:00:00Z")
  val updatedTime: LocalDateTime,
)
