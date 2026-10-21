package uk.gov.justice.digital.hmpps.personalrelationships.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder
import uk.gov.justice.hmpps.kotlin.auth.AuthAwareAuthenticationToken

class UserRequestContextConfigurationTest {

  private val userRequestContextInterceptor = UserRequestContextInterceptor()

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
  fun `should use authenticated username on modifying method`(method: String) {
    setSecurityContext(" USER1 ")
    val req = MockHttpServletRequest(method, "http://localhost/api")
    val res = MockHttpServletResponse()

    userRequestContextInterceptor.preHandle(req, res, "")
    assertThat(req.getAttribute(User.REQUEST_ATTRIBUTE)).isEqualTo(User(username = "USER1"))
  }

  @Test
  fun `should use authenticated username for GET requests`() {
    setSecurityContext("bob")
    val req = MockHttpServletRequest("GET", "http://localhost/api")
    val res = MockHttpServletResponse()

    userRequestContextInterceptor.preHandle(req, res, "")

    assertThat(req.getAttribute(User.REQUEST_ATTRIBUTE)).isEqualTo(User("bob"))
  }

  @Test
  fun `should allow GET requests without username`() {
    setSecurityContext(null)
    val req = MockHttpServletRequest("GET", "http://localhost/api")
    val res = MockHttpServletResponse()

    userRequestContextInterceptor.preHandle(req, res, "")

    assertThat(req.getAttribute(User.REQUEST_ATTRIBUTE)).isEqualTo(User.SYS_USER)
  }

  private fun setSecurityContext(username: String?, clientId: String = "client-id") = mock<AuthAwareAuthenticationToken> {
    on { this.userName } doReturn username
    on { this.clientId } doReturn clientId
  }.also { token -> SecurityContextHolder.setContext(mock { on { authentication } doReturn token }) }
}
