package uk.gov.justice.digital.hmpps.hmppscontactsapi.util

data class StubUser(
  val username: String,
  val displayName: String,
  val roles: List<String>,
  val activeCaseloadId: String? = null,
  // isSystemUser indicates a user that does not have a user_name claim, i.e. syscon
  val isSystemUser: Boolean = false,
) {
  companion object {
    val USER_WITH_NO_ROLES = StubUser("unauthorised", "Unauthorised", emptyList())
    val USER_WITH_WRONG_ROLES = StubUser("wrong_roles", "Wrong", listOf("ROLE_WRONG"))
    val SYNC_AND_MIGRATE_USER = StubUser("sys", "System", listOf("PERSONAL_RELATIONSHIPS_MIGRATION"), activeCaseloadId = null, isSystemUser = true)
    val READ_ONLY_USER = StubUser("read_only_user", "Read Only", listOf("ROLE_CONTACTS__R"), activeCaseloadId = "BXI")
    val READ_WRITE_USER = StubUser("read_write_user", "Read Write", listOf("ROLE_CONTACTS__RW"), activeCaseloadId = "BXI")
    val CREATING_USER = StubUser("created", "Created", listOf("ROLE_CONTACTS__RW"), activeCaseloadId = "BXI")
    val UPDATING_USER = StubUser("updated", "Updated", listOf("ROLE_CONTACTS__RW"), activeCaseloadId = "BXI")
    val DELETING_USER = StubUser("deleted", "Deleted", listOf("ROLE_CONTACTS__RW"), activeCaseloadId = "BXI")
  }
}
