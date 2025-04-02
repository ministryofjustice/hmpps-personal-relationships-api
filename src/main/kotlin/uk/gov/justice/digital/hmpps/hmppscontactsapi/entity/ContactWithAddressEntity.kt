package uk.gov.justice.digital.hmpps.hmppscontactsapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "v_contacts_with_primary_address")
data class ContactWithAddressEntity(

  @Id
  val contactId: Long,

  val title: String?,

  val firstName: String,

  val lastName: String,

  val middleNames: String?,

  val dateOfBirth: LocalDate?,

  val deceasedDate: LocalDate?,

  val createdBy: String,

  val createdTime: LocalDateTime,

  var contactAddressId: Long? = null,

  val addressType: String? = null,

  val primaryAddress: Boolean? = false,

  val flat: String? = null,

  val property: String? = null,

  val street: String? = null,

  val area: String? = null,

  val cityCode: String? = null,

  val cityDescription: String? = null,

  val countyCode: String? = null,

  val countyDescription: String? = null,

  val postCode: String? = null,

  val countryCode: String? = null,

  val countryDescription: String? = null,

  val verified: Boolean? = false,

  val verifiedBy: String? = null,

  val verifiedTime: LocalDateTime? = null,

  val mailFlag: Boolean? = false,

  val startDate: LocalDate? = null,

  val endDate: LocalDate? = null,

  val noFixedAddress: Boolean? = false,

  val comments: String? = null,

  val updatedBy: String? = null,

  val updatedTime: Instant? = null,
)
