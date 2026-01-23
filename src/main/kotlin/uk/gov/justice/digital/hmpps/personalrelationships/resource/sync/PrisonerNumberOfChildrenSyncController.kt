package uk.gov.justice.digital.hmpps.personalrelationships.resource.sync

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
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.personalrelationships.facade.sync.PrisonerNumberOfChildrenSyncFacade
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.sync.SyncUpdatePrisonerNumberOfChildrenRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.sync.SyncPrisonerNumberOfChildrenResponse
import uk.gov.justice.digital.hmpps.personalrelationships.swagger.AuthApiResponses
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

@Tag(name = "Migrate and sync")
@RestController
@RequestMapping(value = ["/sync"], produces = [MediaType.APPLICATION_JSON_VALUE])
class PrisonerNumberOfChildrenSyncController(val prisonerNumberOfChildrenSyncFacade: PrisonerNumberOfChildrenSyncFacade) {
  @GetMapping(path = ["/{prisonerNumber}/number-of-children"], produces = [MediaType.APPLICATION_JSON_VALUE])
  @Operation(
    summary = "Returns the number of children for a prisoner by prisonerNumber",
    description = """
      Requires role: PERSONAL_RELATIONSHIPS_MIGRATION.
      Used to get the number of children for one prisoner.
      """,
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Found the number of children",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = SyncPrisonerNumberOfChildrenResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Could not find the number of children for this prisoner",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('PERSONAL_RELATIONSHIPS_MIGRATION')")
  fun syncGetNumberOfChildrenByPrisonerNumber(
    @PathVariable prisonerNumber: String,
  ): SyncPrisonerNumberOfChildrenResponse = prisonerNumberOfChildrenSyncFacade.getNumberOfChildrenByPrisonerNumber(prisonerNumber)

  /**
   * Creates a number of children record from NOMIS.
   * If a record already exists, it will be moved to history for auditability before creating the new record.
   *
   * Updates an existing number of children record from NOMIS.
   * The existing record will be moved to history for auditability before creating the new updated record.
   */

  @PutMapping(path = ["/{prisonerNumber}/number-of-children"], produces = [MediaType.APPLICATION_JSON_VALUE])
  @ResponseBody
  @Operation(
    summary = "Create or Updates the number of children for a prisoner",
    description = """
      Requires role: PERSONAL_RELATIONSHIPS_MIGRATION.
      Used to update a prisoner's number of children.
      """,
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Successfully created/updated Prisoner's number of children",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = SyncPrisonerNumberOfChildrenResponse::class),
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
  @AuthApiResponses
  @PreAuthorize("hasAnyRole('PERSONAL_RELATIONSHIPS_MIGRATION')")
  fun syncUpdateNumberOfChildren(
    @PathVariable prisonerNumber: String,
    @Valid @RequestBody request: SyncUpdatePrisonerNumberOfChildrenRequest,
  ): SyncPrisonerNumberOfChildrenResponse = prisonerNumberOfChildrenSyncFacade.createOrUpdateNumberOfChildren(prisonerNumber, request)
}
