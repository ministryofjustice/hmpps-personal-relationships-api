package uk.gov.justice.digital.hmpps.personalrelationships.service

import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.personalrelationships.client.manage.users.ManageUsersApiClient
import uk.gov.justice.digital.hmpps.personalrelationships.client.manage.users.UserDetails
import uk.gov.justice.digital.hmpps.personalrelationships.config.CacheConfiguration

@Service
class ManageUsersService(private val manageUsersClient: ManageUsersApiClient) {
  @Cacheable(CacheConfiguration.USER_DETAILS_CACHE)
  fun getUserByUsername(username: String): UserDetails? = manageUsersClient.getUserByUsername(username)
}
