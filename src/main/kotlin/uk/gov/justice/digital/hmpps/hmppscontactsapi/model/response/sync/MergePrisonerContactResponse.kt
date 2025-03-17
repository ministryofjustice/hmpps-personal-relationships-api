package uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.sync

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.migrate.IdPair

@Schema(description = "The response object for a prisoner merge request")
data class MergePrisonerContactResponse(
  val relationshipsCreated: List<PrisonerContactAndRestrictionIds>,
  val relationshipsRemoved: List<PrisonerRelationshipIds>,
)

@Schema(description = "Contains the details of the relationships and restrictions removed during a prisoner merge")
data class PrisonerRelationshipIds(

  @Schema(description = "The prisoner number in NOMIS", example = "A1234AA")
  val prisonerNumber: String,

  @Schema(description = "The ID of the contact this relationship is with", example = "12345")
  val contactId: Long,

  @Schema(description = "The ID of relationship", example = "12345")
  val prisonerContactId: Long,

  @Schema(description = "A list of relationship restriction IDs", example = "[1234, 2345, 3456]")
  val prisonerContactRestrictionIds: List<Long> = emptyList(),
)

@Schema(description = "Contains the IDs of the contact, relationships and restrictions created during a prisoner merge")
data class PrisonerContactAndRestrictionIds(
  @Schema(description = "The contactId that this relationship is with")
  val contactId: Long,

  @Schema(description = "The unique IDs in NOMIS and DPS for this relationship or prisoner contact")
  val relationship: IdPair,

  @Schema(description = "The pairs of IDs in NOMIS and DPS for relationship-specific restrictions")
  val restrictions: List<IdPair>,
)
