package uk.gov.justice.digital.hmpps.hmppscontactsapi.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestAttribute
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppscontactsapi.config.User
import uk.gov.justice.digital.hmpps.hmppscontactsapi.facade.ContactGlobalRestrictionsFacade
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.restrictions.CreateContactRestrictionRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.restrictions.UpdateContactRestrictionRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.ContactRestrictionDetails
import uk.gov.justice.digital.hmpps.hmppscontactsapi.swagger.AuthApiResponses
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

@Tag(name = "Restrictions")
@RestController
@RequestMapping(value = ["contact/{contactId}/restriction"], produces = [MediaType.APPLICATION_JSON_VALUE])
@AuthApiResponses
class ContactGlobalRestrictionController(
  val restrictionsFacade: ContactGlobalRestrictionsFacade,
) {

  @GetMapping
  @Operation(
    summary = "Get a contacts global restrictions",
    description = """
      Get a contacts global restrictions only. Global restrictions apply to all of a contacts relationships and are known as estate-wide restrictions in NOMIS.

      Additional restrictions between the contact and specific prisoners may also apply.
      """,
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Found the contact and their restrictions",
        content = [
          Content(
            mediaType = "application/json",
            array = ArraySchema(schema = Schema(implementation = ContactRestrictionDetails::class)),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "No contact with that id could be found",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('ROLE_CONTACTS_ADMIN', 'ROLE_CONTACTS__R', 'ROLE_CONTACTS__RW')")
  fun getContactGlobalRestrictions(
    @PathVariable("contactId") @Parameter(
      name = "contactId",
      description = "The id of the contact",
      example = "123456",
    ) contactId: Long,
  ): List<ContactRestrictionDetails> = restrictionsFacade.getGlobalRestrictionsForContact(contactId)

  @PostMapping(consumes = [MediaType.APPLICATION_JSON_VALUE])
  @Operation(
    summary = "Create new global restriction",
    description = "Creates a new global (estate-wide) restriction for the specified contact",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "201",
        description = "Created the global restriction successfully",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ContactRestrictionDetails::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "400",
        description = "The request has invalid or missing fields",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Could not find the the contact this global restriction is for",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('ROLE_CONTACTS_ADMIN','ROLE_CONTACTS__RW')")
  fun createContactGlobalRestriction(
    @PathVariable("contactId") @Parameter(
      name = "contactId",
      description = "The id of the contact",
      example = "123456",
    ) contactId: Long,
    @Valid @RequestBody request: CreateContactRestrictionRequest,
    @RequestAttribute user: User,
  ): ResponseEntity<Any> {
    val created = restrictionsFacade.createContactGlobalRestriction(contactId, request, user)
    return ResponseEntity
      .status(HttpStatus.CREATED)
      .body(created)
  }

  @PutMapping("/{contactRestrictionId}", consumes = [MediaType.APPLICATION_JSON_VALUE])
  @Operation(
    summary = "Update global restriction for a contact",
    description = "Updates a global (estate-wide) restriction for the specified contact and restriction id",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Updated the global restriction successfully",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ContactRestrictionDetails::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "400",
        description = "The request has invalid or missing fields",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Could not find the the contact or global restriction",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('ROLE_CONTACTS_ADMIN', 'ROLE_CONTACTS__RW')")
  fun updateContactGlobalRestriction(
    @PathVariable("contactId") @Parameter(
      name = "contactId",
      description = "The id of the contact",
      example = "123456",
    ) contactId: Long,
    @PathVariable("contactRestrictionId") @Parameter(
      name = "contactRestrictionId",
      description = "The id of the global restriction",
      example = "123456",
    ) contactRestrictionId: Long,
    @Valid @RequestBody request: UpdateContactRestrictionRequest,
    @RequestAttribute user: User,
  ): ContactRestrictionDetails = restrictionsFacade.updateContactGlobalRestriction(contactId, contactRestrictionId, request, user)
}
