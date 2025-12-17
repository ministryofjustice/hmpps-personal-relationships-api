package uk.gov.justice.digital.hmpps.hmppscontactsapi.entity

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.time.LocalDate

@Embeddable
data class ContactAuditPk(
  @Column(name = "contact_id")
  val contactId: Long,
  @Column(name = "rev_id")
  val revId: Long,
)

@Entity
@Table(name = "contact_audit")
data class ContactEntityAudit(
  @EmbeddedId
  val id: ContactAuditPk,

  @Column(name = "last_name")
  val lastName: String,

  @Column(name = "first_name")
  val firstName: String,

  @Column(name = "middle_names")
  val middleNames: String?,

  @Column(name = "date_of_birth")
  val dateOfBirth: LocalDate?,
)
