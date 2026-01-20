package uk.gov.justice.digital.hmpps.personalrelationships.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestAttribute
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.personalrelationships.config.User
import uk.gov.justice.digital.hmpps.personalrelationships.facade.PrisonerNumberOfChildrenFacade
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.CreateOrUpdatePrisonerNumberOfChildrenRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.PrisonerNumberOfChildrenResponse
import uk.gov.justice.digital.hmpps.personalrelationships.swagger.AuthApiResponses
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

@Tag(name = "Prisoner Number Of Children")
@RestController
@RequestMapping(value = ["prisoner/{prisonerNumber}/number-of-children"], produces = [MediaType.APPLICATION_JSON_VALUE])
@AuthApiResponses
class PrisonerNumberOfChildrenController(
  private val prisonerNumberOfChildrenFacade: PrisonerNumberOfChildrenFacade,
) {

  @Operation(summary = "Get prisoner number of children")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Prisoner number of children retrieved successfully",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = PrisonerNumberOfChildrenResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Could not find the number of children for this prisoner",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  @GetMapping
  @PreAuthorize("hasAnyRole('ROLE_CONTACTS_ADMIN', 'ROLE_CONTACTS__R', 'ROLE_CONTACTS__RW')")
  fun getNumberOfChildren(
    @PathVariable prisonerNumber: String,
  ): PrisonerNumberOfChildrenResponse = prisonerNumberOfChildrenFacade.getNumberOfChildren(prisonerNumber)

  @Operation(summary = "Create or update prisoner number of children")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Prisoner number of children created/updated successfully",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = PrisonerNumberOfChildrenResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Prisoner not found",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "400",
        description = "Invalid input data",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PutMapping
  @PreAuthorize("hasAnyRole('ROLE_CONTACTS_ADMIN', 'ROLE_CONTACTS__RW')")
  fun createOrUpdateNumberOfChildren(
    @PathVariable prisonerNumber: String,
    @Valid @RequestBody request: CreateOrUpdatePrisonerNumberOfChildrenRequest,
    @RequestAttribute user: User,
  ): PrisonerNumberOfChildrenResponse = prisonerNumberOfChildrenFacade.createOrUpdateNumberOfChildren(prisonerNumber, request, user)
}
