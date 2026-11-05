package uk.gov.justice.digital.hmpps.personalrelationships.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
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
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestAttribute
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.personalrelationships.config.User
import uk.gov.justice.digital.hmpps.personalrelationships.facade.ManagePrisonerRestrictionsFacade
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.restrictions.CreatePrisonerRestrictionRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.restrictions.UpdatePrisonerRestrictionRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.PrisonerRestrictionDetails
import uk.gov.justice.digital.hmpps.personalrelationships.swagger.AuthApiResponses
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

@Tag(name = "Restrictions")
@RestController
@RequestMapping(value = ["prisoner-restrictions/{prisonerNumber}"], produces = [MediaType.APPLICATION_JSON_VALUE])
@AuthApiResponses
class ManagePrisonerRestrictionsController(
  private val facade: ManagePrisonerRestrictionsFacade,
) {

  @PostMapping(consumes = [MediaType.APPLICATION_JSON_VALUE])
  @Operation(
    summary = "Create a prisoner restriction",
    description = "Creates a restriction for the prisoner. A DPS-sourced domain event is published so the change is synchronised to NOMIS.",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "201",
        description = "Created the prisoner restriction",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = PrisonerRestrictionDetails::class))],
      ),
      ApiResponse(
        responseCode = "400",
        description = "The request has invalid or missing fields",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "The restriction type is not a known reference code",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('ROLE_CONTACTS_ADMIN', 'ROLE_CONTACTS__RW')")
  fun createPrisonerRestriction(
    @PathVariable("prisonerNumber") @Parameter(description = "The prisoner number", example = "A1234BC") prisonerNumber: String,
    @Valid @RequestBody request: CreatePrisonerRestrictionRequest,
    @RequestAttribute user: User,
  ): ResponseEntity<PrisonerRestrictionDetails> = ResponseEntity
    .status(HttpStatus.CREATED)
    .body(facade.createPrisonerRestriction(prisonerNumber, request, user))

  @PutMapping("/{prisonerRestrictionId}", consumes = [MediaType.APPLICATION_JSON_VALUE])
  @Operation(
    summary = "Update a prisoner restriction",
    description = "Replaces the editable fields of a prisoner restriction. A DPS-sourced domain event is published so the change is synchronised to NOMIS.",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Updated the prisoner restriction",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = PrisonerRestrictionDetails::class))],
      ),
      ApiResponse(
        responseCode = "400",
        description = "The request has invalid or missing fields",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "The restriction was not found for this prisoner, or the restriction type is unknown",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('ROLE_CONTACTS_ADMIN', 'ROLE_CONTACTS__RW')")
  fun updatePrisonerRestriction(
    @PathVariable("prisonerNumber") @Parameter(description = "The prisoner number", example = "A1234BC") prisonerNumber: String,
    @PathVariable("prisonerRestrictionId") @Parameter(description = "The prisoner restriction id", example = "123456") prisonerRestrictionId: Long,
    @Valid @RequestBody request: UpdatePrisonerRestrictionRequest,
    @RequestAttribute user: User,
  ): PrisonerRestrictionDetails = facade.updatePrisonerRestriction(prisonerNumber, prisonerRestrictionId, request, user)

  @DeleteMapping("/{prisonerRestrictionId}")
  @Operation(
    summary = "Delete a prisoner restriction",
    description = "Deletes a prisoner restriction. A DPS-sourced domain event is published so the change is synchronised to NOMIS.",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "204", description = "Deleted the prisoner restriction"),
      ApiResponse(
        responseCode = "404",
        description = "The restriction was not found for this prisoner",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('ROLE_CONTACTS_ADMIN', 'ROLE_CONTACTS__RW')")
  fun deletePrisonerRestriction(
    @PathVariable("prisonerNumber") @Parameter(description = "The prisoner number", example = "A1234BC") prisonerNumber: String,
    @PathVariable("prisonerRestrictionId") @Parameter(description = "The prisoner restriction id", example = "123456") prisonerRestrictionId: Long,
    @RequestAttribute user: User,
  ): ResponseEntity<Void> {
    facade.deletePrisonerRestriction(prisonerNumber, prisonerRestrictionId, user)
    return ResponseEntity.noContent().build()
  }
}
