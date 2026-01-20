package uk.gov.justice.digital.hmpps.personalrelationships.model.response.sync

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.LocalDateTime

@Schema(description = "Response object with prisoner contact restriction details")
data class SyncPrisonerContactRestriction(

  @Schema(description = "ID of the prisoner contact restriction", example = "232")
  val prisonerContactRestrictionId: Long,

  @Schema(description = "ID of the prisoner contact (relationship) to which the restriction applies", example = "12345")
  val prisonerContactId: Long,

  @Schema(description = "ID of the contact (person) to which the restriction applies", example = "12345")
  val contactId: Long,

  @Schema(description = "The prisoner number involved in this relationship restriction", example = "A1234AA")
  val prisonerNumber: String,

  @Schema(
    description =
    """
    The coded type of restriction that applies to this relationship.
    This is a coded value from the group RESTRICTION in reference codes.
    Example values include ACC, BAN, CHILD, CLOSED, RESTRICTED, DIHCON, NONCON.
    """,
    example = "NONCON",
    nullable = true,
  )
  val restrictionType: String? = null,

  @Schema(description = "Start date of the restriction", example = "2024-01-01", nullable = true)
  val startDate: LocalDate? = null,

  @Schema(description = "Expiry date of the restriction, if applicable", example = "2024-12-31", nullable = true)
  val expiryDate: LocalDate? = null,

  @Schema(description = "Comments regarding the restriction", example = "Restriction applied due to safety concerns", nullable = true)
  val comments: String? = null,

  @Schema(description = "User who created the restriction record", example = "admin", nullable = true)
  val createdBy: String? = null,

  @Schema(description = "Time when the restriction record was created", example = "2024-10-01T12:00:00Z", nullable = true)
  val createdTime: LocalDateTime? = null,

  @Schema(description = "User who last updated the restriction record", example = "editor", nullable = true)
  val updatedBy: String? = null,

  @Schema(description = "Time when the restriction record was last updated", example = "2024-10-02T15:30:00Z", nullable = true)
  val updatedTime: LocalDateTime? = null,
)
