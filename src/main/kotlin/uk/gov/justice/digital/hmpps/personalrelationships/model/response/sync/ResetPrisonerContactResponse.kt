package uk.gov.justice.digital.hmpps.personalrelationships.model.response.sync

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "The response object for a reset of relationships for a prisoner")
data class ResetPrisonerContactResponse(
  val relationshipsCreated: List<PrisonerContactAndRestrictionIds>,
  val relationshipsRemoved: List<PrisonerRelationshipIds>,
)
