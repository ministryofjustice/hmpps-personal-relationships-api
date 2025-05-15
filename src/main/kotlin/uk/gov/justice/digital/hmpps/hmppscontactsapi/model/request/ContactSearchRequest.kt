package uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Past
import jakarta.validation.constraints.Pattern
import org.springframework.format.annotation.DateTimeFormat
import java.time.LocalDate

private const val VALID_NAME_REGEX = "[a-zA-Z\\s,.'-]*"
private const val VALID_NAME_MESSAGE = "must be a letter or punctuation"

@Schema(description = "Contact Search Request")
data class ContactSearchRequest(

  @Schema(description = "Last name of the contact", example = "Jones", nullable = false)
  @field:NotBlank(message = "must not be blank")
  @field:Pattern(regexp = VALID_NAME_REGEX, message = VALID_NAME_MESSAGE)
  val lastName: String,

  @Schema(description = "First name of the contact", example = "Elton", nullable = true)
  @field:Pattern(regexp = VALID_NAME_REGEX, message = VALID_NAME_MESSAGE)
  val firstName: String?,

  @Schema(description = "Middle names of the contact", example = "Simon", nullable = true)
  @field:Pattern(regexp = VALID_NAME_REGEX, message = VALID_NAME_MESSAGE)
  val middleNames: String?,

  @Schema(description = "Date of Birth of the contact in ISO format", example = "30/12/2010", nullable = true, format = "dd/MM/yyyy")
  @field:Past(message = "The date of birth must be in the past")
  @field:DateTimeFormat(pattern = "dd/MM/yyyy")
  val dateOfBirth: LocalDate?,
)
