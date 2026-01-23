package uk.gov.justice.digital.hmpps.personalrelationships.model.request.sync

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "Request to create a new contact identity ")
data class SyncCreateContactIdentityRequest(
  @Schema(description = "Unique identifier for the contact", example = "123")
  val contactId: Long,

  @Schema(description = "Type of identity", example = "DL")
  val identityType: String,

  @Schema(description = "Identity number or reference", example = "HP9909SM1883")
  val identityValue: String,

  @Schema(description = "Issuing authority ", example = "DVLA")
  val issuingAuthority: String?,

  @Schema(description = "User who created the entry", example = "JJ99821")
  val createdBy: String,

  @Schema(description = "The timestamp of when the identity was created", example = "2024-01-01T00:00:00Z")
  val createdTime: LocalDateTime = LocalDateTime.now(),
)
