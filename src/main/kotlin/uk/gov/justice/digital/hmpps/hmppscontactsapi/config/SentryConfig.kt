package uk.gov.justice.digital.hmpps.hmppscontactsapi.config

import io.sentry.SentryOptions
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
@Configuration
class SentryConfig {
  @Bean
  fun ignoreHealthRequests() = SentryOptions.BeforeSendTransactionCallback { transaction, _ ->
    transaction.transaction?.let { transactionName ->
      when {
        transactionName.startsWith("GET /health") -> null
        transactionName.startsWith("GET /info") -> null
        else -> transaction
      }
    } ?: transaction
  }
}
