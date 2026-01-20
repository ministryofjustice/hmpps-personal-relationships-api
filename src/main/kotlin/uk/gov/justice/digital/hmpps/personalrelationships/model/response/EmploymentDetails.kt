package uk.gov.justice.digital.hmpps.personalrelationships.model.response

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.personalrelationships.client.organisationsapi.model.OrganisationSummary
import java.time.LocalDateTime

@Schema(description = "The details of an employment for a contact including a summary of the employing organisation.")
data class EmploymentDetails(
  @Schema(description = "The id for this employment", example = "123456")
  val employmentId: Long,

  @Schema(description = "This id for this contact", example = "654321")
  val contactId: Long,

  @Schema(description = "A summary of the employing organisation")
  val employer: OrganisationSummary,

  @Schema(description = "Whether this is a current employment or not")
  val isActive: Boolean,

  @Schema(description = "User who created the entry", example = "admin")
  val createdBy: String,

  @Schema(description = "Timestamp when the entry was created", example = "2023-09-23T10:15:30")
  val createdTime: LocalDateTime,

  @Schema(description = "User who updated the entry", example = "admin2")
  val updatedBy: String?,

  @Schema(description = "Timestamp when the entry was updated", example = "2023-09-24T12:00:00")
  val updatedTime: LocalDateTime?,
)
