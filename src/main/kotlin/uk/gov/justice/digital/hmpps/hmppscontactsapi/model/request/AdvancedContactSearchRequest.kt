package uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request

import java.time.LocalDate

data class AdvancedContactSearchRequest(
  val lastName: String,
  val firstName: String?,
  val middleNames: String?,
  val dateOfBirth: LocalDate?,
  val soundsLike: Boolean = false,
  val includeAnyExistingRelationshipsToPrisoner: String?,
)
