package uk.gov.justice.digital.hmpps.personalrelationships.util

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.personalrelationships.config.User

@Service
class UserUtil {
  fun userOrDefault(username: String? = null): User = username?.let { User(it) } ?: User.SYS_USER
}
