package uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.wiremock

import com.github.tomakehurst.wiremock.client.WireMock
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import uk.gov.justice.digital.hmpps.hmppscontactsapi.client.manage.users.UserDetails

class ManageUsersApiMockServer : MockServer(8093) {

  fun stubGetUser(user: UserDetails) {
    stubFor(
      WireMock.get("/users/${user.username}")
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(
              """
                {
                  "username": "${user.username}",
                  "name": "${user.name}"
                  ${user.activeCaseLoadId?.let {", \"activeCaseLoadId\": \"${user.activeCaseLoadId}\""} ?: ""}
                }
              """.trimIndent(),
            )
            .withStatus(200),
        ),
    )
  }
}

class ManageUsersApiExtension :
  BeforeAllCallback,
  AfterAllCallback,
  BeforeEachCallback {
  companion object {
    @JvmField
    val manageUsersApiMockServer = ManageUsersApiMockServer()
  }

  override fun beforeAll(context: ExtensionContext) {
    manageUsersApiMockServer.start()
  }

  override fun beforeEach(context: ExtensionContext) {
    manageUsersApiMockServer.resetAll()
  }

  override fun afterAll(context: ExtensionContext) {
    manageUsersApiMockServer.stop()
  }
}
