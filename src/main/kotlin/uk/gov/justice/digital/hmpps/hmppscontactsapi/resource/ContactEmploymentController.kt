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
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppscontactsapi.facade.EmploymentFacade
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.employment.CreateEmploymentRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.employment.PatchEmploymentsRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.employment.UpdateEmploymentRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.EmploymentDetails
import uk.gov.justice.digital.hmpps.hmppscontactsapi.swagger.AuthApiResponses
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.net.URI

@Tag(name = "Contacts")
@RestController
@RequestMapping(value = ["contact/{contactId}/employment"], produces = [MediaType.APPLICATION_JSON_VALUE])
@AuthApiResponses
class ContactEmploymentController(
  private val employmentFacade: EmploymentFacade,
) {

  @PatchMapping
  @Operation(
    summary = "Patch employments",
    description = "Allows several updates to employments in one go. Includes creating new employments, updating existing employments and removing existing employments.",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "The changes were applied successfully. Returns full list of employments after update.",
        content = [
          Content(
            mediaType = "application/json",
            array = ArraySchema(schema = Schema(implementation = EmploymentDetails::class)),
          ),
        ],
      ),
      ApiResponse(

        responseCode = "400",
        description = "Invalid request",
        content = [
          Content(schema = Schema(implementation = ErrorResponse::class)),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "No contact with that id could be found or employments for update or delete could not be found or an organisation could not be found.",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('ROLE_CONTACTS_ADMIN', 'ROLE_CONTACTS__RW')")
  fun patchEmployment(
    @PathVariable("contactId") @Parameter(
      name = "contactId",
      description = "The id of the contact",
      example = "123456",
    ) contactId: Long,
    @Valid @RequestBody request: PatchEmploymentsRequest,
  ): List<EmploymentDetails> = employmentFacade.patchEmployments(contactId, request)

  @PostMapping
  @Operation(
    summary = "Create employment",
    description = "Create a single employment for a contact",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "201",
        description = "The employment was created successfully",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = EmploymentDetails::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "400",
        description = "Invalid request",
        content = [
          Content(schema = Schema(implementation = ErrorResponse::class)),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "No contact with that id could be found.",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('ROLE_CONTACTS_ADMIN', 'ROLE_CONTACTS__RW')")
  fun createEmployment(
    @PathVariable("contactId") @Parameter(
      name = "contactId",
      description = "The id of the contact",
      example = "123456",
    ) contactId: Long,
    @Valid @RequestBody request: CreateEmploymentRequest,
  ): ResponseEntity<EmploymentDetails> {
    val createdEmployment = employmentFacade.createEmployment(contactId, request)
    return ResponseEntity
      .created(URI.create("/contact/$contactId/employment/${createdEmployment.employmentId}"))
      .body(createdEmployment)
  }

  @PutMapping("/{employmentId}")
  @Operation(
    summary = "Update employment",
    description = "Update a single employment for a contact",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "The employment was updated successfully",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = EmploymentDetails::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "400",
        description = "Invalid request",
        content = [
          Content(schema = Schema(implementation = ErrorResponse::class)),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "No contact or employment with that id could be found.",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('ROLE_CONTACTS_ADMIN', 'ROLE_CONTACTS__RW')")
  fun updateEmployment(
    @PathVariable("contactId") @Parameter(
      name = "contactId",
      description = "The id of the contact",
      example = "123456",
    ) contactId: Long,
    @PathVariable("employmentId") @Parameter(
      name = "employmentId",
      description = "The id of the employment",
      example = "123456",
    ) employmentId: Long,
    @Valid @RequestBody request: UpdateEmploymentRequest,
  ) = employmentFacade.updateEmployment(contactId, employmentId, request)

  @DeleteMapping("/{employmentId}")
  @Operation(
    summary = "Delete employment",
    description = "Delete a single employment for a contact",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "204",
        description = "The employment was deleted successfully",
      ),
      ApiResponse(
        responseCode = "404",
        description = "No contact or employment with that id could be found.",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('ROLE_CONTACTS_ADMIN', 'ROLE_CONTACTS__RW')")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  fun deleteEmployment(
    @PathVariable("contactId") @Parameter(
      name = "contactId",
      description = "The id of the contact",
      example = "123456",
    ) contactId: Long,
    @PathVariable("employmentId") @Parameter(
      name = "employmentId",
      description = "The id of the employment",
      example = "123456",
    ) employmentId: Long,
  ) {
    employmentFacade.deleteEmployment(contactId, employmentId)
  }

  @GetMapping("/{employmentId}")
  @Operation(
    summary = "Get an employment",
    description = "Get a single employment by id",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "The employment was found",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = EmploymentDetails::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "No contact or employment with that id could be found.",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('ROLE_CONTACTS_ADMIN', 'ROLE_CONTACTS__R', 'ROLE_CONTACTS__RW')")
  fun getEmployment(
    @PathVariable("contactId") @Parameter(
      name = "contactId",
      description = "The id of the contact",
      example = "123456",
    ) contactId: Long,
    @PathVariable("employmentId") @Parameter(
      name = "employmentId",
      description = "The id of the employment",
      example = "123456",
    ) employmentId: Long,
  ) = employmentFacade.getEmployment(contactId, employmentId)
}
