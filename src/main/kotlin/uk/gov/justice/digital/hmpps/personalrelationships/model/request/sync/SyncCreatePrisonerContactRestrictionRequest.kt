package uk.gov.justice.digital.hmpps.personalrelationships.model.request.sync

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.LocalDateTime

@Schema(description = "Request object to create a prisoner contact restriction")
data class SyncCreatePrisonerContactRestrictionRequest(

  @Schema(description = "ID of the prisoner contact (relationship) on which the restriction applies", example = "12345")
  val prisonerContactId: Long,

  @Schema(description = "Type of restriction applied", example = "NoContact")
  val restrictionType: String,

  @Schema(description = "Start date of the restriction", example = "2024-01-01", nullable = true)
  val startDate: LocalDate? = null,

  @Schema(description = "Expiry date of the restriction, if applicable", example = "2024-12-31", nullable = true)
  val expiryDate: LocalDate? = null,

  @Schema(description = "Comments regarding the restriction", example = "Restriction applied due to safety concerns", nullable = true)
  val comments: String? = null,

  @Schema(description = "User who created the restriction record", example = "admin")
  val createdBy: String,

  @Schema(description = "Time when the restriction record was created", example = "2024-10-01T12:00:00Z")
  val createdTime: LocalDateTime,
)
