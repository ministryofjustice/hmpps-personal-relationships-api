package uk.gov.justice.digital.hmpps.hmppscontactsapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.envers.Audited
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalDateTime.now

/**
 * If making changes to this class they should be reflected in BaseContactEntity and ContactWithFixedIdEntity as well.
 *
 * Generates a new id using the identity column. If syncing from NOMIS you should use ContactWithFixedIdEntity
 * instead which will not use the sequence and instead provide a specific value from NOMIS person_id.
 *
 * DPS contact_id is >= 20000000 and NOMIS person_id is < 20000000.
 */
@Audited
@Entity
@Table(name = "contact")
data class ContactEntity(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val contactId: Long? = null,
  override val title: String?,
  override val firstName: String,
  override val lastName: String,
  override val middleNames: String?,
  override val dateOfBirth: LocalDate?,
  override val deceasedDate: LocalDate?,
  override val createdBy: String,
  override val createdTime: LocalDateTime = now(),
  override val staffFlag: Boolean = false,
  override val remitterFlag: Boolean = false,
  override val gender: String? = null,
  override val domesticStatus: String? = null,
  override val languageCode: String? = null,
  override val interpreterRequired: Boolean = false,
  override val updatedBy: String? = null,
  override val updatedTime: LocalDateTime? = null,
) : BaseContactEntity(
  title = title,
  firstName = firstName,
  lastName = lastName,
  middleNames = middleNames,
  dateOfBirth = dateOfBirth,
  deceasedDate = deceasedDate,
  createdBy = createdBy,
  createdTime = createdTime,
  staffFlag = staffFlag,
  remitterFlag = remitterFlag,
  gender = gender,
  domesticStatus = domesticStatus,
  languageCode = languageCode,
  interpreterRequired = interpreterRequired,
  updatedBy = updatedBy,
  updatedTime = updatedTime,
) {
  override fun id(): Long = requireNotNull(this.contactId) { "Contact id should be non-null once created" }
}
