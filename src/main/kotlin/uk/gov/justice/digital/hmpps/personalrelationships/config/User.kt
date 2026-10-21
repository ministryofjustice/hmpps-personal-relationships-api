package uk.gov.justice.digital.hmpps.personalrelationships.config

data class User(val username: String) {
  companion object {
    val SYS_USER = User("SYS")
    const val REQUEST_ATTRIBUTE = "user"
  }
}
