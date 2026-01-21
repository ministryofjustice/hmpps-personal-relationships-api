package uk.gov.justice.digital.hmpps.personalrelationships.entity

import jakarta.persistence.Column
import jakarta.persistence.MappedSuperclass
import java.time.LocalDate
import java.time.LocalDateTime

@MappedSuperclass
abstract class BaseContactEntity(

  open val title: String?,

  open val firstName: String,

  open val lastName: String,

  open val middleNames: String?,

  open val dateOfBirth: LocalDate?,

  open val deceasedDate: LocalDate?,

  @Column(updatable = false)
  open val createdBy: String,

  @Column(updatable = false)
  open val createdTime: LocalDateTime,

  open val staffFlag: Boolean = false,

  open val remitterFlag: Boolean = false,

  open val gender: String? = null,

  open val domesticStatus: String? = null,

  open val languageCode: String? = null,

  open val interpreterRequired: Boolean = false,

  open val updatedBy: String? = null,

  open val updatedTime: LocalDateTime? = null,
) {
  abstract fun id(): Long
}
