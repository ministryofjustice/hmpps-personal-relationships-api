package uk.gov.justice.digital.hmpps.personalrelationships.model.response.sync

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.LocalDateTime

@Schema(description = "Prisoner restriction sync response")
data class SyncPrisonerRestriction(
  @Schema(description = "The internal ID for the prisoner restriction", example = "12345")
  val prisonerRestrictionId: Long,

  @Schema(description = "The prisoner number", example = "A1234BC")
  val prisonerNumber: String,

  @Schema(description = "The restriction type", example = "NO_VISIT")
  val restrictionType: String,

  @Schema(description = "Effective date of the restriction", example = "2024-06-11")
  val effectiveDate: LocalDate,

  @Schema(description = "Expiry date of the restriction", example = "2024-12-31", nullable = true)
  val expiryDate: LocalDate? = null,

  @Schema(description = "Comment text", example = "No visits allowed")
  val commentText: String? = null,

  @Schema(description = "Authorised staff user name", example = "JSMITH")
  val authorisedUsername: String,

  @Schema(description = "True if this restriction applies to the latest or current term in prison, false if a previous term", example = "true")
  val currentTerm: Boolean,

  @Schema(description = "Username of the person who created the record", example = "JSMITH_ADM", nullable = true)
  val createdBy: String,

  @Schema(description = "Timestamp when the record was created", nullable = true)
  val createdTime: LocalDateTime,

  @Schema(description = "Username of the person who last updated the record", example = "JDOE_ADM", nullable = true)
  val updatedBy: String? = null,

  @Schema(description = "Timestamp when the record was last updated", nullable = true)
  val updatedTime: LocalDateTime? = null,
)
