package uk.gov.justice.digital.hmpps.personalrelationships.model.response

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.LocalDateTime

@Schema(description = "Prisoner restriction details response")
data class PrisonerRestrictionDetails(
  @Schema(description = "The internal ID for the prisoner restriction", example = "12345", required = false)
  val prisonerRestrictionId: Long,

  @Schema(description = "The prisoner number", example = "A1234BC", required = false)
  val prisonerNumber: String,

  @Schema(
    description =
    """
    The coded type of restriction that applies to this contact.
    This is a coded value from the group RESTRICTION in reference codes.
    Example values include ACC, BAN, CHILD, CLOSED, RESTRICTED, DIHCON, NONCON.
    """,
    example = "BAN",
  )
  val restrictionType: String,

  @Schema(description = "The description of restrictionType", example = "Banned")
  val restrictionTypeDescription: String,

  @Schema(description = "Effective date of the restriction", example = "2024-06-11", required = false)
  val effectiveDate: LocalDate,

  @Schema(description = "Expiry date of the restriction", example = "2024-12-31", nullable = true, required = false)
  val expiryDate: LocalDate? = null,

  @Schema(description = "Comment text", example = "No visits allowed", required = false)
  val commentText: String? = null,

  @Schema(description = "Authorised staff user name", example = "JSMITH", required = false)
  val authorisedUsername: String,

  @Schema(description = "The display name of either the person who authorised the restriction.", example = "John Smith")
  val authorisedByDisplayName: String,

  @Schema(description = "True if this restriction applies to the latest or current term in prison, false if a previous term", example = "true", required = false)
  val currentTerm: Boolean,

  @Schema(description = "Username of the person who created the record", example = "JSMITH_ADM", nullable = true, required = false)
  val createdBy: String,

  @Schema(description = "Timestamp when the record was created", nullable = true, required = false)
  val createdTime: LocalDateTime,

  @Schema(description = "Username of the person who last updated the record", example = "JDOE_ADM", nullable = true, required = false)
  val updatedBy: String? = null,

  @Schema(description = "Timestamp when the record was last updated", nullable = true, required = false)
  val updatedTime: LocalDateTime? = null,
)
