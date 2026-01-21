package uk.gov.justice.digital.hmpps.personalrelationships.model.request.sync

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "The request to update details of an employment record via sync")
data class SyncUpdateEmploymentRequest(

  @Schema(description = "The ID of the organization associated with the employment", example = "12345")
  val organisationId: Long,

  @Schema(description = "The ID of the contact associated with the employment", example = "67890")
  val contactId: Long,

  @Schema(description = "If the employment is active", example = "true")
  val active: Boolean,

  @Schema(description = "The username of the person who made the update", example = "JD000001")
  val updatedBy: String,

  @Schema(description = "The time when the update was made", example = "2024-01-01T00:00:00Z")
  val updatedTime: LocalDateTime,
)
