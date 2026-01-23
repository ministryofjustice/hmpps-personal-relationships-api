package uk.gov.justice.digital.hmpps.personalrelationships.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "v_prisoner_contact_count")
data class PrisonerContactRelationshipCountEntity(
  @Id
  val prisonerNumber: String,

  val social: Long,

  val official: Long,
)
