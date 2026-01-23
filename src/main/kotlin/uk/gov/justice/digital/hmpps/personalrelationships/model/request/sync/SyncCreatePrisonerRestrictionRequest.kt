package uk.gov.justice.digital.hmpps.personalrelationships.model.request.sync

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.LocalDate
import java.time.LocalDateTime

@Schema(description = "Request to create a prisoner restriction")
data class SyncCreatePrisonerRestrictionRequest(

  @Schema(description = "The prisoner number", example = "A1234BC")
  @field:NotBlank
  val prisonerNumber: String,

  @Schema(description = "The restriction type", example = "NO_VISIT")
  @field:Size(max = 12, message = "restrictionType must be less than or equal to 12 characters")
  val restrictionType: String,

  @Schema(description = "Effective date of the restriction", example = "2024-06-11")
  val effectiveDate: LocalDate,

  @Schema(description = "Expiry date of the restriction", example = "2024-12-31")
  val expiryDate: LocalDate? = null,

  @Schema(description = "True if this restriction applies to the latest or current term in prison, false if a previous term", example = "true")
  val currentTerm: Boolean,

  @Schema(description = "Comment text", example = "No visits allowed")
  @field:Size(max = 240)
  val commentText: String? = null,

  @Schema(description = "Authorised staff user name", example = "JSMITH")
  @field:NotNull(message = "The NOMIS authorised staff user name must be present in the request")
  val authorisedUsername: String,

  @Schema(description = "Username of the person who created the record", example = "JSMITH_ADM")
  @field:NotBlank
  val createdBy: String,

  @Schema(description = "Timestamp when the record was created")
  val createdTime: LocalDateTime,

)
