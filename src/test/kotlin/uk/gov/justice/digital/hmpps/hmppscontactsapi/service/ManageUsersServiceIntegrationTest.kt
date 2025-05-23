package uk.gov.justice.digital.hmpps.hmppscontactsapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.CacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import uk.gov.justice.digital.hmpps.hmppscontactsapi.client.manage.users.ManageUsersApiClient
import uk.gov.justice.digital.hmpps.hmppscontactsapi.client.manage.users.UserDetails
import uk.gov.justice.digital.hmpps.hmppscontactsapi.config.CacheConfiguration

@ExtendWith(SpringExtension::class)
@ContextConfiguration(classes = [CacheConfiguration::class, ManageUsersServiceIntegrationTest.ManageUsersServiceIntegrationTestConfiguration::class])
class ManageUsersServiceIntegrationTest {

  @Configuration
  class ManageUsersServiceIntegrationTestConfiguration {
    @Bean
    fun manageUsersService(manageUsersApiClient: ManageUsersApiClient): ManageUsersService = ManageUsersService(manageUsersApiClient)

    @Bean
    fun manageUsersApiClient(): ManageUsersApiClient = mock()
  }

  @Autowired
  private lateinit var manageUsersService: ManageUsersService

  @Autowired
  private lateinit var manageUsersApiClient: ManageUsersApiClient

  @Autowired
  private lateinit var cacheManager: CacheManager

  @Test
  fun `should cache requests for users`() {
    val user = UserDetails("FOO", "Foo User")

    whenever(manageUsersApiClient.getUserByUsername("FOO")).thenReturn(UserDetails("FOO", "Foo User"))

    assertThat(manageUsersService.getUserByUsername("FOO")).isEqualTo(user)
    assertThat(manageUsersService.getUserByUsername("FOO")).isEqualTo(user)

    verify(manageUsersApiClient, times(1)).getUserByUsername("FOO")
  }

  @Test
  fun `should cache again after eviction`() {
    val user = UserDetails("FOO", "Foo User")

    whenever(manageUsersApiClient.getUserByUsername("FOO")).thenReturn(UserDetails("FOO", "Foo User"))

    assertThat(manageUsersService.getUserByUsername("FOO")).isEqualTo(user)

    cacheManager.getCache(CacheConfiguration.USER_DETAILS_CACHE)?.clear()

    assertThat(manageUsersService.getUserByUsername("FOO")).isEqualTo(user)

    verify(manageUsersApiClient, times(2)).getUserByUsername("FOO")
  }
}
