package uk.gov.justice.digital.hmpps.hmppscontactsapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppscontactsapi.client.manage.users.ManageUsersApiClient
import uk.gov.justice.digital.hmpps.hmppscontactsapi.client.manage.users.UserDetails

@Service
class ManageUsersService(private val manageUsersClient: ManageUsersApiClient) {
  fun getUserByUsername(username: String): UserDetails? = manageUsersClient.getUserByUsername(username)
}
