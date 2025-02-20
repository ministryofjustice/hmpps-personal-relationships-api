package uk.gov.justice.digital.hmpps.hmppscontactsapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "prisoner_domestic_status")
data class PrisonerDomesticStatus(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val prisonerDomesticStatusId: Long = 0,

  val prisonerNumber: String,

  val domesticStatusCode: String?,

  val active: Boolean,

  val createdBy: String,

  val createdTime: LocalDateTime,

)
