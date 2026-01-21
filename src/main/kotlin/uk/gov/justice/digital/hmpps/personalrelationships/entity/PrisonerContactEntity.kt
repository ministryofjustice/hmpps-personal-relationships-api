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
@Table(name = "prisoner_contact")
data class PrisonerContactEntity(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val prisonerContactId: Long,

  val contactId: Long,

  val prisonerNumber: String,

  val relationshipType: String,

  val relationshipToPrisoner: String,

  val nextOfKin: Boolean,

  val emergencyContact: Boolean,

  val comments: String?,

  val active: Boolean,

  val approvedVisitor: Boolean,

  val currentTerm: Boolean,

  @Column(updatable = false)
  val createdBy: String,

  @Column(updatable = false)
  val createdTime: LocalDateTime,
) {
  var approvedBy: String? = null

  var approvedTime: LocalDateTime? = null

  var expiryDate: LocalDate? = null

  var createdAtPrison: String? = null

  var updatedBy: String? = null

  var updatedTime: LocalDateTime? = null
}
