package uk.gov.justice.digital.hmpps.personalrelationships.client.prisonersearch

data class Prisoner(
  val prisonerNumber: String,
  val prisonId: String?,
  val prisonName: String?,
  val lastName: String,
  val firstName: String,
  val middleNames: String?,
)
