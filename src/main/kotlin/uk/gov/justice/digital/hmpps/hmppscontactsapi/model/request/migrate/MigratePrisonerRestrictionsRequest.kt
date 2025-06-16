package uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.migrate

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.Size
import java.time.LocalDate
import java.time.LocalDateTime

@Schema(description = "Request to migrate a prisoner's restrictions")
data class MigratePrisonerRestrictionsRequest(
  @Schema(description = "The prisoner number", example = "A1234BC")
  val prisonerNumber: String,

  @Schema(description = "Restriction records")
  @field:Valid
  @field:Size(min = 1, message = "restrictions must contain at least one record")
  val restrictions: List<PrisonerRestrictionDetailsRequest>,
)

@Schema(description = "Details of a prisoner restriction record")
data class PrisonerRestrictionDetailsRequest(
  @Schema(description = "The restriction type", example = "NO_VISIT")
  @field:Size(max = 12, message = "restrictionType must be less than or equal to 12 characters")
  val restrictionType: String?,

  @Schema(description = "Effective date of the restriction", example = "2024-06-11")
  val effectiveDate: LocalDate? = null,

  @Schema(description = "Expiry date of the restriction", example = "2024-12-31")
  val expiryDate: LocalDate? = null,

  @Schema(description = "Comment text", example = "No visits allowed")
  @field:Size(max = 240, message = "commentText must be less than or equal to 240 characters")
  val commentText: String? = null,

  @Schema(description = "Authorised staff id", example = "STAFF123")
  @field:Size(max = 10, message = "authorisedStaffId must be less than or equal to 10 characters")
  val authorisedStaffId: String? = null,

  @Schema(description = "Entered staff id", example = "STAFF456")
  @field:Size(max = 10, message = "enteredStaffId must be less than or equal to 10 characters")
  val enteredStaffId: String? = null,

  @Schema(description = "Username of the person who created the record", example = "JSMITH_ADM")
  val createdBy: String,

  @Schema(description = "Timestamp when the record was created")
  val createdTime: LocalDateTime,

  @Schema(description = "Username of the person who last updated the record", example = "JDOE_ADM")
  val updatedBy: String? = null,

  @Schema(description = "Timestamp when the record was last updated")
  val updatedTime: LocalDateTime? = null,
)
