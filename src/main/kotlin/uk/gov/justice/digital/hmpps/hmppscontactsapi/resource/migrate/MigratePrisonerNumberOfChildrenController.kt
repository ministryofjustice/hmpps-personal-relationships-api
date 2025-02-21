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
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.migrate.MigratePrisonerNumberOfChildrenRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.migrate.PrisonerNumberOfChildrenMigrationResponse
import uk.gov.justice.digital.hmpps.hmppscontactsapi.swagger.AuthApiResponses

@Tag(name = "Sync & Migrate")
@RestController
@RequestMapping(value = ["migrate/number-of-children"], produces = [MediaType.APPLICATION_JSON_VALUE])
@AuthApiResponses
class MigratePrisonerNumberOfChildrenController {
  @PostMapping(consumes = [MediaType.APPLICATION_JSON_VALUE])
  @Operation(
    summary = "Migrate number of children for prisoner",
    description = "Migrate a prisoner's number of children from NOMIS.",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "The prisoner's number of children was migrated successfully",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = PrisonerNumberOfChildrenMigrationResponse::class),
          ),
        ],
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('PERSONAL_RELATIONSHIPS_MIGRATION')")
  fun migrateNumberOfChildren(
    @Valid @RequestBody request: MigratePrisonerNumberOfChildrenRequest,
  ): PrisonerNumberOfChildrenMigrationResponse = PrisonerNumberOfChildrenMigrationResponse(
    prisonerNumber = "A1234BC",
    current = 1L,
  )
}
