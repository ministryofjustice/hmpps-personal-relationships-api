package uk.gov.justice.digital.hmpps.hmppscontactsapi.integration

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.context.annotation.Import
import org.springframework.http.HttpHeaders
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppscontactsapi.client.manage.users.UserDetails
import uk.gov.justice.digital.hmpps.hmppscontactsapi.client.organisationsapi.model.OrganisationSummary
import uk.gov.justice.digital.hmpps.hmppscontactsapi.client.prisonersearch.Prisoner
import uk.gov.justice.digital.hmpps.hmppscontactsapi.helpers.createOrganisationSummary
import uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.helper.TestAPIClient
import uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.wiremock.HmppsAuthApiExtension
import uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.wiremock.HmppsAuthApiExtension.Companion.hmppsAuth
import uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.wiremock.ManageUsersApiExtension
import uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.wiremock.ManageUsersApiExtension.Companion.manageUsersApiMockServer
import uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.wiremock.OrganisationsApiExtension
import uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.wiremock.OrganisationsApiExtension.Companion.organisationsApiMockServer
import uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.wiremock.PrisonerSearchApiExtension
import uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.wiremock.PrisonerSearchApiExtension.Companion.prisonerSearchApiServer
import uk.gov.justice.digital.hmpps.hmppscontactsapi.util.StubUser
import uk.gov.justice.hmpps.test.kotlin.auth.JwtAuthorisationHelper

@ExtendWith(HmppsAuthApiExtension::class, PrisonerSearchApiExtension::class, ManageUsersApiExtension::class, OrganisationsApiExtension::class)
@SpringBootTest(webEnvironment = RANDOM_PORT)
@Import(TestConfiguration::class)
@ActiveProfiles("test")
abstract class IntegrationTestBase {

  @Autowired
  protected lateinit var webTestClient: WebTestClient

  @Autowired
  protected lateinit var jwtAuthHelper: JwtAuthorisationHelper

  @Autowired
  protected lateinit var stubEvents: StubOutboundEventsPublisher

  protected lateinit var testAPIClient: TestAPIClient

  @BeforeEach
  fun setupTestApiClient() {
    testAPIClient = TestAPIClient(webTestClient, jwtAuthHelper, null)
    stubEvents.reset()
  }

  internal fun setAuthorisation(
    username: String? = "AUTH_ADM",
    roles: List<String> = listOf(),
    scopes: List<String> = listOf("read"),
  ): (HttpHeaders) -> Unit = testAPIClient.setAuthorisation(username = username, scopes = scopes, roles = roles)

  internal fun setAuthorisationUsingCurrentUser(): (HttpHeaders) -> Unit = testAPIClient.setAuthorisationUsingCurrentUser()

  protected fun stubPingWithResponse(status: Int) {
    hmppsAuth.stubHealthPing(status)
    prisonerSearchApiServer.stubHealthPing(status)
  }

  protected fun stubPrisonSearchWithResponse(prisonerNumber: String) {
    prisonerSearchApiServer.stubGetPrisoner(prisonerNumber)
  }

  protected fun stubPrisonerSearch(prisoner: Prisoner) {
    prisonerSearchApiServer.stubGetPrisoner(prisoner)
  }

  protected fun stubSearchPrisonersByPrisonerNumbers(idsBeingSearchFor: Set<String>, prisonersToReturn: List<Prisoner>) {
    prisonerSearchApiServer.stubSearchPrisonersByPrisonerNumber(idsBeingSearchFor, prisonersToReturn)
  }

  protected fun stubPrisonSearchWithNotFoundResponse(prisonerNumber: String) {
    prisonerSearchApiServer.stubGetPrisonerReturnNoResults(prisonerNumber)
  }

  protected fun stubGetUserByUsername(user: UserDetails) {
    manageUsersApiMockServer.stubGetUser(user)
  }

  fun stubOrganisationSummary(id: Long, name: String = "Some name limited"): OrganisationSummary {
    val organisation = createOrganisationSummary(id, name)
    organisationsApiMockServer.stubOrganisationSummary(organisation)
    return organisation
  }

  fun stubOrganisationSummaryNotFound(id: Long) {
    organisationsApiMockServer.stubOrganisationSummaryNotFound(id)
  }

  fun setCurrentUser(user: StubUser?) {
    testAPIClient.currentUser = user
    if (user != null && !user.isSystemUser) {
      manageUsersApiMockServer.stubGetUser(UserDetails(username = user.username, name = user.displayName))
    }
  }

  fun <T> doWithTemporaryWritePermission(action: () -> T): T {
    val previousUser = testAPIClient.currentUser
    setCurrentUser(StubUser.READ_WRITE_USER)
    val result = action()
    setCurrentUser(previousUser)
    return result
  }
}
