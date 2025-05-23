package uk.gov.justice.digital.hmpps.hmppscontactsapi.service

import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppscontactsapi.client.manage.users.ManageUsersApiClient
import uk.gov.justice.digital.hmpps.hmppscontactsapi.client.manage.users.UserDetails
import uk.gov.justice.digital.hmpps.hmppscontactsapi.config.CacheConfiguration

@Service
class ManageUsersService(private val manageUsersClient: ManageUsersApiClient) {
  @Cacheable(CacheConfiguration.USER_DETAILS_CACHE)
  fun getUserByUsername(username: String): UserDetails? = manageUsersClient.getUserByUsername(username)
}
