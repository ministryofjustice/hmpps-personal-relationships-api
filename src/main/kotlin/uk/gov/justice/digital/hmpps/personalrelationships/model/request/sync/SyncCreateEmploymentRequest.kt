package uk.gov.justice.digital.hmpps.personalrelationships.model.request.sync

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "Request object for creating a new employment record")
data class SyncCreateEmploymentRequest(

  @Schema(description = "The ID of the organization associated with the employment", example = "12345")
  val organisationId: Long,

  @Schema(description = "The ID of the contact associated with the employment", example = "67890")
  val contactId: Long,

  @Schema(description = "If the employment is active", example = "true")
  val active: Boolean,

  @Schema(description = "User who created the entry", example = "admin")
  val createdBy: String,

  @Schema(description = "Timestamp when the entry was created", example = "2023-09-23T10:15:30")
  val createdTime: LocalDateTime,
)
