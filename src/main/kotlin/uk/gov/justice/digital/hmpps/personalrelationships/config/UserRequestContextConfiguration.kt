package uk.gov.justice.digital.hmpps.personalrelationships.config

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Configuration
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.servlet.HandlerInterceptor
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import uk.gov.justice.digital.hmpps.personalrelationships.service.ManageUsersService
import uk.gov.justice.hmpps.kotlin.auth.AuthAwareAuthenticationToken

@Configuration
class UserRequestContextConfiguration(private val userRequestContextInterceptor: UserRequestContextInterceptor) : WebMvcConfigurer {
  override fun addInterceptors(registry: InterceptorRegistry) {
    registry.addInterceptor(userRequestContextInterceptor)
      .addPathPatterns("/contact/**", "/prisoner/**", "/prisoner-contact/**")
      .excludePathPatterns("/prisoner-contact/restrictions")
  }
}

@Configuration
class UserRequestContextInterceptor(private val manageUsersService: ManageUsersService) : HandlerInterceptor {
  companion object {
    private val logger = LoggerFactory.getLogger(this::class.java)
  }

  override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
    val username = authentication().userName?.trim()
    // Require a valid username for all modifying methods
    val modifyingMethods = arrayOf("POST", "PUT", "PATCH", "DELETE")
    val user = if (request.method in modifyingMethods) {
      if (username === null) {
        throw AccessDeniedException("Username is missing from token")
      }
      enrichUserIfPossible(username)
    } else {
      username?.let { User(username) } ?: User.SYS_USER
    }
    request.setAttribute(User.REQUEST_ATTRIBUTE, user)
    return true
  }

  private fun enrichUserIfPossible(username: String): User {
    val userDetails = try {
      manageUsersService.getUserByUsername(username)
    } catch (e: Exception) {
      logger.error("Unhandled exception getting user {}", username, e)
      null
    }
    return User(username = username, userDetails?.activeCaseLoadId)
  }

  private fun authentication(): AuthAwareAuthenticationToken = SecurityContextHolder.getContext().authentication as AuthAwareAuthenticationToken?
    ?: throw AccessDeniedException("User is not authenticated")
}
