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
  val lastNameSoundex: Boolean? = false,
  val lastNameHistorical: Boolean? = false,
  val firstName: String? = null,
  val firstNameSoundex: Boolean? = false,
  val middleNames: String? = null,
  val middleNamesSoundex: Boolean? = false,
  val contactId: Long? = null,
  val dateOfBirth: LocalDate? = null,
  val maxResults: Int? = 200,
  val sortOrder: String? = "lastName ASC",
  val includePrisonerRelationships: String? = null,
)
