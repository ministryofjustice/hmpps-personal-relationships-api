package uk.gov.justice.digital.hmpps.personalrelationships.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.persistence.EntityNotFoundException
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
import org.springframework.web.bind.annotation.RequestAttribute
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.personalrelationships.config.User
import uk.gov.justice.digital.hmpps.personalrelationships.facade.ContactPhoneFacade
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.phone.CreateMultiplePhoneNumbersRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.phone.CreatePhoneRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.phone.UpdatePhoneRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.ContactPhoneDetails
import uk.gov.justice.digital.hmpps.personalrelationships.swagger.AuthApiResponses
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

@Tag(name = "Contacts")
@RestController
@RequestMapping(value = ["contact/{contactId}"], produces = [MediaType.APPLICATION_JSON_VALUE])
@AuthApiResponses
class ContactPhoneController(private val contactPhoneFacade: ContactPhoneFacade) {

  @PostMapping("/phone", consumes = [MediaType.APPLICATION_JSON_VALUE])
  @Operation(
    summary = "Create new contact phone number",
    description = "Creates a new phone number for the specified contact",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "201",
        description = "Created the contact phone successfully",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ContactPhoneDetails::class),
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
        description = "Could not find the the contact this phone is for",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('ROLE_CONTACTS_ADMIN', 'ROLE_CONTACTS__RW')")
  fun createPhone(
    @PathVariable("contactId") @Parameter(
      name = "contactId",
      description = "The id of the contact",
      example = "123456",
    ) contactId: Long,
    @Valid @RequestBody request: CreatePhoneRequest,
    @RequestAttribute user: User,
  ): ResponseEntity<Any> {
    val createdPhone = contactPhoneFacade.create(contactId, request, user)
    return ResponseEntity
      .status(HttpStatus.CREATED)
      .body(createdPhone)
  }

  @PostMapping("/phones", consumes = [MediaType.APPLICATION_JSON_VALUE])
  @Operation(
    summary = "Create multiple new contact phone numbers",
    description = "Creates one or more phone numbers for the specified contact",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "201",
        description = "Created all the contact phone numbers successfully",
        content = [
          Content(
            mediaType = "application/json",
            array = ArraySchema(schema = Schema(implementation = ContactPhoneDetails::class)),
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
        description = "Could not find the the contact this phone is for",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('ROLE_CONTACTS_ADMIN', 'ROLE_CONTACTS__RW')")
  fun createMultipleContactPhoneNumber(
    @PathVariable("contactId") @Parameter(
      name = "contactId",
      description = "The id of the contact",
      example = "123456",
    ) contactId: Long,
    @Valid @RequestBody request: CreateMultiplePhoneNumbersRequest,
    @RequestAttribute user: User,
  ): ResponseEntity<Any> {
    val createdPhone = contactPhoneFacade.createMultiple(contactId, request, user)
    return ResponseEntity
      .status(HttpStatus.CREATED)
      .body(createdPhone)
  }

  @GetMapping("/phone/{contactPhoneId}")
  @Operation(
    summary = "Get a phone number",
    description = "Gets a contacts phone number by id",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Found the phone successfully",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ContactPhoneDetails::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Could not find the the contact or phone this request is for",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('ROLE_CONTACTS_ADMIN', 'ROLE_CONTACTS__R', 'ROLE_CONTACTS__RW')")
  fun getPhone(
    @PathVariable("contactId") @Parameter(
      name = "contactId",
      description = "The id of the contact",
      example = "123456",
    ) contactId: Long,
    @PathVariable("contactPhoneId") @Parameter(
      name = "contactPhoneId",
      description = "The id of the contact phone",
      example = "987654",
    ) contactPhoneId: Long,
  ): ResponseEntity<Any> = contactPhoneFacade.get(contactId, contactPhoneId)
    ?.let { ResponseEntity.ok(it) }
    ?: throw EntityNotFoundException("Contact phone with id ($contactPhoneId) not found for contact ($contactId)")

  @PutMapping("/phone/{contactPhoneId}", consumes = [MediaType.APPLICATION_JSON_VALUE])
  @Operation(
    summary = "Update contact phone number",
    description = "Updates an existing contact phone by id",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Updated the contact phone successfully",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ContactPhoneDetails::class),
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
        description = "Could not find the the contact or phone by their ids",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('ROLE_CONTACTS_ADMIN', 'ROLE_CONTACTS__RW')")
  fun updatePhone(
    @PathVariable("contactId") @Parameter(
      name = "contactId",
      description = "The id of the contact",
      example = "123456",
    ) contactId: Long,
    @PathVariable("contactPhoneId") @Parameter(
      name = "contactPhoneId",
      description = "The id of the contact phone",
      example = "987654",
    ) contactPhoneId: Long,
    @Valid @RequestBody request: UpdatePhoneRequest,
    @RequestAttribute user: User,
  ): ResponseEntity<Any> {
    val updatedPhone = contactPhoneFacade.update(contactId, contactPhoneId, request, user)
    return ResponseEntity.ok(updatedPhone)
  }

  @DeleteMapping("/phone/{contactPhoneId}")
  @Operation(
    summary = "Delete contact phone number",
    description = "Deletes an existing contact phone by id",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "204",
        description = "Deleted the contact phone successfully",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ContactPhoneDetails::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Could not find the the contact or phone by their ids",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('ROLE_CONTACTS_ADMIN', 'ROLE_CONTACTS__RW')")
  fun deletePhone(
    @PathVariable("contactId") @Parameter(
      name = "contactId",
      description = "The id of the contact",
      example = "123456",
    ) contactId: Long,
    @PathVariable("contactPhoneId") @Parameter(
      name = "contactPhoneId",
      description = "The id of the contact phone",
      example = "987654",
    ) contactPhoneId: Long,
    @RequestAttribute user: User,
  ): ResponseEntity<Any> {
    contactPhoneFacade.delete(contactId, contactPhoneId, user)
    return ResponseEntity.noContent().build()
  }
}
