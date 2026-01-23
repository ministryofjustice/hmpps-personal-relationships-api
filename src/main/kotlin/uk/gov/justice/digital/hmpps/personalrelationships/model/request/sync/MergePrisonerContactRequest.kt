package uk.gov.justice.digital.hmpps.personalrelationships.model.request.sync

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.migrate.AbstractAuditable
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.migrate.CodedValue
import java.time.LocalDate

@Schema(description = "The request to replace the relationships for a prisoner")
data class MergePrisonerContactRequest(

  @Schema(description = "The prisoner number that was retained in NOMIS")
  val retainedPrisonerNumber: String,

  @Schema(description = "The list of relationships to create")
  val prisonerContacts: List<SyncPrisonerRelationship>,

  @Schema(description = "The prisoner number that was removed from NOMIS")
  val removedPrisonerNumber: String,
)

@Schema(description = "A single prisoner relationship")
data class SyncPrisonerRelationship(

  @Schema(description = "The ID for this relationship in NOMIS", example = "123")
  val id: Long,

  @Schema(description = "The contactId which this relationship is with")
  val contactId: Long,

  @Schema(
    description = """
      Coded value indicating either a social or official contact (mandatory).
      This is a coded value (from the group code CONTACT_TYPE in reference data).
      Known values are (S) Social or (O) official.
    """,
    example = "S",
  )
  val contactType: CodedValue,

  @Schema(description = "Coded value indicating the type of relationship - from reference data")
  val relationshipType: CodedValue,

  @Schema(description = "True if this relationship applies to the latest or current term in prison, false if a previous term", example = "true")
  val currentTerm: Boolean,

  @Schema(description = "The relationship is active", example = "true")
  val active: Boolean,

  @Schema(description = "The date that this relationship expired", nullable = true, example = "2024-03-01")
  val expiryDate: LocalDate? = null,

  @Schema(description = "Approved visitor", example = "true")
  val approvedVisitor: Boolean,

  @Schema(description = "Next of kin", example = "true")
  val nextOfKin: Boolean,

  @Schema(description = "Emergency contact", example = "true")
  val emergencyContact: Boolean,

  @Schema(description = "Comment on this relationship", nullable = true, example = "This is an optional comment")
  val comment: String?,

  @Schema(description = "The prisoner number (NOMS ID) related", example = "A1234AA")
  val prisonerNumber: String,

  @Schema(description = "The restrictions for this prisoner contact relationship")
  val restrictions: List<SyncRelationshipRestriction> = emptyList(),
) : AbstractAuditable()

data class SyncRelationshipRestriction(
  @Schema(description = "The ID of this restriction in NOMIS", example = "123")
  val id: Long,

  @Schema(description = "Coded value indicating the restriction type from reference data")
  val restrictionType: CodedValue,

  @Schema(description = "Comment on this restriction", nullable = true, example = "Comment on restriction")
  val comment: String?,

  @Schema(description = "The date that this restriction took effect", example = "2024-03-01")
  val startDate: LocalDate,

  @Schema(description = "The date that this restriction expires", example = "2024-03-01")
  val expiryDate: LocalDate? = null,
) : AbstractAuditable()
