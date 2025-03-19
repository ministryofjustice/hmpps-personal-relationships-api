package uk.gov.justice.digital.hmpps.hmppscontactsapi.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.County
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.CountyService
import uk.gov.justice.digital.hmpps.hmppscontactsapi.swagger.AuthApiResponses

@Tag(name = "Reference data")
@Deprecated(
  level = DeprecationLevel.WARNING,
  message = "No longer used. Please use the generic endpoint /reference-codes/group/{groupCode} for all reference values",
)
@RestController
@RequestMapping(value = ["county-reference"], produces = [MediaType.APPLICATION_JSON_VALUE])
@AuthApiResponses
class CountyController(private val countyService: CountyService) {

  @GetMapping("/{id}")
  @Operation(
    summary = "Get county reference",
    description = "Gets a county reference by their id",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Found the county reference",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = County::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "No county reference with that id could be found",
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('ROLE_CONTACTS_ADMIN', 'ROLE_CONTACTS__R', 'ROLE_CONTACTS__RW')")
  fun getCountyById(@PathVariable id: Long) = countyService.getCountyById(id)

  @GetMapping
  @Operation(
    summary = "Get county reference",
    description = "Gets all county references",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Found the county reference",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = County::class),
          ),
        ],
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('ROLE_CONTACTS_ADMIN', 'ROLE_CONTACTS__R', 'ROLE_CONTACTS__RW')")
  fun getAllCounties() = countyService.getAllCounties()

  @GetMapping("/nomis-code/{code}")
  @Operation(
    summary = "Get county reference",
    description = "Gets a county reference by their nomis code",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Found the county reference",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = County::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "No county reference with that nomis code could be found",
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('ROLE_CONTACTS_ADMIN', 'ROLE_CONTACTS__R', 'ROLE_CONTACTS__RW')")
  fun getCountyByNomisCode(@PathVariable code: String) = countyService.getCountyByNomisCode(code)
}
