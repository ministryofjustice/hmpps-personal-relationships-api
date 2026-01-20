package uk.gov.justice.digital.hmpps.personalrelationships.model.response

data class RestrictionsSummary(
  val active: Set<RestrictionTypeDetails>,
  val totalActive: Int,
  val totalExpired: Int,
) {
  companion object {
    val NO_RESTRICTIONS = RestrictionsSummary(emptySet(), 0, 0)
  }
}
