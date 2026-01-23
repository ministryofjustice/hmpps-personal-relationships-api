package uk.gov.justice.digital.hmpps.personalrelationships.model.response

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.LocalDateTime

@Schema(description = "Restriction related to a prisoner and contacts relationship")
data class PrisonerContactRestrictionDetails(
  @Schema(description = "The unique identifier for the prisoner contact restriction", example = "123456")
  val prisonerContactRestrictionId: Long,

  @Schema(description = "The unique identifier for the prisoner contact", example = "123456")
  val prisonerContactId: Long,

  @Schema(description = "The unique identifier for the contact", example = "123456")
  val contactId: Long,

  @Schema(description = "The prisoner number", example = "A1234BC")
  val prisonerNumber: String,

  @Schema(
    description =
    """
    The coded type of restriction that applies to this relationship.
    This is a coded value from the group RESTRICTION in reference codes.
    Example values include ACC, BAN, CHILD, CLOSED, RESTRICTED, DIHCON, NONCON.
    """,
    example = "BAN",
  )
  val restrictionType: String,

  @Schema(description = "The description of restrictionType", example = "Banned")
  val restrictionTypeDescription: String,

  @Schema(description = "Restriction created date", example = "2024-01-01")
  val startDate: LocalDate? = null,

  @Schema(description = "Restriction end date ", example = "2024-01-01")
  val expiryDate: LocalDate? = null,

  @Schema(description = "Comments for the restriction ", example = "N/A")
  val comments: String? = null,

  @Schema(description = "The username of either the person who created the restriction or the last person to update it if it has been modified.", example = "admin")
  val enteredByUsername: String,

  @Schema(description = "The display name of either the person who created the restriction or the last person to update it if it has been modified.", example = "John Smith")
  val enteredByDisplayName: String,

  @Schema(description = "User who created the entry", example = "admin")
  val createdBy: String,

  @Schema(description = "Timestamp when the entry was created", example = "2023-09-23T10:15:30")
  val createdTime: LocalDateTime,

  @Schema(description = "User who updated the entry", example = "admin2")
  val updatedBy: String?,

  @Schema(description = "Timestamp when the entry was updated", example = "2023-09-24T12:00:00")
  val updatedTime: LocalDateTime?,
)
