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
@Table(name = "v_contact_restriction_details")
data class ContactRestrictionDetailsEntity(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val contactRestrictionId: Long,

  val contactId: Long,

  val restrictionType: String,

  val restrictionTypeDescription: String,

  val startDate: LocalDate?,

  val expiryDate: LocalDate?,

  val comments: String?,

  @Column(updatable = false)
  val createdBy: String,

  @Column(updatable = false)
  val createdTime: LocalDateTime,

  val updatedBy: String?,

  val updatedTime: LocalDateTime?,
)
