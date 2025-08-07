package uk.gov.justice.digital.hmpps.hmppscontactsapi.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppscontactsapi.facade.ContactFacade
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.UpdateInternalOfficialDobResponse
import uk.gov.justice.digital.hmpps.hmppscontactsapi.swagger.ProtectedByIngress

/**
 * These endpoints are secured in the ingress rather than the app so that they can be called from
 * within the namespace without requiring authentication
 */

@Tag(name = "Utility Controller")
@RestController
@ProtectedByIngress
@RequestMapping(value = ["utility"], produces = [MediaType.APPLICATION_JSON_VALUE])
class UtilityController(val contactFacade: ContactFacade) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @Operation(summary = "Endpoint to remove the date of birth from contacts in solely internal official relationships")
  @PutMapping(path = ["/remove-internal-official-dob"])
  @ResponseStatus(HttpStatus.OK)
  fun removeDateOfBirth(): UpdateInternalOfficialDobResponse = run {
    log.info("UTILITY: Remove internal official contacts dates of birth started")
    val result = contactFacade.removeInternalOfficialDateOfBirth()
    log.info("UTILITY: Remove internal official dates of birth completed - updated ${result.size} contacts")
    return UpdateInternalOfficialDobResponse(contactsUpdated = result)
  }
}
