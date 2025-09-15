package uk.gov.justice.digital.hmpps.hmppscontactsapi.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "v_prisoner_restriction_details")
data class PrisonerRestrictionDetailsEntity(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val prisonerRestrictionId: Long,

  val prisonerNumber: String,

  val restrictionType: String,

  val restrictionTypeDescription: String,

  val effectiveDate: LocalDate,

  val expiryDate: LocalDate?,

  val commentText: String?,

  val currentTerm: Boolean,

  val authorisedUsername: String,

  @Column(updatable = false)
  val createdBy: String,

  @Column(updatable = false)
  val createdTime: LocalDateTime,

  val updatedBy: String?,

  val updatedTime: LocalDateTime?,
)
