package uk.gov.justice.digital.hmpps.personalrelationships.model.internal

data class DeletedResponse(
  val ids: DeletedRelationshipIds,
  val wasUpdated: Boolean,
)
