package uk.gov.justice.digital.hmpps.hmppscontactsapi.model.internal

import org.springframework.data.domain.Pageable

data class PrisonerContactSearchParams(
  val prisonerNumber: String,
  val active: Boolean?,
  val relationshipType: String?,
  val emergencyContact: Boolean?,
  val nextOfKin: Boolean?,
  val emergencyContactOrNextOfKin: Boolean?,
  val pageable: Pageable,
)
