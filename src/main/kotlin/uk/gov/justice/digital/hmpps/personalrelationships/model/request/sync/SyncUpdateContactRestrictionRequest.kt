package uk.gov.justice.digital.hmpps.personalrelationships.model.request.sync

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.LocalDateTime

@Schema(description = "Request to update a contact restriction ")
data class SyncUpdateContactRestrictionRequest(
  @Schema(description = "Unique identifier for the contact", example = "123")
  val contactId: Long,

  @Schema(description = "Type of restriction", example = "MOBILE")
  val restrictionType: String,

  @Schema(description = "Restriction start date", example = "2024-01-01")
  val startDate: LocalDate? = null,

  @Schema(description = "Restriction end date ", example = "2024-01-01")
  val expiryDate: LocalDate? = null,

  @Schema(description = "Any comments about the restriction ", example = "N/A")
  val comments: String? = null,

  @Schema(description = "The id of the user who updated the contact restriction", example = "JD000001")
  val updatedBy: String,

  @Schema(description = "The timestamp of when the contact restriction was changed", example = "2024-01-01T00:00:00Z")
  val updatedTime: LocalDateTime,
)
