package uk.gov.justice.digital.hmpps.personalrelationships.model.request.restrictions

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@Schema(description = "Request to close a prisoner restriction by setting its expiry date")
data class ClosePrisonerRestrictionRequest(
  @Schema(
    description = "The date the restriction expires. Must not be in the past, or before the restriction's effective date. Use today's date to close the restriction immediately.",
    example = "2024-12-31",
  )
  val expiryDate: LocalDate,
)
