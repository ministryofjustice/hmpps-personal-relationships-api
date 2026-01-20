package uk.gov.justice.digital.hmpps.personalrelationships.model.response

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "A phone number related to a contact with descriptions of all reference data")
data class ContactPhoneDetails(
  @Schema(description = "Unique identifier for the contact phone", example = "1")
  val contactPhoneId: Long,

  @Schema(description = "Unique identifier for the contact", example = "123")
  val contactId: Long,

  @Schema(description = "Type of phone", example = "MOB")
  val phoneType: String,

  @Schema(description = "Description of the type of phone", example = "Mobile")
  val phoneTypeDescription: String,

  @Schema(description = "Phone number", example = "+1234567890")
  val phoneNumber: String,

  @Schema(description = "Extension number", example = "123")
  val extNumber: String?,

  @Schema(description = "User who created the entry", example = "admin")
  val createdBy: String,

  @Schema(description = "Timestamp when the entry was created", example = "2023-09-23T10:15:30")
  val createdTime: LocalDateTime,

  @Schema(description = "User who updated the entry", example = "admin2")
  val updatedBy: String?,

  @Schema(description = "Timestamp when the entry was updated", example = "2023-09-24T12:00:00")
  val updatedTime: LocalDateTime?,
)
