package uk.gov.justice.digital.hmpps.personalrelationships.model.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size

@Schema(description = "Request to create or update prisoner domestic status")
data class CreateOrUpdatePrisonerDomesticStatusRequest(

  @Schema(description = "The domestic status code for DOMESTIC_STS group code", example = "M")
  @field:Size(max = 12, message = "domesticStatusCode must be less than or equal to 12 characters")
  val domesticStatusCode: String?,
)
