package uk.gov.justice.digital.hmpps.personalrelationships.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "contact_identity")
data class ContactIdentityEntity(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val contactIdentityId: Long,

  val contactId: Long,

  val identityType: String,

  val identityValue: String,

  val issuingAuthority: String? = null,

  @Column(updatable = false)
  val createdBy: String,

  @Column(updatable = false)
  val createdTime: LocalDateTime,

  val updatedBy: String? = null,

  val updatedTime: LocalDateTime? = null,
)
