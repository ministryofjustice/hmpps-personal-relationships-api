package uk.gov.justice.digital.hmpps.personalrelationships.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.CacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.junit.jupiter.SpringExtension
import uk.gov.justice.digital.hmpps.personalrelationships.client.manage.users.ManageUsersApiClient
import uk.gov.justice.digital.hmpps.personalrelationships.client.manage.users.UserDetails
import uk.gov.justice.digital.hmpps.personalrelationships.config.CacheConfiguration

@ExtendWith(SpringExtension::class)
@ContextConfiguration(classes = [CacheConfiguration::class, ManageUsersServiceIntegrationTest.TestConfig::class])
class ManageUsersServiceIntegrationTest {

  @BeforeEach
  fun setUp() {
    cacheManager.getCache(CacheConfiguration.USER_DETAILS_CACHE)?.clear()
    Mockito.reset(manageUsersApiClient)
  }

  @Autowired
  private lateinit var manageUsersService: ManageUsersService

  @Autowired
  private lateinit var cacheManager: CacheManager

  @MockitoBean
  private lateinit var manageUsersApiClient: ManageUsersApiClient

  @Configuration
  class TestConfig {
    @Bean
    fun manageUsersService(manageUsersApiClient: ManageUsersApiClient): ManageUsersService = ManageUsersService(manageUsersApiClient)
  }

  @Test
  fun `should cache requests for users`() {
    val user = UserDetails("FOO", "Foo User")
    whenever(manageUsersApiClient.getUserByUsername("FOO")).thenReturn(user)

    assertThat(manageUsersService.getUserByUsername("FOO")).isEqualTo(user)
    assertThat(manageUsersService.getUserByUsername("FOO")).isEqualTo(user)

    verify(manageUsersApiClient, times(1)).getUserByUsername("FOO")
  }

  @Test
  fun `should cache again after eviction`() {
    val user = UserDetails("FOO", "Foo User")
    whenever(manageUsersApiClient.getUserByUsername("FOO")).thenReturn(user)

    assertThat(manageUsersService.getUserByUsername("FOO")).isEqualTo(user)

    cacheManager.getCache(CacheConfiguration.USER_DETAILS_CACHE)?.clear()

    assertThat(manageUsersService.getUserByUsername("FOO")).isEqualTo(user)

    verify(manageUsersApiClient, times(2)).getUserByUsername("FOO")
  }
}
