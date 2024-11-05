package uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.EstimatedIsOverEighteen
import java.time.LocalDate

@Schema(description = "Describes the details of a prisoner's contact")
data class PrisonerContactSummary(

  @Schema(description = "The unique identifier for the prisoner contact", example = "123456")
  val prisonerContactId: Long,

  @Schema(description = "The unique identifier for the contact", example = "654321")
  val contactId: Long,

  @Schema(description = "Prisoner number (NOMS ID)", example = "A1234BC")
  val prisonerNumber: String,

  @Schema(description = "The last name of the contact", example = "Doe")
  val lastName: String,

  @Schema(description = "The first name of the contact", example = "John")
  val firstName: String,

  @Schema(description = "The middle names of the contact, if any", example = "William", nullable = true)
  val middleNames: String? = null,

  @Schema(description = "The date of birth of the contact", example = "1980-01-01")
  val dateOfBirth: LocalDate?,

  @Schema(description = "YES if the contact is over 18 years old, NO if under, null if unknown", example = "YES")
  val estimatedIsOverEighteen: EstimatedIsOverEighteen?,

  @Schema(description = "The relationship code between the prisoner and the contact", example = "FRI")
  val relationshipCode: String,

  @Schema(description = "The description of the relationship", example = "Friend")
  val relationshipDescription: String,

  @Schema(description = "Flat number in the address, if any", example = "Flat 1", nullable = true)
  val flat: String?,

  @Schema(description = "Property name or number", example = "123")
  val property: String,

  @Schema(description = "Street name", example = "Baker Street")
  val street: String,

  @Schema(description = "Area or locality, if any", example = "Marylebone", nullable = true)
  val area: String?,

  @Schema(description = "City code", example = "25343")
  val cityCode: String,

  @Schema(description = "The description of city code", example = "Sheffield")
  val cityDescription: String,

  @Schema(description = "County code", example = "S.YORKSHIRE")
  val countyCode: String,

  @Schema(description = "The description of county code", example = "South Yorkshire")
  val countyDescription: String,

  @Schema(description = "Postal code", example = "NW1 6XE")
  val postCode: String,

  @Schema(description = "Country code", example = "ENG")
  val countryCode: String,

  @Schema(description = "The description of country code", example = "England")
  val countryDescription: String,

  @Schema(description = "Type of the latest phone number", example = "MOB", nullable = true)
  val phoneType: String?,

  @Schema(description = "Description of the type of the latest phone number", example = "Mobile", nullable = true)
  val phoneTypeDescription: String?,

  @Schema(description = "The latest phone number, if there are any", example = "+1234567890", nullable = true)
  val phoneNumber: String?,

  @Schema(description = "The extension number of the latest phone number", example = "123", nullable = true)
  val extNumber: String?,

  @Schema(description = "Indicates whether the contact is an approved visitor", example = "true")
  val approvedVisitor: Boolean,

  @Schema(description = "Is this contact the prisoner's next of kin?", example = "false")
  val nextOfKin: Boolean,

  @Schema(description = "Is this contact the prisoner's emergency contact?", example = "true")
  val emergencyContact: Boolean,

  @Schema(description = "Is this relationship active for the current booking?", example = "true")
  val currentTerm: Boolean,

  @Schema(description = "Any additional comments", example = "Close family friend", nullable = true)
  val comments: String?,
)
