package uk.gov.justice.digital.hmpps.hmppscontactsapi.config

import io.sentry.SentryOptions
import jakarta.servlet.http.HttpServletRequest
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
  fun transactionSampling(): SentryOptions.TracesSamplerCallback = SentryOptions.TracesSamplerCallback { context ->
    val request = context.customSamplingContext?.get("request") as? HttpServletRequest

    val uri = request?.requestURI.orEmpty()
    val method = request?.method.orEmpty()

    when {
      method == "GET" &&
        (
          uri.matches(PRISONER_CONTACT.toRegex()) ||
            uri.matches(CONTACT_RECONCILE.toRegex()) ||
            uri.matches(PRISONER_NUMBER_OF_CHILDREN.toRegex()) ||
            uri.matches(PRISONER_DOMESTIC_STATUS.toRegex())
          ) -> 0.0025

      method == "PUT" &&
        (
          uri.matches(PRISONER_NUMBER_OF_CHILDREN.toRegex()) ||
            uri.matches(PRISONER_DOMESTIC_STATUS.toRegex())
          ) -> 0.0025

      else -> 0.05
    }
  }
}
