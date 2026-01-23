package uk.gov.justice.digital.hmpps.personalrelationships.model.response

data class RelationshipsApprovedResponse(
  val relationships: List<RelationshipsApproved>,
)

data class RelationshipsApproved(
  val contactId: Long,
  val prisonerContactId: Long,
  val prisonerNumber: String,
  val approvedToVisit: Boolean,
)
