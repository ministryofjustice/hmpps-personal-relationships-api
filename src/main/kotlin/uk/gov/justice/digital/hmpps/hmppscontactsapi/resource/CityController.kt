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
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.City
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.CityService
import uk.gov.justice.digital.hmpps.hmppscontactsapi.swagger.AuthApiResponses

@Tag(name = "Reference data")
@Deprecated(
  level = DeprecationLevel.WARNING,
  message = "No longer used. Please use the generic endpoint /reference-codes/group/{groupCode} for all reference values",
)
@RestController
@RequestMapping(value = ["city-reference"], produces = [MediaType.APPLICATION_JSON_VALUE])
@AuthApiResponses
class CityController(private val cityService: CityService) {

  @GetMapping("/{id}")
  @Operation(
    summary = "Get city reference",
    description = "Gets a city reference by their id",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Found the city reference",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = City::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "No city reference with that id could be found",
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('ROLE_CONTACTS_ADMIN', 'ROLE_CONTACTS__R', 'ROLE_CONTACTS__RW')")
  fun getCityById(@PathVariable id: Long): City = cityService.getCityById(id)

  @GetMapping
  @Operation(
    summary = "Get city reference",
    description = "Gets all city references",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Found the city reference",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = City::class),
          ),
        ],
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('ROLE_CONTACTS_ADMIN', 'ROLE_CONTACTS__R', 'ROLE_CONTACTS__RW')")
  fun getAllCities() = cityService.getAllCities()

  @GetMapping("/nomis-code/{code}")
  @Operation(
    summary = "Get city reference",
    description = "Gets a city reference by their nomis code",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Found the city reference",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = City::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "No city reference with that nomis code could be found",
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('ROLE_CONTACTS_ADMIN', 'ROLE_CONTACTS__R', 'ROLE_CONTACTS__RW')")
  fun getCityByNomisCode(@PathVariable code: String) = cityService.getCityByNomisCode(code)
}
