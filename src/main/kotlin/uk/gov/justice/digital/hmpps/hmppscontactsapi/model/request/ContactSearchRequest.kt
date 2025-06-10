package uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request

import java.time.LocalDate

data class ContactSearchRequest(
  val lastName: String,
  val firstName: String?,
  val middleNames: String?,
  val dateOfBirth: LocalDate?,
  val includeAnyExistingRelationshipsToPrisoner: String?,
)
