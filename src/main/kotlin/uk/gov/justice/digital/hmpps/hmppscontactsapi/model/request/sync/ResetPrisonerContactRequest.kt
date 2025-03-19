package uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "The request to remove and replace relationships and relationship restrictions for a prisoner")
data class ResetPrisonerContactRequest(

  @Schema(description = "The prisoner number to reset relationships for")
  val prisonerNumber: String,

  @Schema(description = "The list of relationships to create in place of the existing")
  val prisonerContacts: List<SyncPrisonerRelationship>,
)
