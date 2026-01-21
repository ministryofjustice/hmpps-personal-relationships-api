package uk.gov.justice.digital.hmpps.personalrelationships.entity

data class PrisonerContactRestrictionCountsId(
  val prisonerContactId: Long = 0,

  val restrictionType: String = "",

  val restrictionTypeDescription: String = "",

  val expired: Boolean = false,
)
