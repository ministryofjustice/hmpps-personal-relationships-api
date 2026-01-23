package uk.gov.justice.digital.hmpps.personalrelationships.entity

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "deleted_prisoner_contact")
data class DeletedPrisonerContactEntity(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val deletedPrisonerContactId: Long,

  val prisonerContactId: Long,

  val contactId: Long,

  val prisonerNumber: String,

  val relationshipType: String?,

  val relationshipToPrisoner: String?,

  val nextOfKin: Boolean?,

  val emergencyContact: Boolean?,

  val comments: String?,

  val active: Boolean?,

  val approvedVisitor: Boolean?,

  val currentTerm: Boolean?,

  val createdBy: String?,

  val createdTime: LocalDateTime?,

  val approvedBy: String?,

  val approvedTime: LocalDateTime?,

  val expiryDate: LocalDate?,

  val createdAtPrison: String?,

  val updatedBy: String?,

  val updatedTime: LocalDateTime?,

  val deletedBy: String,

  val deletedTime: LocalDateTime,
)
