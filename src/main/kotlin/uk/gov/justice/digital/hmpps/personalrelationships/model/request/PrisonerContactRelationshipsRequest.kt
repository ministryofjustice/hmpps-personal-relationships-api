package uk.gov.justice.digital.hmpps.personalrelationships.model.request

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Request to find relationships for prisoners and contacts")
data class PrisonerContactRelationshipsRequest(
  val identifiers: List<PrisonerAndContactId>,
)

data class PrisonerAndContactId(
  @Schema(description = "The prisoner number", example = "A1234AA")
  val prisonerNumber: String,

  @Schema(description = "The contact ID", example = "1234")
  val contactId: Long,
)
