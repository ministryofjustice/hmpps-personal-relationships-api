package uk.gov.justice.digital.hmpps.hmppscontactsapi.client.prisonersearch

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.hmppscontactsapi.client.manage.users.ManageUsersApiClient
import uk.gov.justice.digital.hmpps.hmppscontactsapi.client.manage.users.UserDetails
import uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.wiremock.ManageUsersApiMockServer

class ManageUsersApiClientTest {

  private val server = ManageUsersApiMockServer().also { it.start() }
  private val client = ManageUsersApiClient(WebClient.create("http://localhost:${server.port()}"))

  @Test
  fun `should get user`() {
    server.stubGetUser(UserDetails("USER1", "User One"))

    assertThat(client.getUserByUsername("USER1")).isEqualTo(UserDetails("USER1", "User One"))
  }

  @Test
  fun `should return null on 404`() {
    assertThat(client.getUserByUsername("USER1")).isNull()
  }

  @AfterEach
  fun after() {
    server.stop()
  }
}
