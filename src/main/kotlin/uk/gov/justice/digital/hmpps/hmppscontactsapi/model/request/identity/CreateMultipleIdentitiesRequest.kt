package uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.identity

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.Size

@Schema(description = "Request to create multiple contact identity documents")
data class CreateMultipleIdentitiesRequest(
  @Schema(description = "Identity documents")
  @field:Valid
  @field:Size(min = 1, message = "identities must have at least 1 item")
  val identities: List<IdentityDocument>,
)
