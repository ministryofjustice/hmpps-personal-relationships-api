package uk.gov.justice.digital.hmpps.personalrelationships.client.manage.users

data class UserDetails(
  val username: String,
  val name: String?,
  val activeCaseLoadId: String? = null,
)
