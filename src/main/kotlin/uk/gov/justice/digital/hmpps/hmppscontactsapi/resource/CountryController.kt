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
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.Country
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.CountryService
import uk.gov.justice.digital.hmpps.hmppscontactsapi.swagger.AuthApiResponses

@Tag(name = "Reference data")
@Deprecated(
  level = DeprecationLevel.WARNING,
  message = "No longer used. Please use the generic endpoint /reference-codes/group/{groupCode} for all reference values",
)
@RestController
@RequestMapping(value = ["country-reference"], produces = [MediaType.APPLICATION_JSON_VALUE])
@AuthApiResponses
class CountryController(private val countryService: CountryService) {

  @GetMapping("/{id}")
  @Operation(
    summary = "Get country reference",
    description = "Gets a country reference by their id",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Found the country reference",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = Country::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "No country reference with that id could be found",
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('ROLE_CONTACTS_ADMIN', 'ROLE_CONTACTS__R', 'ROLE_CONTACTS__RW')")
  fun getCountryById(@PathVariable id: Long) = countryService.getCountryById(id)

  @GetMapping
  @Operation(
    summary = "Get country reference",
    description = "Gets all country references",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Found the country reference",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = Country::class),
          ),
        ],
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('ROLE_CONTACTS_ADMIN', 'ROLE_CONTACTS__R', 'ROLE_CONTACTS__RW')")
  fun getAllCountries() = countryService.getAllCountries()

  @GetMapping("/nomis-code/{code}")
  @Operation(
    summary = "Get country reference",
    description = "Gets a country reference by their nomis code",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Found the country reference",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = Country::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "No country reference with that nomis code could be found",
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('ROLE_CONTACTS_ADMIN', 'ROLE_CONTACTS__R', 'ROLE_CONTACTS__RW')")
  fun getCountryByNomisCode(@PathVariable code: String) = countryService.getCountryByNomisCode(code)

  @GetMapping("/iso-alpha2/{code}")
  @Operation(
    summary = "Get country reference",
    description = "Gets a country reference by their ISO Alpha 2 code",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Found the country reference",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = Country::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "No country reference with that ISO Alpha 2 code could be found",
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('ROLE_CONTACTS_ADMIN', 'ROLE_CONTACTS__R', 'ROLE_CONTACTS__RW')")
  fun getCountryByIsoAlpha2(@PathVariable code: String) = countryService.getCountryByIsoAlpha2(code)

  @GetMapping("/iso-alpha3/{code}")
  @Operation(
    summary = "Get country reference",
    description = "Gets a country reference by their by ISO Alpha 3 code",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Found the country reference",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = Country::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "No country reference with that ISO Alpha 3 code could be found",
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('ROLE_CONTACTS_ADMIN', 'ROLE_CONTACTS__R', 'ROLE_CONTACTS__RW')")
  fun getCountryByIsoAlpha3(@PathVariable code: String) = countryService.getCountryByIsoAlpha3(code)
}
