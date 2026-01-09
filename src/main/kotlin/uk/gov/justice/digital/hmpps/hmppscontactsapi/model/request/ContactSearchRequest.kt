package uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request

import java.time.LocalDate

data class ContactSearchRequest(
  val lastName: String,
  val firstName: String?,
  val middleNames: String?,
  val contactId: String?,
  val dateOfBirth: LocalDate?,
  val soundsLike: Boolean = false,
  val includeAnyExistingRelationshipsToPrisoner: String?,
)

data class ContactSearchRequestV2(
  val lastName: String? = null,
  val firstName: String? = null,
  val middleNames: String? = null,
  val dateOfBirth: LocalDate? = null,
  val searchType: UserSearchType,
  val previousNames: Boolean? = false,
  val contactId: Long? = null,
  val includePrisonerRelationships: String? = null,
)

enum class UserSearchType {
  EXACT,
  PARTIAL,
  SOUNDS_LIKE,
}
