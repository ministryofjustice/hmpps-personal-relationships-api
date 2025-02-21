package uk.gov.justice.digital.hmpps.hmppscontactsapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "prisoner_number_of_children")
data class PrisonerNumberOfChildren(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val prisonerNumberOfChildrenId: Long = 0,

  val prisonerNumber: String,

  val numberOfChildren: String,

  val active: Boolean,

  val createdBy: String,

  val createdTime: LocalDateTime,

)
