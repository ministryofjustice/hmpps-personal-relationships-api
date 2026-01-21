package uk.gov.justice.digital.hmpps.personalrelationships.resource.sync

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
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.personalrelationships.entity.EmploymentEntity
import uk.gov.justice.digital.hmpps.personalrelationships.facade.SyncFacade
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.sync.SyncCreateEmploymentRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.sync.SyncUpdateEmploymentRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.sync.SyncEmployment
import uk.gov.justice.digital.hmpps.personalrelationships.swagger.AuthApiResponses
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

@Tag(name = "Migrate and sync")
@RestController
@RequestMapping(value = ["/sync"], produces = [MediaType.APPLICATION_JSON_VALUE])
class EmploymentSyncController(
  private val syncFacade: SyncFacade,
) {
  @GetMapping(path = ["/employment/{employmentId}"], produces = [MediaType.APPLICATION_JSON_VALUE])
  @Operation(
    summary = "Returns the data for an employment record by employmentId",
    description = "Requires role: PERSONAL_RELATIONSHIPS_MIGRATION. Used to get the details for one employment record.",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "The details of the employment record",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = EmploymentEntity::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "No employment record with this ID could be found",
      ),
    ],
  )
  @AuthApiResponses
  @PreAuthorize("hasAnyRole('PERSONAL_RELATIONSHIPS_MIGRATION')")
  fun syncGetEmploymentById(@PathVariable employmentId: Long): SyncEmployment = syncFacade.getEmploymentById(employmentId)

  @DeleteMapping(path = ["/employment/{employmentId}"], produces = [MediaType.APPLICATION_JSON_VALUE])
  @Operation(
    summary = "Deletes an employment record by internal ID",
    description = "Requires role: PERSONAL_RELATIONSHIPS_MIGRATION. Delete an employment record by internal ID.",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "204",
        description = "Successfully deleted an employment record",
      ),
      ApiResponse(
        responseCode = "404",
        description = "No employment record with this ID could be found",
      ),
    ],
  )
  @AuthApiResponses
  @PreAuthorize("hasAnyRole('PERSONAL_RELATIONSHIPS_MIGRATION')")
  fun syncDeleteEmploymentById(@PathVariable employmentId: Long) {
    syncFacade.deleteEmployment(employmentId)
  }

  @PostMapping(path = ["/employment"], produces = [MediaType.APPLICATION_JSON_VALUE])
  @ResponseBody
  @Operation(
    summary = "Creates a new employment record",
    description = "Requires role: PERSONAL_RELATIONSHIPS_MIGRATION. Used to create a new employment record.",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "201",
        description = "Successfully created an employment record",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = SyncEmployment::class))],
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
  fun syncCreateEmployment(
    @RequestBody employmentRequest: SyncCreateEmploymentRequest,
  ): SyncEmployment = syncFacade.createEmployment(employmentRequest)

  @PutMapping(path = ["/employment/{employmentId}"], produces = [MediaType.APPLICATION_JSON_VALUE])
  @ResponseBody
  @Operation(
    summary = "Updates an employment record",
    description = "Requires role: PERSONAL_RELATIONSHIPS_MIGRATION. Used to update an employment record.",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Successfully updated an employment record",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = SyncEmployment::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "The employment record was not found",
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
  fun syncUpdateEmployment(
    @PathVariable employmentId: Long,
    @RequestBody employment: SyncUpdateEmploymentRequest,
  ): SyncEmployment = syncFacade.updateEmployment(employmentId, employment)
}
