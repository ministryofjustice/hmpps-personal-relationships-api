package uk.gov.justice.digital.hmpps.hmppscontactsapi.config

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.concurrent.ConcurrentMapCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import java.util.concurrent.TimeUnit

@Configuration
@EnableCaching
@EnableScheduling
class CacheConfiguration {
  companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
    const val USER_DETAILS_CACHE = "user_details"
    private const val USER_DETAILS_EVICT_HOURS = 12L
  }

  @Bean
  fun cacheManager(): CacheManager = ConcurrentMapCacheManager(
    USER_DETAILS_CACHE,
  )

  @CacheEvict(value = [USER_DETAILS_CACHE], allEntries = true)
  @Scheduled(fixedDelay = USER_DETAILS_EVICT_HOURS, timeUnit = TimeUnit.HOURS)
  fun cacheEvictUserDetails() {
    log.info("Evicting cache: $USER_DETAILS_CACHE after $USER_DETAILS_EVICT_HOURS hours")
  }
}
