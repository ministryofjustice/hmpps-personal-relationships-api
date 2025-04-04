package uk.gov.justice.digital.hmpps.hmppscontactsapi.resource.sync

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
import uk.gov.justice.digital.hmpps.hmppscontactsapi.facade.sync.PrisonerDomesticStatusSyncFacade
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync.SyncUpdatePrisonerDomesticStatusRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.sync.SyncPrisonerDomesticStatusResponse
import uk.gov.justice.digital.hmpps.hmppscontactsapi.swagger.AuthApiResponses
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

@Tag(name = "Migrate and sync")
@RestController
@RequestMapping(value = ["/sync"], produces = [MediaType.APPLICATION_JSON_VALUE])
class PrisonerDomesticStatusSyncController(
  private val prisonerDomesticStatusSyncFacade: PrisonerDomesticStatusSyncFacade,
) {
  @GetMapping(path = ["/{prisonerNumber}/domestic-status"], produces = [MediaType.APPLICATION_JSON_VALUE])
  @Operation(
    summary = "Returns the domestic status for a prisoner by prisonerNumber",
    description = """
      Requires role: PERSONAL_RELATIONSHIPS_MIGRATION.
      Used to get the domestic status for one prisoner.
      """,
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Found the domestic status",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = SyncPrisonerDomesticStatusResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "No active domestic status found for the requested prisoner.",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('PERSONAL_RELATIONSHIPS_MIGRATION')")
  fun syncGetDomesticStatusByPrisonerNumber(
    @PathVariable prisonerNumber: String,
  ) = prisonerDomesticStatusSyncFacade.getDomesticStatusByPrisonerNumber(prisonerNumber)

  /**
   * Creates a domestic status record from NOMIS.
   * If a record already exists, it will be moved to history for auditability before creating the new record.
   *
   * Updates an existing domestic status record from NOMIS.
   * The existing record will be moved to history for auditability before creating the new updated record.
   */

  @PutMapping(path = ["/{prisonerNumber}/domestic-status"], produces = [MediaType.APPLICATION_JSON_VALUE])
  @ResponseBody
  @Operation(
    summary = "Create or Updates the domestic status for a prisoner",
    description = """
      Requires role: PERSONAL_RELATIONSHIPS_MIGRATION.
      Used to update a prisoner's domestic status.
      """,
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Successfully created/updated domestic status for the requested prisoner",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = SyncPrisonerDomesticStatusResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Could not find the reference data for the supplied domestic status code",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "400",
        description = "Invalid data provided in the request",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  @AuthApiResponses
  @PreAuthorize("hasAnyRole('PERSONAL_RELATIONSHIPS_MIGRATION')")
  fun syncUpdateDomesticStatus(
    @PathVariable prisonerNumber: String,
    @Valid @RequestBody request: SyncUpdatePrisonerDomesticStatusRequest,
  ) = prisonerDomesticStatusSyncFacade.createOrUpdateDomesticStatus(prisonerNumber, request)
}
