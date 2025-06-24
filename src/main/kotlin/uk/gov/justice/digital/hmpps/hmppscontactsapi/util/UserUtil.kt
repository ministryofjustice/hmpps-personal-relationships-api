package uk.gov.justice.digital.hmpps.hmppscontactsapi.util

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppscontactsapi.config.User
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.ManageUsersService

@Service
class UserUtil(private val manageUsersService: ManageUsersService) {
  companion object {
    private val logger = LoggerFactory.getLogger(this::class.java)
  }

  fun userOrDefault(username: String? = null): User = username?.let { enrichIfPossible(username) } ?: User.SYS_USER

  private fun enrichIfPossible(username: String): User {
    val userDetails = try {
      manageUsersService.getUserByUsername(username)
    } catch (e: Exception) {
      logger.error("Unhandled exception getting user {}", username, e)
      null
    }
    return User(username, userDetails?.activeCaseLoadId)
  }
}
