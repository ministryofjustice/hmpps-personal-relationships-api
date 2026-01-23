package uk.gov.justice.digital.hmpps.personalrelationships.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "v_contact_phone_numbers")
data class ContactPhoneDetailsEntity(
  @Id
  val contactPhoneId: Long,

  val contactId: Long,

  val phoneType: String,

  val phoneTypeDescription: String,

  val phoneNumber: String,

  val extNumber: String?,

  val createdBy: String,

  val createdTime: LocalDateTime,

  val updatedBy: String?,

  val updatedTime: LocalDateTime?,
)
