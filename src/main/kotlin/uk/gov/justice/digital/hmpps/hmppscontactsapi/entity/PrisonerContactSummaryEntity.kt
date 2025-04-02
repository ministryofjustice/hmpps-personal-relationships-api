package uk.gov.justice.digital.hmpps.hmppscontactsapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.annotation.Immutable
import java.time.LocalDate

@Entity
@Immutable
@Table(name = "v_prisoner_contacts")
data class PrisonerContactSummaryEntity(
  @Id
  val prisonerContactId: Long,

  val contactId: Long,

  val title: String?,

  val firstName: String,

  val middleNames: String?,

  val lastName: String,

  val dateOfBirth: LocalDate?,

  val deceasedDate: LocalDate?,

  val contactAddressId: Long?,

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

  val primaryAddress: Boolean?,

  val mailFlag: Boolean?,

  val contactPhoneId: Long?,

  val phoneType: String?,

  val phoneTypeDescription: String?,

  val phoneNumber: String?,

  val extNumber: String?,

  val contactEmailId: Long?,

  val emailAddress: String?,

  val prisonerNumber: String,

  val relationshipToPrisoner: String,

  val relationshipToPrisonerDescription: String?,

  val active: Boolean,

  val approvedVisitor: Boolean,

  val nextOfKin: Boolean,

  val emergencyContact: Boolean,

  val currentTerm: Boolean,

  val comments: String?,

  val relationshipType: String,

  val relationshipTypeDescription: String,
)
