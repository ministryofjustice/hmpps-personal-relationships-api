package uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.helper

import PrisonerRestrictionId
import org.springframework.data.web.PagedModel
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.ReferenceCodeGroup
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.AddContactRelationshipRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.CreateContactRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.PatchRelationshipRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.PrisonerContactIdsRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.address.CreateContactAddressRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.address.PatchContactAddressRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.address.UpdateContactAddressRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.email.CreateEmailRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.email.CreateMultipleEmailsRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.email.UpdateEmailRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.employment.CreateEmploymentRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.employment.PatchEmploymentsRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.employment.UpdateEmploymentRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.identity.CreateIdentityRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.identity.CreateMultipleIdentitiesRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.identity.UpdateIdentityRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.migrate.MigrateContactRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.phone.CreateContactAddressPhoneRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.phone.CreateMultiplePhoneNumbersRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.phone.CreatePhoneRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.phone.UpdateContactAddressPhoneRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.phone.UpdatePhoneRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.restrictions.CreateContactRestrictionRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.restrictions.CreatePrisonerContactRestrictionRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.restrictions.UpdateContactRestrictionRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.restrictions.UpdatePrisonerContactRestrictionRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync.SyncCreateContactRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.AdvancedContactSearchResultItem
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.ContactAddressPhoneDetails
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.ContactAddressResponse
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.ContactAuditEntry
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.ContactCreationResult
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.ContactDetails
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.ContactEmailDetails
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.ContactIdentityDetails
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.ContactNameDetails
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.ContactPhoneDetails
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.ContactRestrictionDetails
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.ContactSearchResultItem
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.EmploymentDetails
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.LinkedPrisonerDetails
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.PatchContactResponse
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.PrisonerContactRelationshipCount
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.PrisonerContactRelationshipDetails
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.PrisonerContactRestrictionDetails
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.PrisonerContactRestrictionsResponse
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.PrisonerContactSummary
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.PrisonerContactsRestrictionsResponse
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.PrisonerRestrictionDetails
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.ReferenceCode
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.RelationshipDeletePlan
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.migrate.MigrateContactResponse
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.sync.SyncContact
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.sync.SyncContactId
import uk.gov.justice.digital.hmpps.hmppscontactsapi.util.StubUser
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import uk.gov.justice.hmpps.test.kotlin.auth.JwtAuthorisationHelper
import java.net.URI

class TestAPIClient(private val webTestClient: WebTestClient, private val jwtAuthHelper: JwtAuthorisationHelper, var currentUser: StubUser?) {

  fun syncReconcileContacts(page: Long = 0, size: Long = 10) = webTestClient.get()
    .uri("/sync/contact/reconcile?page=$page&size=$size")
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisationUsingCurrentUser())
    .exchange()
    .expectStatus()
    .isOk
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBody(ContactIdsResponse::class.java)
    .returnResult().responseBody!!

  fun syncCreateAnContact(request: SyncCreateContactRequest) = webTestClient.post()
    .uri("/sync/contact")
    .accept(MediaType.APPLICATION_JSON)
    .contentType(MediaType.APPLICATION_JSON)
    .headers(setAuthorisationUsingCurrentUser())
    .bodyValue(request)
    .exchange()
    .expectStatus()
    .isOk
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBody(SyncContact::class.java)
    .returnResult().responseBody!!

  fun createAContact(request: CreateContactRequest): ContactDetails = createAContactWithARelationship(
    request,
  ).createdContact

  fun createAContactWithARelationship(request: CreateContactRequest): ContactCreationResult = webTestClient.post()
    .uri("/contact")
    .accept(MediaType.APPLICATION_JSON)
    .contentType(MediaType.APPLICATION_JSON)
    .headers(setAuthorisationUsingCurrentUser())
    .bodyValue(request)
    .exchange()
    .expectStatus()
    .isCreated
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectHeader().valuesMatch("Location", "/contact/(\\d)+")
    .expectBody(ContactCreationResult::class.java)
    .returnResult().responseBody!!

  fun patchAContact(request: Any, url: String): PatchContactResponse = webTestClient.patch()
    .uri(url)
    .accept(MediaType.APPLICATION_JSON)
    .contentType(MediaType.APPLICATION_JSON)
    .headers(setAuthorisationUsingCurrentUser())
    .bodyValue(request)
    .exchange()
    .expectStatus()
    .isOk
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBody(PatchContactResponse::class.java)
    .returnResult().responseBody!!

  fun getContact(id: Long): ContactDetails = webTestClient.get()
    .uri("/contact/$id")
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisationUsingCurrentUser())
    .exchange()
    .expectStatus()
    .isOk
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBody(ContactDetails::class.java)
    .returnResult().responseBody!!

  fun getContactName(id: Long): ContactNameDetails = webTestClient.get()
    .uri("/contact/$id/name")
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisationUsingCurrentUser())
    .exchange()
    .expectStatus()
    .isOk
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBody(ContactNameDetails::class.java)
    .returnResult().responseBody!!

  fun getPrisonerContacts(prisonerNumber: String): PrisonerContactSummaryResponse = webTestClient.get()
    .uri("/prisoner/$prisonerNumber/contact")
    .headers(setAuthorisationUsingCurrentUser())
    .exchange()
    .expectStatus()
    .isOk
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBody(PrisonerContactSummaryResponse::class.java)
    .returnResult().responseBody!!

  fun getAllSummariesForPrisonerAndContact(prisonerNumber: String, contactId: Long): List<PrisonerContactSummary> = webTestClient.get()
    .uri("/prisoner/$prisonerNumber/contact/$contactId")
    .headers(setAuthorisationUsingCurrentUser())
    .exchange()
    .expectStatus()
    .isOk
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBodyList(PrisonerContactSummary::class.java)
    .returnResult().responseBody!!

  fun getPrisonerContactRelationshipCount(prisonerNumber: String): PrisonerContactRelationshipCount = webTestClient.get()
    .uri("/prisoner/$prisonerNumber/contact/count")
    .headers(setAuthorisationUsingCurrentUser())
    .exchange()
    .expectStatus()
    .isOk
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBody(PrisonerContactRelationshipCount::class.java)
    .returnResult().responseBody!!

  fun getReferenceCodes(
    groupCode: ReferenceCodeGroup,
    sort: String? = null,
    activeOnly: Boolean? = null,
  ): MutableList<ReferenceCode>? = webTestClient.get()
    .uri("/reference-codes/group/$groupCode?${sort?.let { "sort=$sort&" } ?: ""}${activeOnly?.let { "&activeOnly=$activeOnly" } ?: ""}")
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisationUsingCurrentUser())
    .exchange()
    .expectStatus().isOk
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBodyList(ReferenceCode::class.java)
    .returnResult().responseBody

  fun addAContactRelationship(request: AddContactRelationshipRequest): PrisonerContactRelationshipDetails = webTestClient.post()
    .uri("/prisoner-contact")
    .accept(MediaType.APPLICATION_JSON)
    .contentType(MediaType.APPLICATION_JSON)
    .headers(setAuthorisationUsingCurrentUser())
    .bodyValue(request)
    .exchange()
    .expectStatus()
    .isCreated
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBody(PrisonerContactRelationshipDetails::class.java)
    .returnResult().responseBody!!

  fun getSearchContactResults(uri: URI) = webTestClient.get()
    .uri(uri.toString())
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisationUsingCurrentUser())
    .exchange()
    .expectStatus().isOk
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBody(ContactSearchResponse::class.java)
    .returnResult().responseBody

  fun getAdvancedSearchContactResults(uri: URI) = webTestClient.get()
    .uri(uri.toString())
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisationUsingCurrentUser())
    .exchange()
    .expectStatus().isOk
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBody(AdvancedContactSearchResponse::class.java)
    .returnResult().responseBody

  fun getBadResponseErrors(uri: URI) = webTestClient.get()
    .uri(uri.toString())
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisationUsingCurrentUser())
    .exchange()
    .expectStatus()
    .isBadRequest
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBody(ErrorResponse::class.java)
    .returnResult().responseBody!!

  fun <T> getBadResponseErrorsWithPatch(request: T, uri: URI) = webTestClient.patch()
    .uri(uri.toString())
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisationUsingCurrentUser())
    .bodyValue(request!!)
    .exchange()
    .expectStatus()
    .isBadRequest
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBody(ErrorResponse::class.java)
    .returnResult().responseBody!!

  fun createAContactPhone(contactId: Long, request: CreatePhoneRequest): ContactPhoneDetails = webTestClient.post()
    .uri("/contact/$contactId/phone")
    .accept(MediaType.APPLICATION_JSON)
    .contentType(MediaType.APPLICATION_JSON)
    .headers(setAuthorisationUsingCurrentUser())
    .bodyValue(request)
    .exchange()
    .expectStatus()
    .isCreated
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBody(ContactPhoneDetails::class.java)
    .returnResult().responseBody!!

  fun createMultipleContactPhones(contactId: Long, request: CreateMultiplePhoneNumbersRequest): List<ContactPhoneDetails> = webTestClient.post()
    .uri("/contact/$contactId/phones")
    .accept(MediaType.APPLICATION_JSON)
    .contentType(MediaType.APPLICATION_JSON)
    .headers(setAuthorisationUsingCurrentUser())
    .bodyValue(request)
    .exchange()
    .expectStatus()
    .isCreated
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBodyList(ContactPhoneDetails::class.java)
    .returnResult().responseBody!!

  fun updateAContactPhone(contactId: Long, contactPhoneId: Long, request: UpdatePhoneRequest): ContactPhoneDetails = webTestClient.put()
    .uri("/contact/$contactId/phone/$contactPhoneId")
    .accept(MediaType.APPLICATION_JSON)
    .contentType(MediaType.APPLICATION_JSON)
    .headers(setAuthorisationUsingCurrentUser())
    .bodyValue(request)
    .exchange()
    .expectStatus()
    .isOk
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBody(ContactPhoneDetails::class.java)
    .returnResult().responseBody!!

  fun getContactPhone(contactId: Long, contactPhoneId: Long): ContactPhoneDetails = webTestClient.get()
    .uri("/contact/$contactId/phone/$contactPhoneId")
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisationUsingCurrentUser())
    .exchange()
    .expectStatus()
    .isOk
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBody(ContactPhoneDetails::class.java)
    .returnResult().responseBody!!

  fun createAContactAddressPhone(
    contactId: Long,
    contactAddressId: Long,
    request: CreateContactAddressPhoneRequest,
  ): ContactAddressPhoneDetails = webTestClient.post()
    .uri("/contact/$contactId/address/$contactAddressId/phone")
    .accept(MediaType.APPLICATION_JSON)
    .contentType(MediaType.APPLICATION_JSON)
    .headers(setAuthorisationUsingCurrentUser())
    .bodyValue(request)
    .exchange()
    .expectStatus()
    .isCreated
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBody(ContactAddressPhoneDetails::class.java)
    .returnResult().responseBody!!

  fun createMultipleContactAddressPhones(contactId: Long, contactAddressId: Long, request: CreateMultiplePhoneNumbersRequest): List<ContactAddressPhoneDetails> = webTestClient.post()
    .uri("/contact/$contactId/address/$contactAddressId/phones")
    .accept(MediaType.APPLICATION_JSON)
    .contentType(MediaType.APPLICATION_JSON)
    .headers(setAuthorisationUsingCurrentUser())
    .bodyValue(request)
    .exchange()
    .expectStatus()
    .isCreated
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBodyList(ContactAddressPhoneDetails::class.java)
    .returnResult().responseBody!!

  fun updateAContactAddressPhone(
    contactId: Long,
    contactAddressId: Long,
    contactAddressPhoneId: Long,
    request: UpdateContactAddressPhoneRequest,
  ): ContactAddressPhoneDetails = webTestClient.put()
    .uri("/contact/$contactId/address/$contactAddressId/phone/$contactAddressPhoneId")
    .accept(MediaType.APPLICATION_JSON)
    .contentType(MediaType.APPLICATION_JSON)
    .headers(setAuthorisationUsingCurrentUser())
    .bodyValue(request)
    .exchange()
    .expectStatus()
    .isOk
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBody(ContactAddressPhoneDetails::class.java)
    .returnResult().responseBody!!

  fun createAContactIdentity(contactId: Long, request: CreateIdentityRequest): ContactIdentityDetails = webTestClient.post()
    .uri("/contact/$contactId/identity")
    .accept(MediaType.APPLICATION_JSON)
    .contentType(MediaType.APPLICATION_JSON)
    .headers(setAuthorisationUsingCurrentUser())
    .bodyValue(request)
    .exchange()
    .expectStatus()
    .isCreated
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBody(ContactIdentityDetails::class.java)
    .returnResult().responseBody!!

  fun createMultipleContactIdentityDocuments(contactId: Long, request: CreateMultipleIdentitiesRequest): List<ContactIdentityDetails> = webTestClient.post()
    .uri("/contact/$contactId/identities")
    .accept(MediaType.APPLICATION_JSON)
    .contentType(MediaType.APPLICATION_JSON)
    .headers(setAuthorisationUsingCurrentUser())
    .bodyValue(request)
    .exchange()
    .expectStatus()
    .isCreated
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBodyList(ContactIdentityDetails::class.java)
    .returnResult().responseBody!!

  fun updateAContactIdentity(
    contactId: Long,
    contactIdentityId: Long,
    request: UpdateIdentityRequest,
  ): ContactIdentityDetails = webTestClient.put()
    .uri("/contact/$contactId/identity/$contactIdentityId")
    .accept(MediaType.APPLICATION_JSON)
    .contentType(MediaType.APPLICATION_JSON)
    .headers(setAuthorisationUsingCurrentUser())
    .bodyValue(request)
    .exchange()
    .expectStatus()
    .isOk
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBody(ContactIdentityDetails::class.java)
    .returnResult().responseBody!!

  fun getContactIdentity(contactId: Long, contactIdentityId: Long): ContactIdentityDetails = webTestClient.get()
    .uri("/contact/$contactId/identity/$contactIdentityId")
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisationUsingCurrentUser())
    .exchange()
    .expectStatus()
    .isOk
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBody(ContactIdentityDetails::class.java)
    .returnResult().responseBody!!

  fun createAContactEmail(contactId: Long, request: CreateEmailRequest): ContactEmailDetails = webTestClient.post()
    .uri("/contact/$contactId/email")
    .accept(MediaType.APPLICATION_JSON)
    .contentType(MediaType.APPLICATION_JSON)
    .headers(setAuthorisationUsingCurrentUser())
    .bodyValue(request)
    .exchange()
    .expectStatus()
    .isCreated
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBody(ContactEmailDetails::class.java)
    .returnResult().responseBody!!

  fun createContactEmails(contactId: Long, request: CreateMultipleEmailsRequest): List<ContactEmailDetails> = webTestClient.post()
    .uri("/contact/$contactId/emails")
    .accept(MediaType.APPLICATION_JSON)
    .contentType(MediaType.APPLICATION_JSON)
    .headers(setAuthorisationUsingCurrentUser())
    .bodyValue(request)
    .exchange()
    .expectStatus()
    .isCreated
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBodyList(ContactEmailDetails::class.java)
    .returnResult().responseBody!!

  fun updateAContactEmail(contactId: Long, contactEmailId: Long, request: UpdateEmailRequest): ContactEmailDetails = webTestClient.put()
    .uri("/contact/$contactId/email/$contactEmailId")
    .accept(MediaType.APPLICATION_JSON)
    .contentType(MediaType.APPLICATION_JSON)
    .headers(setAuthorisationUsingCurrentUser())
    .bodyValue(request)
    .exchange()
    .expectStatus()
    .isOk
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBody(ContactEmailDetails::class.java)
    .returnResult().responseBody!!

  fun updateRelationship(prisonerContactId: Long, request: PatchRelationshipRequest) {
    webTestClient.patch()
      .uri("/prisoner-contact/$prisonerContactId")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .headers(setAuthorisationUsingCurrentUser())
      .bodyValue(request)
      .exchange()
      .expectStatus()
      .isNoContent
  }

  fun getContactEmail(contactId: Long, contactEmailId: Long): ContactEmailDetails = webTestClient.get()
    .uri("/contact/$contactId/email/$contactEmailId")
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisationUsingCurrentUser())
    .exchange()
    .expectStatus()
    .isOk
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBody(ContactEmailDetails::class.java)
    .returnResult().responseBody!!

  fun migrateAContact(request: MigrateContactRequest) = webTestClient.post()
    .uri("/migrate/contact")
    .accept(MediaType.APPLICATION_JSON)
    .contentType(MediaType.APPLICATION_JSON)
    .headers(setAuthorisationUsingCurrentUser())
    .bodyValue(request)
    .exchange()
    .expectStatus()
    .isOk
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBody(MigrateContactResponse::class.java)
    .returnResult().responseBody!!

  fun getContactGlobalRestrictions(contactId: Long): List<ContactRestrictionDetails> = webTestClient.get()
    .uri("/contact/$contactId/restriction")
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisationUsingCurrentUser())
    .exchange()
    .expectStatus().isOk
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBodyList(ContactRestrictionDetails::class.java)
    .returnResult().responseBody!!

  fun createContactGlobalRestriction(
    contactId: Long,
    request: CreateContactRestrictionRequest,
  ): ContactRestrictionDetails = webTestClient.post()
    .uri("/contact/$contactId/restriction")
    .accept(MediaType.APPLICATION_JSON)
    .contentType(MediaType.APPLICATION_JSON)
    .headers(setAuthorisationUsingCurrentUser())
    .bodyValue(request)
    .exchange()
    .expectStatus()
    .isCreated
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBody(ContactRestrictionDetails::class.java)
    .returnResult().responseBody!!

  fun updateContactGlobalRestriction(
    contactId: Long,
    contactRestrictionId: Long,
    request: UpdateContactRestrictionRequest,
  ): ContactRestrictionDetails = webTestClient.put()
    .uri("/contact/$contactId/restriction/$contactRestrictionId")
    .accept(MediaType.APPLICATION_JSON)
    .contentType(MediaType.APPLICATION_JSON)
    .headers(setAuthorisationUsingCurrentUser())
    .bodyValue(request)
    .exchange()
    .expectStatus()
    .isOk
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBody(ContactRestrictionDetails::class.java)
    .returnResult().responseBody!!

  fun getPrisonerContactRestrictions(prisonerContactId: Long): PrisonerContactRestrictionsResponse = webTestClient.get()
    .uri("/prisoner-contact/$prisonerContactId/restriction")
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisationUsingCurrentUser())
    .exchange()
    .expectStatus().isOk
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBody(PrisonerContactRestrictionsResponse::class.java)
    .returnResult().responseBody!!

  fun postPrisonerContactsRestrictions(vararg prisonerContactIds: Long): PrisonerContactsRestrictionsResponse = webTestClient.post()
    .uri("/prisoner-contact/restrictions")
    .bodyValue(PrisonerContactIdsRequest(prisonerContactIds.toList()))
    .headers(setAuthorisationUsingCurrentUser())
    .exchange()
    .expectStatus().isOk
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBody(PrisonerContactsRestrictionsResponse::class.java)
    .returnResult().responseBody!!

  fun createPrisonerContactRestriction(
    prisonerContactId: Long,
    request: CreatePrisonerContactRestrictionRequest,
  ): PrisonerContactRestrictionDetails = webTestClient.post()
    .uri("/prisoner-contact/$prisonerContactId/restriction")
    .accept(MediaType.APPLICATION_JSON)
    .contentType(MediaType.APPLICATION_JSON)
    .headers(setAuthorisationUsingCurrentUser())
    .bodyValue(request)
    .exchange()
    .expectStatus()
    .isCreated
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBody(PrisonerContactRestrictionDetails::class.java)
    .returnResult().responseBody!!

  fun updatePrisonerContactRestriction(
    prisonerContactId: Long,
    prisonerRestrictionContactId: Long,
    request: UpdatePrisonerContactRestrictionRequest,
  ): PrisonerContactRestrictionDetails = webTestClient.put()
    .uri("/prisoner-contact/$prisonerContactId/restriction/$prisonerRestrictionContactId")
    .accept(MediaType.APPLICATION_JSON)
    .contentType(MediaType.APPLICATION_JSON)
    .headers(setAuthorisationUsingCurrentUser())
    .bodyValue(request)
    .exchange()
    .expectStatus()
    .isOk
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBody(PrisonerContactRestrictionDetails::class.java)
    .returnResult().responseBody!!

  fun createAContactAddress(contactId: Long, request: CreateContactAddressRequest): ContactAddressResponse = webTestClient.post()
    .uri("/contact/$contactId/address")
    .accept(MediaType.APPLICATION_JSON)
    .contentType(MediaType.APPLICATION_JSON)
    .headers(setAuthorisationUsingCurrentUser())
    .bodyValue(request)
    .exchange()
    .expectStatus()
    .isCreated
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBody(ContactAddressResponse::class.java)
    .returnResult().responseBody!!

  fun updateAContactAddress(contactId: Long, contactAddressId: Long, request: UpdateContactAddressRequest): ContactAddressResponse = webTestClient.put()
    .uri("/contact/$contactId/address/$contactAddressId")
    .accept(MediaType.APPLICATION_JSON)
    .contentType(MediaType.APPLICATION_JSON)
    .headers(setAuthorisationUsingCurrentUser())
    .bodyValue(request)
    .exchange()
    .expectStatus()
    .isOk
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBody(ContactAddressResponse::class.java)
    .returnResult().responseBody!!

  fun patchAContactAddress(contactId: Long, contactAddressId: Long, request: PatchContactAddressRequest): ContactAddressResponse = webTestClient.patch()
    .uri("/contact/$contactId/address/$contactAddressId")
    .accept(MediaType.APPLICATION_JSON)
    .contentType(MediaType.APPLICATION_JSON)
    .headers(setAuthorisationUsingCurrentUser())
    .bodyValue(request)
    .exchange()
    .expectStatus()
    .isOk
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBody(ContactAddressResponse::class.java)
    .returnResult().responseBody!!

  fun getLinkedPrisoners(contactId: Long, page: Int? = null, size: Int? = null): LinkedPrisonerResponse = webTestClient.get()
    .uri("/contact/$contactId/linked-prisoners?${page?.let { "page=$page&"} }${size?.let { "size=$size"} }")
    .headers(setAuthorisationUsingCurrentUser())
    .exchange()
    .expectStatus()
    .isOk
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBody(LinkedPrisonerResponse::class.java)
    .returnResult().responseBody!!

  fun getPrisonerRestrictions(prisonerNumber: String, currentTermOnly: Boolean? = false, paged: Boolean? = true, page: Int? = null, size: Int? = null): PrisonerRestrictionsResponse = webTestClient.get()
    .uri("/prisoner-restrictions/$prisonerNumber?${currentTermOnly?.let { "currentTermOnly=$currentTermOnly&"} }${paged?.let { "paged=$paged&"} }${page?.let { "page=$page&"} }${size?.let { "size=$size"} }")
    .headers(setAuthorisationUsingCurrentUser())
    .exchange()
    .expectStatus()
    .isOk
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBody(PrisonerRestrictionsResponse::class.java)
    .returnResult().responseBody!!

  fun patchEmployments(contactId: Long, request: PatchEmploymentsRequest): List<EmploymentDetails> = webTestClient.patch()
    .uri("/contact/$contactId/employment")
    .accept(MediaType.APPLICATION_JSON)
    .contentType(MediaType.APPLICATION_JSON)
    .headers(setAuthorisationUsingCurrentUser())
    .bodyValue(request)
    .exchange()
    .expectStatus()
    .isOk
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBodyList(EmploymentDetails::class.java)
    .returnResult().responseBody!!

  fun createAnEmployment(contactId: Long, request: CreateEmploymentRequest): EmploymentDetails = webTestClient.post()
    .uri("/contact/$contactId/employment")
    .accept(MediaType.APPLICATION_JSON)
    .contentType(MediaType.APPLICATION_JSON)
    .headers(setAuthorisationUsingCurrentUser())
    .bodyValue(request)
    .exchange()
    .expectStatus()
    .isCreated
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectHeader().valuesMatch("Location", "/contact/$contactId/employment/(\\d)+")
    .expectBody(EmploymentDetails::class.java)
    .returnResult().responseBody!!

  fun getAnEmployment(contactId: Long, employmentId: Long): EmploymentDetails = webTestClient.get()
    .uri("/contact/$contactId/employment/$employmentId")
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisationUsingCurrentUser())
    .exchange()
    .expectStatus()
    .isOk
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBody(EmploymentDetails::class.java)
    .returnResult().responseBody!!

  fun updateAnEmployment(contactId: Long, employmentId: Long, request: UpdateEmploymentRequest): EmploymentDetails = webTestClient.put()
    .uri("/contact/$contactId/employment/$employmentId")
    .accept(MediaType.APPLICATION_JSON)
    .contentType(MediaType.APPLICATION_JSON)
    .headers(setAuthorisationUsingCurrentUser())
    .bodyValue(request)
    .exchange()
    .expectStatus()
    .isOk
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBody(EmploymentDetails::class.java)
    .returnResult().responseBody!!

  fun deleteAnEmployment(contactId: Long, employmentId: Long) {
    webTestClient.delete()
      .uri("/contact/$contactId/employment/$employmentId")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisationUsingCurrentUser())
      .exchange()
      .expectStatus()
      .isNoContent
  }

  fun planDeletePrisonerContact(prisonerContactId: Long) = webTestClient.get()
    .uri("/prisoner-contact/$prisonerContactId/plan-delete")
    .headers(setAuthorisationUsingCurrentUser())
    .exchange()
    .expectStatus()
    .isOk
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBody(RelationshipDeletePlan::class.java)
    .returnResult().responseBody!!

  fun deletePrisonerContact(prisonerContactId: Long) {
    webTestClient.delete()
      .uri("/prisoner-contact/$prisonerContactId")
      .headers(setAuthorisationUsingCurrentUser())
      .exchange()
      .expectStatus()
      .isNoContent
  }

  fun getContactHistory(contactId: Long): List<ContactAuditEntry> = webTestClient.get()
    .uri("/contact/$contactId/history")
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisationUsingCurrentUser())
    .exchange()
    .expectStatus()
    .isOk
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBodyList(ContactAuditEntry::class.java)
    .returnResult().responseBody!!

  fun getSearchContactsById(contactId: String, includeAnyExistingRelationshipsToPrisoner: String? = null) = webTestClient.get()
    .uri("/contact/search/contact-id?contactId=$contactId${includeAnyExistingRelationshipsToPrisoner?.let { "&includeAnyExistingRelationshipsToPrisoner=$it" } ?: ""}")
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisationUsingCurrentUser())
    .exchange()
    .expectStatus().isOk
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBody(AdvancedContactSearchResponse::class.java)
    .returnResult().responseBody

  data class AdvancedContactSearchResponse(
    val content: List<AdvancedContactSearchResultItem>,
    val page: PagedModel.PageMetadata,
  )

  data class ContactSearchResponse(
    val content: List<ContactSearchResultItem>,
    val page: PagedModel.PageMetadata,
  )

  data class PrisonerContactSummaryResponse(
    val content: List<PrisonerContactSummary>,
    val page: PagedModel.PageMetadata,
  )

  data class LinkedPrisonerResponse(
    val content: List<LinkedPrisonerDetails>,
    val page: PagedModel.PageMetadata,
  )

  data class ContactIdsResponse(
    val content: List<SyncContactId>,
    val page: PagedModel.PageMetadata,
  )

  data class PrisonerRestrictionIdResponse(
    val content: List<PrisonerRestrictionId>,
    val page: PagedModel.PageMetadata,
  )

  data class PrisonerRestrictionsResponse(
    val content: List<PrisonerRestrictionDetails>,
    val page: PagedModel.PageMetadata,
  )

  fun setAuthorisationUsingCurrentUser(): (HttpHeaders) -> Unit = currentUser?.let {
    jwtAuthHelper.setAuthorisationHeader(
      username = if (it.isSystemUser) null else it.username,
      scope = listOf("read"),
      roles = it.roles,
    )
  } ?: {}
}
