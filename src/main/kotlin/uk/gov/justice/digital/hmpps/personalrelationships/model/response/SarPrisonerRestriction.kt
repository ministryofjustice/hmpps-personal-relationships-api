package uk.gov.justice.digital.hmpps.personalrelationships.model.response

import uk.gov.justice.digital.hmpps.personalrelationships.entity.PrisonerRestriction
import java.time.LocalDate
import java.time.LocalDateTime

data class SubjectAccessRequestData(
  val prisonerNumber: String,
  val fromDate: LocalDate,
  val toDate: LocalDate,
  val prisonerRestrictions: List<SarPrisonerRestriction>,
)

data class SarPrisonerRestriction(
  val prisonerRestrictionId: Long,
  val restrictionType: String,
  val effectiveDate: LocalDate,
  val expiryDate: LocalDate?,
  val commentText: String?,
  val authorisedUsername: String,
  val currentTerm: Boolean,
  val createdBy: String,
  val createdTime: LocalDateTime,
  val updatedBy: String?,
  val updatedTime: LocalDateTime?,
) {
  constructor(entity: PrisonerRestriction) : this(
    entity.prisonerRestrictionId,
    entity.restrictionType,
    entity.effectiveDate,
    entity.expiryDate,
    entity.commentText,
    entity.authorisedUsername,
    entity.currentTerm,
    entity.createdBy,
    entity.createdTime,
    entity.updatedBy,
    entity.updatedTime,
  )
}
