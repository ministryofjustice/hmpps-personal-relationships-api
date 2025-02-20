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
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.migrate.MigratePrisonerDependentsRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.migrate.DependentsDetailsResponse
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.migrate.PrisonerDependentsMigrationResponse
import uk.gov.justice.digital.hmpps.hmppscontactsapi.swagger.AuthApiResponses

@Tag(name = "Sync & Migrate")
@RestController
@RequestMapping(value = ["migrate/dependents"], produces = [MediaType.APPLICATION_JSON_VALUE])
@AuthApiResponses
class MigratePrisonerDependentsController(
  // private val migrationService: PrisonerDependentsMigrationService,
) {
  @PostMapping(consumes = [MediaType.APPLICATION_JSON_VALUE])
  @Operation(
    summary = "Migrate dependents for prisoner",
    description = "Migrate a prisoner's dependents from NOMIS.",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "The prisoner's dependents was migrated successfully",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = PrisonerDependentsMigrationResponse::class),
          ),
        ],
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('PERSONAL_RELATIONSHIPS_MIGRATION')")
  fun migrateDependents(
    @Valid @RequestBody request: MigratePrisonerDependentsRequest,
  ): PrisonerDependentsMigrationResponse = PrisonerDependentsMigrationResponse(
    prisonerNumber = "A1234BC",
    current = DependentsDetailsResponse(
      id = 1L,
    ),
    history = emptyList(),
  )
}
