package uk.gov.justice.digital.hmpps.personalrelationships.model.request.sync

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.LocalDateTime

@Schema(description = "Request object to update te  prisoner contact restriction details")
data class SyncUpdatePrisonerContactRestrictionRequest(

  @Schema(description = "Type of restriction applied", example = "NoContact")
  val restrictionType: String,

  @Schema(description = "Start date of the restriction", example = "2024-01-01", nullable = true)
  val startDate: LocalDate? = null,

  @Schema(description = "Expiry date of the restriction, if applicable", example = "2024-12-31", nullable = true)
  val expiryDate: LocalDate? = null,

  @Schema(description = "Comments regarding the restriction", example = "Restriction applied due to safety concerns", nullable = true)
  val comments: String? = null,

  @Schema(description = "User who last updated the restriction record", example = "editor", nullable = true)
  val updatedBy: String? = null,

  @Schema(description = "Time when the restriction record was last updated", example = "2024-10-02T15:30:00Z", nullable = true)
  val updatedTime: LocalDateTime? = null,
)
