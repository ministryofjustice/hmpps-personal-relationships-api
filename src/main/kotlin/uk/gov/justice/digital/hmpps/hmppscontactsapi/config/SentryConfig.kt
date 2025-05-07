package uk.gov.justice.digital.hmpps.hmppscontactsapi.config

import io.sentry.SentryOptions
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

private const val CONTACT_RECONCILE = "/sync/contact/\\d+/reconcile"
private const val PRISONER_NUMBER_OF_CHILDREN = "/sync/[A-Z0-9]+/number-of-children"
private const val PRISONER_DOMESTIC_STATUS = "/sync/[A-Z0-9]+/domestic-status"
private const val PRISONER_CONTACT = "/prisoner/[A-Z0-9]+/contact"

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

  @Bean
  fun ignoreEntityNotFoundExceptions() = SentryOptions.BeforeSendCallback { event, _ ->
    when {
      event.exceptions?.any { it.type == "EntityNotFoundException" } == true -> null
      else -> event
    }
  }
}
