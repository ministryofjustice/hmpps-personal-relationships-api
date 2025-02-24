package uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size
import org.openapitools.jackson.nullable.JsonNullable
import java.time.LocalDate

@Schema(
  description = "Request to patch a new contact. " +
    "firstName and lastName are not updatable so are intentionally missing from this request.",
)
data class PatchContactRequest(

  @Schema(description = "Whether the contact is a staff member", example = "false", nullable = false, type = "boolean", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @get:JsonProperty(value = "isStaff")
  @set:JsonProperty(value = "isStaff")
  var isStaff: JsonNullable<Boolean> = JsonNullable.undefined(),

  @Schema(description = "The domestic status code of the contact", example = "S", nullable = true, type = "string", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  var domesticStatus: JsonNullable<String?> = JsonNullable.undefined(),

  @Schema(description = "Whether an interpreter is required", example = "false", nullable = false, type = "boolean", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  var interpreterRequired: JsonNullable<Boolean> = JsonNullable.undefined(),

  @Schema(description = "The language code of the contact", example = "EN", nullable = true, type = "string", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  var languageCode: JsonNullable<String?> = JsonNullable.undefined(),

  @Schema(description = "The date of birth of the contact, if known", example = "1980-01-01", type = "string", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  var dateOfBirth: JsonNullable<LocalDate?> = JsonNullable.undefined(),

  @Schema(description = "The title code for the contact, if any", type = "string", example = "MR", nullable = true, maxLength = 12, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @field:Size(max = 12, message = "title must be <= 12 characters")
  var title: JsonNullable<String> = JsonNullable.undefined(),

  @Schema(description = "The middle names of the contact, if any", type = "string", example = "William", nullable = true, maxLength = 35, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @field:Size(max = 35, message = "middleNames must be <= 35 characters")
  var middleNames: JsonNullable<String> = JsonNullable.undefined(),

  @Schema(
    description =
    """
    The optional gender code for the contact.
    This is a coded value (from the group code GENDER in reference data).
    Known values are (M) Male, (F) Female, (NK) Not Known, (NS) Not Specified.
    """,
    example = "M",
    nullable = true,
    type = "string",
    requiredMode = Schema.RequiredMode.NOT_REQUIRED,
  )
  var gender: JsonNullable<String?> = JsonNullable.undefined(),

  @Schema(description = "The date the contact deceased, if known", example = "1980-01-01", type = "string", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  var deceasedDate: JsonNullable<LocalDate?> = JsonNullable.undefined(),

  @Schema(description = "The id of the user who updated the contact", example = "JD000001", nullable = false, requiredMode = Schema.RequiredMode.REQUIRED)
  val updatedBy: String,
)
