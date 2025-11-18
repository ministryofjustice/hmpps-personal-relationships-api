package uk.gov.justice.digital.hmpps.hmppscontactsapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.envers.Audited
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalDateTime.now

/**
 * If making changes to this class they should be reflected in BaseContactEntity and ContactEntity as well.
 *
 * The id is manually specified using NOMIS person_id. If generating a new contact on DPS you should use ContactEntity
 * instead which will generate a new id automatically.
 *
 * DPS contact_id is >= 20000000 and NOMIS person_id is < 20000000.
 */
@Audited
@Entity
@Table(name = "contact")
data class ContactWithFixedIdEntity(
  @Id
  val contactId: Long,
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
  override fun id(): Long = contactId
}
