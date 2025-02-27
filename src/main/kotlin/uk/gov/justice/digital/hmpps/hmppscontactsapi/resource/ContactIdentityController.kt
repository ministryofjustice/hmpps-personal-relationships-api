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
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppscontactsapi.facade.ContactIdentityFacade
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.identity.CreateIdentityRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.identity.CreateMultipleIdentitiesRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.identity.UpdateIdentityRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.ContactIdentityDetails
import uk.gov.justice.digital.hmpps.hmppscontactsapi.swagger.AuthApiResponses
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

@Tag(name = "Contacts")
@RestController
@RequestMapping(value = ["contact/{contactId}"], produces = [MediaType.APPLICATION_JSON_VALUE])
@AuthApiResponses
class ContactIdentityController(private val contactIdentityFacade: ContactIdentityFacade) {

  @PostMapping("/identity", consumes = [MediaType.APPLICATION_JSON_VALUE])
  @Operation(
    summary = "Create new contact identity",
    description = "Creates a new identity for the specified contact",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "201",
        description = "Created the contact identity successfully",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ContactIdentityDetails::class),
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
        description = "Could not find the the contact this identity is for",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('ROLE_CONTACTS_ADMIN', 'ROLE_CONTACTS__RW')")
  fun createIdentityNumber(
    @PathVariable("contactId") @Parameter(
      name = "contactId",
      description = "The id of the contact",
      example = "123456",
    ) contactId: Long,
    @Valid @RequestBody request: CreateIdentityRequest,
  ): ResponseEntity<Any> {
    val created = contactIdentityFacade.create(contactId, request)
    return ResponseEntity
      .status(HttpStatus.CREATED)
      .body(created)
  }

  @PostMapping("/identities", consumes = [MediaType.APPLICATION_JSON_VALUE])
  @Operation(
    summary = "Create multiple contact identities",
    description = "Creates one or more new identities for the specified contact",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "201",
        description = "Created all identities successfully",
        content = [
          Content(
            mediaType = "application/json",
            array = ArraySchema(schema = Schema(implementation = ContactIdentityDetails::class)),
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
        description = "Could not find the the contact this identity is for",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('ROLE_CONTACTS_ADMIN', 'ROLE_CONTACTS__RW')")
  fun createMultipleIdentities(
    @PathVariable("contactId") @Parameter(
      name = "contactId",
      description = "The id of the contact",
      example = "123456",
    ) contactId: Long,
    @Valid @RequestBody request: CreateMultipleIdentitiesRequest,
  ): ResponseEntity<Any> {
    val created = contactIdentityFacade.createMultiple(contactId, request)
    return ResponseEntity
      .status(HttpStatus.CREATED)
      .body(created)
  }

  @PutMapping("/identity/{contactIdentityId}", consumes = [MediaType.APPLICATION_JSON_VALUE])
  @Operation(
    summary = "Update contact identity",
    description = "Updates an existing contact identity by id",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Updated the contact identity successfully",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ContactIdentityDetails::class),
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
        description = "Could not find the the contact or identity by their ids",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('ROLE_CONTACTS_ADMIN', 'ROLE_CONTACTS__RW')")
  fun updateIdentityNumber(
    @PathVariable("contactId") @Parameter(
      name = "contactId",
      description = "The id of the contact",
      example = "123456",
    ) contactId: Long,
    @PathVariable("contactIdentityId") @Parameter(
      name = "contactIdentityId",
      description = "The id of the contact identity",
      example = "987654",
    ) contactIdentityId: Long,
    @Valid @RequestBody request: UpdateIdentityRequest,
  ): ResponseEntity<Any> = ResponseEntity.ok(contactIdentityFacade.update(contactId, contactIdentityId, request))

  @GetMapping("/identity/{contactIdentityId}")
  @Operation(
    summary = "Get an identity",
    description = "Gets a contacts identity by id",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Found the identity successfully",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ContactIdentityDetails::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Could not find the the contact or identity this request is for",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('ROLE_CONTACTS_ADMIN', 'ROLE_CONTACTS__R', 'ROLE_CONTACTS__RW')")
  fun getIdentityNumber(
    @PathVariable("contactId") @Parameter(
      name = "contactId",
      description = "The id of the contact",
      example = "123456",
    ) contactId: Long,
    @PathVariable("contactIdentityId") @Parameter(
      name = "contactIdentityId",
      description = "The id of the contact identity",
      example = "987654",
    ) contactIdentityId: Long,
  ): ResponseEntity<Any> = ResponseEntity.ok(contactIdentityFacade.get(contactId, contactIdentityId))

  @DeleteMapping("/identity/{contactIdentityId}")
  @Operation(
    summary = "Delete contact identity",
    description = "Deletes an existing contact identity by id",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "204",
        description = "Deleted the contact identity successfully",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ContactIdentityDetails::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Could not find the the contact or identity by their ids",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('ROLE_CONTACTS_ADMIN', 'ROLE_CONTACTS__RW')")
  fun deleteIdentityNumber(
    @PathVariable("contactId") @Parameter(
      name = "contactId",
      description = "The id of the contact",
      example = "123456",
    ) contactId: Long,
    @PathVariable("contactIdentityId") @Parameter(
      name = "contactIdentityId",
      description = "The id of the contact identity",
      example = "987654",
    ) contactIdentityId: Long,
  ): ResponseEntity<Any> {
    contactIdentityFacade.delete(contactId, contactIdentityId)
    return ResponseEntity.noContent().build()
  }
}
