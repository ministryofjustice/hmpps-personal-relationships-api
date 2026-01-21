package uk.gov.justice.digital.hmpps.personalrelationships.model.request.phone

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.Size

@Schema(description = "Request to create multiple phone numbers for a contact or an address")
data class CreateMultiplePhoneNumbersRequest(
  @Schema(description = "Phone numbers")
  @field:Valid
  @field:Size(min = 1, message = "phoneNumbers must have at least 1 item")
  val phoneNumbers: List<PhoneNumber>,
)
