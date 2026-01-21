package uk.gov.justice.digital.hmpps.personalrelationships.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder
import uk.gov.justice.digital.hmpps.personalrelationships.client.manage.users.UserDetails
import uk.gov.justice.digital.hmpps.personalrelationships.service.ManageUsersService
import uk.gov.justice.hmpps.kotlin.auth.AuthAwareAuthenticationToken

class UserRequestContextConfigurationTest {

  private val manageUsersService = mock<ManageUsersService>()
  private val userRequestContextInterceptor = UserRequestContextInterceptor(manageUsersService)

  @ParameterizedTest
  @CsvSource(
    value = [
      "PUT",
      "POST",
      "PATCH",
      "DELETE",
      "GET",
    ],
  )
  fun `should throw exception if not authenticated`(method: String) {
    SecurityContextHolder.setContext(mock { on { authentication } doReturn null })
    val req = MockHttpServletRequest(method, "http://localhost/api")
    val res = MockHttpServletResponse()
    val exception = assertThrows<org.springframework.security.access.AccessDeniedException> {
      userRequestContextInterceptor.preHandle(req, res, "")
    }
    assertThat(exception.message).isEqualTo("User is not authenticated")
    assertThat(req.getAttribute(User.REQUEST_ATTRIBUTE)).isNull()
  }

  @ParameterizedTest
  @CsvSource(
    value = [
      "PUT",
      "POST",
      "PATCH",
      "DELETE",
    ],
  )
  fun `should throw authentication error if request is modifying action with no username`(method: String) {
    setSecurityContext(null)
    val req = MockHttpServletRequest(method, "http://localhost/api")
    val res = MockHttpServletResponse()
    val exception = assertThrows<org.springframework.security.access.AccessDeniedException> {
      userRequestContextInterceptor.preHandle(req, res, "")
    }
    assertThat(exception.message).isEqualTo("Username is missing from token")
    assertThat(req.getAttribute(User.REQUEST_ATTRIBUTE)).isNull()
  }

  @ParameterizedTest
  @CsvSource(
    value = [
      "PUT",
      "POST",
      "PATCH",
      "DELETE",
    ],
  )
  fun `should enrich user with user details on modifying method`(method: String) {
    setSecurityContext("USER1")
    whenever(manageUsersService.getUserByUsername("USER1")).thenReturn(UserDetails("USER1", "User One", "BXI"))
    val req = MockHttpServletRequest(method, "http://localhost/api")
    val res = MockHttpServletResponse()

    userRequestContextInterceptor.preHandle(req, res, "")
    assertThat(req.getAttribute(User.REQUEST_ATTRIBUTE)).isEqualTo(User(username = "USER1", activeCaseLoadId = "BXI"))
  }

  @Test
  fun `should handle user not existing on manage users API`() {
    setSecurityContext("USER1")
    whenever(manageUsersService.getUserByUsername("USER1")).thenReturn(null)
    val req = MockHttpServletRequest("POST", "http://localhost/api")
    val res = MockHttpServletResponse()

    userRequestContextInterceptor.preHandle(req, res, "")
    assertThat(req.getAttribute(User.REQUEST_ATTRIBUTE)).isEqualTo(User(username = "USER1", activeCaseLoadId = null))
  }

  @Test
  fun `should handle user manage users API blowing up silently`() {
    setSecurityContext("USER1")
    whenever(manageUsersService.getUserByUsername("USER1")).thenThrow(RuntimeException("Boom!"))
    val req = MockHttpServletRequest("POST", "http://localhost/api")
    val res = MockHttpServletResponse()

    userRequestContextInterceptor.preHandle(req, res, "")
    assertThat(req.getAttribute(User.REQUEST_ATTRIBUTE)).isEqualTo(User(username = "USER1", activeCaseLoadId = null))
  }

  @Test
  fun `should not call users service for GET requests`() {
    setSecurityContext("bob")
    val req = MockHttpServletRequest("GET", "http://localhost/api")
    val res = MockHttpServletResponse()

    userRequestContextInterceptor.preHandle(req, res, "")

    assertThat(req.getAttribute(User.REQUEST_ATTRIBUTE)).isEqualTo(User("bob"))
    verify(manageUsersService, never()).getUserByUsername(any())
  }

  @Test
  fun `should allow GET requests without username`() {
    setSecurityContext(null)
    val req = MockHttpServletRequest("GET", "http://localhost/api")
    val res = MockHttpServletResponse()

    userRequestContextInterceptor.preHandle(req, res, "")

    assertThat(req.getAttribute(User.REQUEST_ATTRIBUTE)).isEqualTo(User.SYS_USER)
    verify(manageUsersService, never()).getUserByUsername(any())
  }

  private fun setSecurityContext(username: String?, clientId: String = "client-id") = mock<AuthAwareAuthenticationToken> {
    on { this.userName } doReturn username
    on { this.clientId } doReturn clientId
  }.also { token -> SecurityContextHolder.setContext(mock { on { authentication } doReturn token }) }
}
