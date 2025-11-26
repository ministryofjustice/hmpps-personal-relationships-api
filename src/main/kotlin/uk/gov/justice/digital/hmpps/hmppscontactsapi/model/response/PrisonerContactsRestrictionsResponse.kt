package uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@Schema(description = "Restrictions related to specific relationships between prisoners and contacts")
data class PrisonerContactsRestrictionsResponse(
  @Schema(description = "The list of prisoner contact restrictions for each prisoner contact where found")
  val prisonerContactRestrictions: List<PrisonerContactRestrictions>,
)

data class PrisonerContactRestrictions(
  @Schema(description = "The unique identifier for the prisoner contact", example = "123456")
  val prisonerContactId: Long,

  @Schema(description = "Relationship specific restrictions for the prisoner contact")
  val prisonerContactRestrictions: List<PrisonerContactRestriction>,

  @Schema(description = "Global (estate-wide) restrictions for the prisoner contact")
  val globalContactRestrictions: List<GlobalContactRestriction>,
)

@Schema(description = "Restriction related to a prisoner and contacts relationship")
data class PrisonerContactRestriction(
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

  @Schema(description = "The description of restriction type", example = "Banned")
  val restrictionTypeDescription: String,

  @Schema(description = "The date the restriction starts", example = "2024-01-01")
  val startDate: LocalDate? = null,

  @Schema(description = "The date the restriction expires", example = "2024-01-01")
  val expiryDate: LocalDate? = null,

  @Schema(description = "Any comments for the restriction", example = "N/A")
  val comments: String? = null,

  @Schema(
    description = "The display name of either the person who created the restriction or the last person to update it if it has been modified.",
    example = "John Smith",
  )
  val enteredByDisplayName: String,
)

@Schema(description = "Global restriction related to a contact, a.k.a estate-wide restrictions")
data class GlobalContactRestriction(
  @Schema(description = "Unique identifier for the contact restriction", example = "1")
  val contactRestrictionId: Long,

  @Schema(description = "Unique identifier for the contact", example = "123")
  val contactId: Long,

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

  @Schema(description = "The date the restriction starts", example = "2024-01-01")
  val startDate: LocalDate? = null,

  @Schema(description = "The date the restriction expires", example = "2024-01-01")
  val expiryDate: LocalDate? = null,

  @Schema(description = "Any comments for the restriction", example = "N/A")
  val comments: String? = null,

  @Schema(
    description = "The display name of either the person who created the restriction or the last person to update it if it has been modified.",
    example = "John Smith",
  )
  val enteredByDisplayName: String,
)
