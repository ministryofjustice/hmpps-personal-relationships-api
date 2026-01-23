package uk.gov.justice.digital.hmpps.personalrelationships.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "prisoner_contact_restriction")
data class PrisonerContactRestrictionEntity(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val prisonerContactRestrictionId: Long,

  val prisonerContactId: Long,

  val restrictionType: String,

  val startDate: LocalDate? = null,

  val expiryDate: LocalDate? = null,

  val comments: String? = null,

  @Column(updatable = false)
  val createdBy: String,

  @Column(updatable = false)
  val createdTime: LocalDateTime,

  val updatedBy: String? = null,

  val updatedTime: LocalDateTime? = null,
)
