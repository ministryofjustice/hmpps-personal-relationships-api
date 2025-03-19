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
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.Language
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.LanguageService
import uk.gov.justice.digital.hmpps.hmppscontactsapi.swagger.AuthApiResponses

@Tag(name = "Reference data")
@Deprecated(
  level = DeprecationLevel.WARNING,
  message = "No longer used. Please use the generic endpoint /reference-codes/group/{groupCode} for all reference values",
)
@RestController
@RequestMapping(value = ["language-reference"], produces = [MediaType.APPLICATION_JSON_VALUE])
@AuthApiResponses
class LanguageController(private val languageService: LanguageService) {

  @GetMapping("/{id}")
  @Operation(
    summary = "Get language reference",
    description = "Gets a language reference by their id",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Found the language reference",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = Language::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "No language reference with that id could be found",
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('ROLE_CONTACTS_ADMIN', 'ROLE_CONTACTS__R', 'ROLE_CONTACTS__RW')")
  fun getLanguageById(@PathVariable id: Long) = languageService.getLanguageById(id)

  @GetMapping
  @Operation(
    summary = "Get language reference",
    description = "Gets all language references",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Found the language reference",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = Language::class),
          ),
        ],
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('ROLE_CONTACTS_ADMIN', 'ROLE_CONTACTS__R', 'ROLE_CONTACTS__RW')")
  fun getAllLanguages() = languageService.getAllLanguages()

  @GetMapping("/nomis-code/{code}")
  @Operation(
    summary = "Get language reference",
    description = "Gets a language reference by their nomis code",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Found the language reference",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = Language::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "No language reference with that nomis code could be found",
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('ROLE_CONTACTS_ADMIN', 'ROLE_CONTACTS__R', 'ROLE_CONTACTS__RW')")
  fun getLanguageByNomisCode(@PathVariable code: String) = languageService.getLanguageByNomisCode(code)

  @GetMapping("/iso-alpha2/{code}")
  @Operation(
    summary = "Get language reference",
    description = "Gets a language reference by their ISO Alpha 2 code",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Found the language reference",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = Language::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "No language reference with that ISO Alpha 2 code could be found",
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('ROLE_CONTACTS_ADMIN', 'ROLE_CONTACTS__R', 'ROLE_CONTACTS__RW')")
  fun getLanguageByIsoAlpha2(@PathVariable code: String) = languageService.getLanguageByIsoAlpha2(code)

  @GetMapping("/iso-alpha3/{code}")
  @Operation(
    summary = "Get language reference",
    description = "Gets a language reference by their by ISO Alpha 3 code",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Found the language reference",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = Language::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "No language reference with that ISO Alpha 3 code could be found",
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('ROLE_CONTACTS_ADMIN', 'ROLE_CONTACTS__R', 'ROLE_CONTACTS__RW')")
  fun getLanguageByIsoAlpha3(@PathVariable code: String) = languageService.getLanguageByIsoAlpha3(code)
}
