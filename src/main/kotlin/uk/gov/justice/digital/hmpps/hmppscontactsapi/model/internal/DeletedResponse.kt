package uk.gov.justice.digital.hmpps.hmppscontactsapi.model.internal

data class DeletedResponse(
  val ids: DeletedRelationshipIds,
  val wasUpdated: Boolean,
)
