package uk.gov.justice.digital.hmpps.personalrelationships.model.request.restrictions

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size
import java.time.LocalDate

@Schema(description = "Request to create a new restriction on a prisoner")
data class CreatePrisonerRestrictionRequest(
  @Schema(
    description =
    """
    The coded type of restriction that applies to this prisoner.
    This is a coded value from the group RESTRICTION in reference codes.
    """,
    example = "BAN",
  )
  @field:Size(max = 12, message = "restrictionType must be <= 12 characters")
  val restrictionType: String,

  @Schema(description = "Restriction effective date", example = "2024-01-01")
  val effectiveDate: LocalDate,

  @Schema(description = "Restriction expiry date", example = "2024-12-31", nullable = true)
  val expiryDate: LocalDate?,

  @Schema(description = "Comments for the restriction", example = "No visits allowed", nullable = true)
  @field:Size(max = 240, message = "commentText must be <= 240 characters")
  val commentText: String?,
)
