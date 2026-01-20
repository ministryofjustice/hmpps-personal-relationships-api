package uk.gov.justice.digital.hmpps.personalrelationships.resource.migrate

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
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.migrate.MigratePrisonerDomesticStatusRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.migrate.PrisonerDomesticStatusMigrationResponse
import uk.gov.justice.digital.hmpps.personalrelationships.service.migrate.PrisonerDomesticStatusMigrationService
import uk.gov.justice.digital.hmpps.personalrelationships.swagger.AuthApiResponses
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

@Tag(name = "Migrate and sync")
@RestController
@RequestMapping(value = ["migrate/domestic-status"], produces = [MediaType.APPLICATION_JSON_VALUE])
@AuthApiResponses
class MigratePrisonerDomesticStatusController(
  private val migrationService: PrisonerDomesticStatusMigrationService,
) {
  @PostMapping(consumes = [MediaType.APPLICATION_JSON_VALUE])
  @Operation(
    summary = "Migrate domestic status for prisoner",
    description = "Migrate a prisoner's domestic status from NOMIS.",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "The domestic status was migrated successfully",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = PrisonerDomesticStatusMigrationResponse::class),
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
        description = "Could not find reference data for the supplied domestic status code",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('PERSONAL_RELATIONSHIPS_MIGRATION')")
  fun migrateDomesticStatus(
    @Valid @RequestBody request: MigratePrisonerDomesticStatusRequest,
  ): PrisonerDomesticStatusMigrationResponse = migrationService.migrateDomesticStatus(request)
}
