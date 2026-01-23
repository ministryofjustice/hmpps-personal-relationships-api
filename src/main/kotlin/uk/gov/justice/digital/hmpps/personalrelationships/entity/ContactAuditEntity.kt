package uk.gov.justice.digital.hmpps.personalrelationships.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.Table
import java.time.LocalDate
import java.time.LocalDateTime

data class AuditPrimaryKey(
  val contactId: Long = 0L,
  val revId: Long = 0L,
)

/**
 * Entity used in read-only queries for searching contacts by historical names.
 * Note:  The @Column annotations to describe the name are needed here as they clash with other
 * definitions for envers auditing.
 */

@Entity
@Table(name = "contact_audit")
@IdClass(AuditPrimaryKey::class)
data class ContactAuditEntity(
  @Id
  @Column(name = "contact_id")
  val contactId: Long,

  @Id
  @Column(name = "rev_id")
  val revId: Long,

  @Column(name = "rev_type")
  val revType: Int,

  @Column(name = "title")
  val title: String?,

  @Column(name = "first_name")
  val firstName: String,

  @Column(name = "last_name")
  val lastName: String,

  @Column(name = "middle_names")
  val middleNames: String?,

  @Column(name = "date_of_birth")
  val dateOfBirth: LocalDate?,

  @Column(name = "deceased_date")
  val deceasedDate: LocalDate?,

  @Column(name = "staff_flag")
  val staffFlag: Boolean = false,

  @Column(name = "remitter_flag")
  val remitterFlag: Boolean = false,

  @Column(name = "gender")
  val gender: String? = null,

  @Column(name = "domestic_status")
  val domesticStatus: String? = null,

  @Column(name = "language_code")
  val languageCode: String? = null,

  @Column(name = "interpreter_required")
  val interpreterRequired: Boolean = false,

  @Column(name = "created_by", updatable = false)
  val createdBy: String,

  @Column(name = "created_time", updatable = false)
  val createdTime: LocalDateTime,

  @Column(name = "updated_by")
  val updatedBy: String? = null,

  @Column(name = "updated_time")
  val updatedTime: LocalDateTime? = null,
) {
  // These are generated columns in the database - here only for JPQL queries on search
  @Column(name = "last_name_soundex", updatable = false, insertable = false)
  val lastNameSoundex: String? = null

  @Column(name = "first_name_soundex", updatable = false, insertable = false)
  val firstNameSoundex: String? = null

  @Column(name = "middle_names_soundex", updatable = false, insertable = false)
  val middleNamesSoundex: String? = null
}
