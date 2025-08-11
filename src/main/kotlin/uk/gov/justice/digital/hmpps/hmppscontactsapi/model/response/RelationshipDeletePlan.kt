package uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response

data class RelationshipDeletePlan(
  val willAlsoDeleteContactDob: Boolean,
  val hasRestrictions: Boolean,
)
