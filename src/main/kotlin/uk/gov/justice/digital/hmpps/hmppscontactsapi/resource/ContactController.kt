package uk.gov.justice.digital.hmpps.hmppscontactsapi.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.headers.Header
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Past
import jakarta.validation.constraints.Pattern
import org.slf4j.LoggerFactory
import org.springdoc.core.converters.models.PageableAsQueryParam
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PagedModel
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestAttribute
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppscontactsapi.config.User
import uk.gov.justice.digital.hmpps.hmppscontactsapi.facade.ContactFacade
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.ContactSearchRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.CreateContactRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.PatchContactRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.ContactCreationResult
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.ContactDetails
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.ContactNameDetails
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.ContactSearchResultItem
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.PatchContactResponse
import uk.gov.justice.digital.hmpps.hmppscontactsapi.swagger.AuthApiResponses
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.net.URI
import java.time.LocalDate

@Tag(name = "Contacts")
@RestController
@RequestMapping(value = ["contact"], produces = [MediaType.APPLICATION_JSON_VALUE])
@AuthApiResponses
@Validated
class ContactController(
  val contactFacade: ContactFacade,
) {
  companion object {
    private val logger = LoggerFactory.getLogger(this::class.java)

    private const val VALID_NAME_REGEX = "[a-zA-Z\\s,.'-]*"
    private const val VALID_NAME_MESSAGE = "must be a letter or punctuation"
    private const val VALID_LETTER_OR_NUMBER_REGEX = "[a-zA-Z0-9]*"
    private const val VALID_LETTER_OR_NUMBER_MESSAGE = "must contain only letters or numbers"
  }

  @PostMapping(consumes = [MediaType.APPLICATION_JSON_VALUE])
  @Operation(
    summary = "Create a new contact",
    description = "Creates a new contact that is not yet associated with any prisoner.",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "201",
        description = "Created the contact successfully",
        headers = [
          Header(
            name = "Location",
            description = "The URL where you can load the contact",
            example = "/contact/123456",
          ),
        ],
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ContactCreationResult::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "400",
        description = "The request has invalid or missing fields",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Could not find the prisoner that this contact has a relationship to",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('ROLE_CONTACTS_ADMIN', 'ROLE_CONTACTS__RW')")
  fun createContact(@Valid @RequestBody createContactRequest: CreateContactRequest, @RequestAttribute user: User): ResponseEntity<Any> {
    val created = contactFacade.createContact(createContactRequest, user)
    return ResponseEntity
      .created(URI.create("/contact/${created.createdContact.id}"))
      .body(created)
  }

  @GetMapping("/{contactId}")
  @Operation(
    summary = "Get contact",
    description = "Gets a contact by their id",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Found the contact",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ContactDetails::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "No contact with that id could be found",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('ROLE_CONTACTS_ADMIN', 'ROLE_CONTACTS__R', 'ROLE_CONTACTS__RW')")
  fun getContact(
    @PathVariable("contactId") @Parameter(
      name = "contactId",
      description = "The id of the contact",
      example = "123456",
    ) contactId: Long,
  ): ResponseEntity<Any> {
    val contact = contactFacade.getContact(contactId)
    return if (contact != null) {
      ResponseEntity.ok(contact)
    } else {
      logger.info("Couldn't find contact with id '{}'", contactId)
      ResponseEntity.notFound().build()
    }
  }

  @GetMapping("/{contactId}/name")
  @Operation(
    summary = "Get contact name",
    description = "Gets a contacts name details by their id. Includes title code, description, first name, middle names and last name.",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Found the contact",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ContactNameDetails::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "No contact with that id could be found",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('ROLE_CONTACTS_ADMIN', 'ROLE_CONTACTS__R', 'ROLE_CONTACTS__RW')")
  fun getContactName(
    @PathVariable("contactId") @Parameter(
      name = "contactId",
      description = "The id of the contact",
      example = "123456",
    ) contactId: Long,
  ): ResponseEntity<Any> {
    val name = contactFacade.getContactName(contactId)
    return if (name != null) {
      ResponseEntity.ok(name)
    } else {
      logger.info("Couldn't find contact with id '{}' to get their name", contactId)
      ResponseEntity.notFound().build()
    }
  }

  @GetMapping("/search")
  @Operation(
    summary = "Search contacts",
    description = "Search all contacts by their last name or first name or middle name or date of birth or contact id",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Found contacts",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Invalid request",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('ROLE_CONTACTS_ADMIN', 'ROLE_CONTACTS__R', 'ROLE_CONTACTS__RW')")
  @PageableAsQueryParam
  fun searchContacts(
    @Parameter(hidden = true)
    pageable: Pageable,
    @Parameter(`in` = ParameterIn.QUERY, description = "Last name of the contact", example = "Jones", required = true)
    @NotBlank(message = "must not be blank")
    @Pattern(regexp = VALID_NAME_REGEX, message = VALID_NAME_MESSAGE)
    lastName: String,
    @Parameter(`in` = ParameterIn.QUERY, description = "First name of the contact", example = "Elton", required = false)
    @Pattern(regexp = VALID_NAME_REGEX, message = VALID_NAME_MESSAGE)
    firstName: String?,
    @Parameter(`in` = ParameterIn.QUERY, description = "Middle names of the contact", example = "Simon", required = false)
    @Pattern(regexp = VALID_NAME_REGEX, message = VALID_NAME_MESSAGE)
    middleNames: String?,
    @Parameter(
      `in` = ParameterIn.QUERY,
      description = "Date of Birth of the contact in ISO format",
      example = "30/12/2010",
      required = false,
    )
    @Parameter(`in` = ParameterIn.QUERY, description = "The contact ID", example = "123456", required = false)
    contactId: String?,
    @Parameter(
      `in` = ParameterIn.QUERY,
      description = "The contact id",
      example = "123456",
      required = false,
    )
    @Past(message = "The date of birth must be in the past")
    @DateTimeFormat(pattern = "dd/MM/yyyy")
    dateOfBirth: LocalDate?,
    @Parameter(
      `in` = ParameterIn.QUERY,
      description = "If true, use sounds-like search (trigram similarity)",
      example = "false",
      required = false,
    )
    soundsLike: Boolean = false,
    @Parameter(
      `in` = ParameterIn.QUERY,
      description = "If a prisoner number is specified, check all matching contacts for any existing relationships to the prisoner. " +
        "All matching contacts are returned regardless of whether they have an existing relationship to the prisoner or not.",
      example = "A1234BC",
      required = false,
    )
    @Pattern(regexp = VALID_LETTER_OR_NUMBER_REGEX, message = VALID_LETTER_OR_NUMBER_MESSAGE)
    includeAnyExistingRelationshipsToPrisoner: String?,
  ): PagedModel<ContactSearchResultItem> = contactFacade.searchContacts(
    pageable,
    ContactSearchRequest(
      lastName = lastName,
      firstName = firstName,
      middleNames = middleNames,
      dateOfBirth = dateOfBirth,
      soundsLike = soundsLike,
      contactId = contactId,
      includeAnyExistingRelationshipsToPrisoner = includeAnyExistingRelationshipsToPrisoner,
    ),
  )

  @PatchMapping("/{contactId}")
  @Operation(
    summary = "Update a contact",
    description = "Update a contact",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "The contact was updated.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = PatchContactResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "400",
        description = "Invalid request",
        content = [
          Content(schema = Schema(implementation = ErrorResponse::class)),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "No contact with that id could be found",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('ROLE_CONTACTS_ADMIN', 'ROLE_CONTACTS__RW')")
  fun patchContact(
    @PathVariable("contactId") @Parameter(
      name = "contactId",
      description = "The id of the contact",
      example = "123456",
    ) contactId: Long,
    @Valid @RequestBody patchContactRequest: PatchContactRequest,
    @RequestAttribute user: User,
  ) = contactFacade.patch(contactId, patchContactRequest, user)
}
