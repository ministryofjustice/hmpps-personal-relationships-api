package uk.gov.justice.digital.hmpps.hmppscontactsapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "prisoner_restrictions")
data class PrisonerRestriction(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val prisonerRestrictionId: Long = 0,

  val prisonerNumber: String,

  val restrictionType: String? = null,

  val effectiveDate: LocalDate? = null,

  val expiryDate: LocalDate? = null,

  val commentText: String? = null,

  val authorisedStaffId: String? = null,

  val enteredStaffId: String? = null, // is a number ex 25810- different to createdBy ex EQH08G

  val createdBy: String,

  val createdTime: LocalDateTime,

  val updatedBy: String? = null,

  val updatedTime: LocalDateTime? = null,
)
