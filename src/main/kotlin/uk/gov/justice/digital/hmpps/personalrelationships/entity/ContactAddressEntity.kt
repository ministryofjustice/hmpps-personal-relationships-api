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
@Table(name = "contact_address")
data class ContactAddressEntity(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  var contactAddressId: Long,

  val contactId: Long? = null,

  val addressType: String? = null,

  val primaryAddress: Boolean = false,

  val flat: String? = null,

  val property: String? = null,

  val street: String? = null,

  val area: String? = null,

  val cityCode: String? = null,

  val countyCode: String? = null,

  val postCode: String? = null,

  val countryCode: String? = null,

  val verified: Boolean = false,

  val mailFlag: Boolean = false,

  val startDate: LocalDate? = null,

  val endDate: LocalDate? = null,

  val noFixedAddress: Boolean = false,

  val comments: String? = null,

  @Column(updatable = false)
  val createdBy: String,

  @Column(updatable = false)
  val createdTime: LocalDateTime,
) {
  var updatedBy: String? = null
  var updatedTime: LocalDateTime? = null
  var verifiedBy: String? = null
  var verifiedTime: LocalDateTime? = null

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as ContactAddressEntity

    return contactAddressId == other.contactAddressId
  }

  override fun hashCode(): Int = contactAddressId.hashCode()
}
