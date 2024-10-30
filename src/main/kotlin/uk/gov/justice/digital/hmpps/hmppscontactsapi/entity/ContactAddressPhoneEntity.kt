package uk.gov.justice.digital.hmpps.hmppscontactsapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import java.time.LocalDateTime

@Entity
@Table(name = "contact_address_phone")
data class ContactAddressPhoneEntity(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val contactAddressPhoneId: Long,

  val contactId: Long,

  val contactAddressId: Long,

  val contactPhoneId: Long,

  val createdBy: String,

  @CreationTimestamp
  val createdTime: LocalDateTime = LocalDateTime.now(),

  val amendedBy: String? = null,

  val amendedTime: LocalDateTime? = null,
)
