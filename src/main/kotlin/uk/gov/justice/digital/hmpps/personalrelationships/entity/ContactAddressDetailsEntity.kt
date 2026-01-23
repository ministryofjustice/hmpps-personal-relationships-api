package uk.gov.justice.digital.hmpps.personalrelationships.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "v_contact_addresses")
data class ContactAddressDetailsEntity(
  @Id
  val contactAddressId: Long,

  val contactId: Long,

  val addressType: String?,

  val addressTypeDescription: String?,

  val primaryAddress: Boolean,

  val flat: String?,

  val property: String?,

  val street: String?,

  val area: String?,

  val cityCode: String?,

  val cityDescription: String?,

  val countyCode: String?,

  val countyDescription: String?,

  val postCode: String?,

  val countryCode: String?,

  val countryDescription: String?,

  val verified: Boolean,

  val verifiedBy: String?,

  val verifiedTime: LocalDateTime?,

  val mailFlag: Boolean,

  val startDate: LocalDate?,

  val endDate: LocalDate?,

  val noFixedAddress: Boolean,

  val comments: String?,

  val createdBy: String,

  val createdTime: LocalDateTime,

  val updatedBy: String?,

  val updatedTime: LocalDateTime?,

)
