package uk.gov.justice.digital.hmpps.personalrelationships.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "v_prisoner_contact_restriction_details")
data class PrisonerContactRestrictionDetailsEntity(
  @Id
  val prisonerContactRestrictionId: Long,

  val prisonerContactId: Long,

  val restrictionType: String,

  val restrictionTypeDescription: String,

  val startDate: LocalDate?,

  val expiryDate: LocalDate?,

  val comments: String?,

  val createdBy: String,

  val createdTime: LocalDateTime,

  val updatedBy: String?,

  val updatedTime: LocalDateTime?,
)
