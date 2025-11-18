package uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.LocalDateTime

@Schema(description = "An audit history entry for a contact")
data class ContactAuditEntry(
  @Schema(description = "Revision id", example = "101")
  val revisionId: Long,

  @Schema(description = "Revision type", example = "ADD", allowableValues = ["ADD", "MOD", "DEL"])
  val revisionType: String,

  @Schema(description = "Revision timestamp")
  val revisionTimestamp: LocalDateTime,

  @Schema(description = "User who performed the change", example = "read_write_user", nullable = true)
  val username: String?,

  @Schema(description = "The id of the contact", example = "123456")
  val id: Long,

  @Schema(description = "Title code", nullable = true)
  val titleCode: String?,

  @Schema(description = "Last name")
  val lastName: String,

  @Schema(description = "First name")
  val firstName: String,

  @Schema(description = "Middle names", nullable = true)
  val middleNames: String?,

  @Schema(description = "Date of birth", nullable = true)
  val dateOfBirth: LocalDate?,

  @Schema(description = "Deceased date", nullable = true)
  val deceasedDate: LocalDate?,

  @Schema(description = "Staff flag")
  val isStaff: Boolean,

  @Schema(description = "Remitter flag")
  val isRemitter: Boolean,

  @Schema(description = "Gender code", nullable = true)
  val genderCode: String?,

  @Schema(description = "Domestic status code", nullable = true)
  val domesticStatusCode: String?,

  @Schema(description = "Language code", nullable = true)
  val languageCode: String?,

  @Schema(description = "Interpreter required")
  val interpreterRequired: Boolean,

  @Schema(description = "Created by")
  val createdBy: String,

  @Schema(description = "Created time")
  val createdTime: LocalDateTime,

  @Schema(description = "Updated by", nullable = true)
  val updatedBy: String?,

  @Schema(description = "Updated time", nullable = true)
  val updatedTime: LocalDateTime?,
)
