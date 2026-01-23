package uk.gov.justice.digital.hmpps.personalrelationships.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime
import java.time.LocalDateTime.now

@Entity
@Table(name = "contact_email")
data class ContactEmailEntity(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val contactEmailId: Long,

  val contactId: Long,

  val emailAddress: String,

  @Column(updatable = false)
  val createdBy: String,

  @Column(updatable = false)
  val createdTime: LocalDateTime = now(),

  val updatedBy: String? = null,

  val updatedTime: LocalDateTime? = null,
)
