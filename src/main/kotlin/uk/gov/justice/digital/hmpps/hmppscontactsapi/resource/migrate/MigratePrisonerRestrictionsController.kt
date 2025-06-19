package uk.gov.justice.digital.hmpps.hmppscontactsapi.resource.migrate

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.migrate.MigratePrisonerRestrictionsRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.migrate.PrisonerRestrictionsMigrationResponse
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.migrate.PrisonerRestrictionsMigrationService
import uk.gov.justice.digital.hmpps.hmppscontactsapi.swagger.AuthApiResponses
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

@Tag(name = "Migrate and sync")
@RestController
@RequestMapping(value = ["migrate/prisoner-restrictions"], produces = [MediaType.APPLICATION_JSON_VALUE])
@AuthApiResponses
class MigratePrisonerRestrictionsController(
  private val migrationService: PrisonerRestrictionsMigrationService,
) {
  @PostMapping(consumes = [MediaType.APPLICATION_JSON_VALUE])
  @Operation(
    summary = "Migrate restrictions for prisoner",
    description = "Migrate a prisoner's restrictions from NOMIS.",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "The restrictions were migrated successfully",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = PrisonerRestrictionsMigrationResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "400",
        description = "The request failed validation with invalid or missing data supplied",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Could not find reference data for the supplied restriction type",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('PERSONAL_RELATIONSHIPS_MIGRATION')")
  fun migratePrisonerRestrictions(
    @Valid @RequestBody request: MigratePrisonerRestrictionsRequest,
  ): PrisonerRestrictionsMigrationResponse = migrationService.migratePrisonerRestrictions(request)
}
