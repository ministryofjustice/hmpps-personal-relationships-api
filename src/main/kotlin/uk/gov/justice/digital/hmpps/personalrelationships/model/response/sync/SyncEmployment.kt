package uk.gov.justice.digital.hmpps.personalrelationships.model.response.sync

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "The response of an employment record via sync")
data class SyncEmployment(

  @Schema(description = "The ID of the employment", example = "12345")
  val employmentId: Long,

  @Schema(description = "The ID of the organization associated with the employment", example = "12345")
  val organisationId: Long,

  @Schema(description = "The ID of the contact associated with the employment", example = "67890")
  val contactId: Long,

  @Schema(description = "If the employment is active", example = "true")
  val active: Boolean,

  @Schema(description = "User who created the employment record", example = "admin", nullable = true)
  val createdBy: String? = null,

  @Schema(description = "Time when the employment record was created", example = "2024-10-01T12:00:00Z", nullable = true)
  val createdTime: LocalDateTime? = null,

  @Schema(description = "User who last updated the employment record", example = "editor", nullable = true)
  val updatedBy: String? = null,

  @Schema(description = "Time when the employment record was last updated", example = "2024-10-02T15:30:00Z", nullable = true)
  val updatedTime: LocalDateTime? = null,
)
