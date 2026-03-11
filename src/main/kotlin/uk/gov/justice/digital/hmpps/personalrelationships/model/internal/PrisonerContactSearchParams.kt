package uk.gov.justice.digital.hmpps.personalrelationships.model.internal

import org.springframework.data.domain.Pageable

data class PrisonerContactSearchParams(
  val prisonerNumber: String,
  val approvedVisitor: Boolean?,
  val active: Boolean?,
  val relationshipType: String?,
  val emergencyContact: Boolean?,
  val nextOfKin: Boolean?,
  val emergencyContactOrNextOfKin: Boolean?,
  val pageable: Pageable,
)
