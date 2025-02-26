package uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.phone

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size

@Schema(description = "Request to update an address-specific phone number")
data class UpdateContactAddressPhoneRequest(
  @Schema(description = "Type of phone", example = "MOB")
  @field:Size(max = 12, message = "phoneType must be <= 12 characters")
  val phoneType: String,

  @Schema(description = "Phone number", example = "+1234567890")
  @field:Size(max = 40, message = "phoneNumber must be <= 40 characters")
  val phoneNumber: String,

  @Schema(description = "Extension number", example = "123")
  @field:Size(max = 7, message = "extNumber must be <= 7 characters")
  val extNumber: String? = null,

  @Schema(description = "The username of the person who made the update", example = "JD000001")
  @field:Size(max = 100, message = "updatedBy must be <= 100 characters")
  val updatedBy: String,
)
