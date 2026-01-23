package uk.gov.justice.digital.hmpps.personalrelationships.model.response.sync

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "Identity related to a contact")
data class SyncContactIdentity(
  @Schema(description = "Unique identifier for the contact identity", example = "1")
  val contactIdentityId: Long,

  @Schema(description = "Unique identifier for the contact", example = "123")
  val contactId: Long,

  @Schema(description = "Type of identity", example = "DL")
  val identityType: String,

  @Schema(description = "Identity ", example = "DL090 0909 909")
  val identityValue: String,

  @Schema(description = "Issuing authority", example = "DVLA")
  val issuingAuthority: String?,

  @Schema(description = "User who created the entry", example = "admin")
  val createdBy: String,

  @Schema(description = "Timestamp when the entry was created", example = "2023-09-23T10:15:30")
  val createdTime: LocalDateTime,

  @Schema(description = "User who updated the entry", example = "admin2")
  val updatedBy: String?,

  @Schema(description = "Timestamp when the entry was updated", example = "2023-09-24T12:00:00")
  val updatedTime: LocalDateTime?,
)
