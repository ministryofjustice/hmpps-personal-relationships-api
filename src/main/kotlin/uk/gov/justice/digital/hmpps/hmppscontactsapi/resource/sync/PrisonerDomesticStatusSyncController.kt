package uk.gov.justice.digital.hmpps.hmppscontactsapi.resource.sync

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
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

@Tag(name = "Sync & Migrate")
@RestController
@RequestMapping(value = ["/sync"], produces = [MediaType.APPLICATION_JSON_VALUE])
class PrisonerDomesticStatusSyncController(
  private val prisonerDomesticStatusSyncFacade: PrisonerDomesticStatusSyncFacade,
) {
  @GetMapping(path = ["/domestic-status/{prisonerNumber}"], produces = [MediaType.APPLICATION_JSON_VALUE])
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
        description = "No domestic status for that prisoner could be found",
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('PERSONAL_RELATIONSHIPS_MIGRATION')")
  fun syncGetDomesticStatusByPrisonerNumber(
    @PathVariable prisonerNumber: String,
  ) = prisonerDomesticStatusSyncFacade.getDomesticStatusByPrisonerNumber(prisonerNumber)

  @PutMapping(path = ["/domestic-status/{prisonerNumber}"], produces = [MediaType.APPLICATION_JSON_VALUE])
  @ResponseBody
  @Operation(
    summary = "Updates the domestic status for a prisoner",
    description = """
      Requires role: PERSONAL_RELATIONSHIPS_MIGRATION.
      Used to update a prisoner's domestic status.
      """,
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Successfully updated domestic status",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = SyncPrisonerDomesticStatusResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Domestic status not found",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Invalid data provided in the request",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @AuthApiResponses
  @PreAuthorize("hasAnyRole('PERSONAL_RELATIONSHIPS_MIGRATION')")
  fun syncUpdateDomesticStatus(
    @PathVariable prisonerNumber: String,
    @RequestBody request: SyncUpdatePrisonerDomesticStatusRequest,
  ) = prisonerDomesticStatusSyncFacade.updateDomesticStatus(prisonerNumber, request)

  // TODO create domestic status from NOMIS will create a record, if we have a record we will move it to history for auditability, then add a new record.

  // TODO update domestic status, when updating a record on NOMIS we will move existing record to history and create a new record

  // TODO when deleting a record in NOMIS we will move that to inactive status - do not delete the record.

  @DeleteMapping(path = ["/domestic-status/{prisonerNumber}"], produces = [MediaType.APPLICATION_JSON_VALUE])
  @Operation(
    summary = "Deletes an domestic status record by prisoner number",
    description = "Requires role: PERSONAL_RELATIONSHIPS_MIGRATION. Delete an employment record by internal ID.",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "204",
        description = "Successfully deleted the prisoner domestic status record",
      ),
      ApiResponse(
        responseCode = "404",
        description = "No prisoner domestic status record with this ID could be found",
      ),
    ],
  )
  @AuthApiResponses
  @PreAuthorize("hasAnyRole('PERSONAL_RELATIONSHIPS_MIGRATION')")
  fun syncDeleteDomesticStatusById(@PathVariable prisonerNumber: String) {
    prisonerDomesticStatusSyncFacade.deleteDomesticStatus(prisonerNumber)
  }
}
