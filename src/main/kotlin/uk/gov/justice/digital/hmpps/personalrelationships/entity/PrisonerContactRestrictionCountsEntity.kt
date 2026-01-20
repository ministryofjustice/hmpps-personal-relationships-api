package uk.gov.justice.digital.hmpps.personalrelationships.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.Table

@Entity
@Table(name = "v_restriction_counts")
@IdClass(PrisonerContactRestrictionCountsId::class)
data class PrisonerContactRestrictionCountsEntity(
  @Id
  val prisonerContactId: Long,

  @Id
  val restrictionType: String,

  @Id
  val restrictionTypeDescription: String,

  @Id
  val expired: Boolean,

  val numberOfRestrictions: Int,
)
