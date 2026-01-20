package uk.gov.justice.digital.hmpps.personalrelationships.entity

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import uk.gov.justice.digital.hmpps.personalrelationships.model.ReferenceCodeGroup
import java.time.LocalDateTime

@Entity
@Table(name = "reference_codes")
data class ReferenceCodeEntity(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val referenceCodeId: Long = 0,

  @Enumerated(EnumType.STRING)
  val groupCode: ReferenceCodeGroup,

  val code: String,

  val description: String,

  val displayOrder: Int,

  val isActive: Boolean,

  val createdBy: String,

  val createdTime: LocalDateTime = LocalDateTime.now(),
)
