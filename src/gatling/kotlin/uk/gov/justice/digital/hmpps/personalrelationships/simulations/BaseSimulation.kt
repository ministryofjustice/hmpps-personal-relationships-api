package uk.gov.justice.digital.hmpps.personalrelationships.simulations

import io.gatling.javaapi.core.CoreDsl.exec
import io.gatling.javaapi.core.CoreDsl.global
import io.gatling.javaapi.core.Simulation
import io.gatling.javaapi.http.HttpDsl.http
import java.lang.System.getenv

/**
 * Base simulation class containing common configuration and utilities for all simulations.
 */
open class BaseSimulation : Simulation() {
  // Number of users to simulate
  protected val userCount = Integer.getInteger("userCount", 10)

  // Duration of the test in seconds
  protected val testDuration = Integer.getInteger("testDuration", 60)

  // Minimum pause duration (in seconds) between simulated user actions
  protected val testPauseRangeMin = Integer.getInteger("testPauseRangeMin", 3)

  // Maximum pause duration (in seconds) between simulated user actions
  protected val testPauseRangeMax = Integer.getInteger("testPauseRangeMax", 5)

  // Number of iterations to repeat the test scenario
  protected val testRepeat = Integer.getInteger("testRepeat", 5)

  // Target environment for the load test (defaults to "dev")
  protected val environment = getenv("environment") ?: "dev"

  // Response time threshold (in milliseconds) that 99.9% of requests should be under
  protected val responseTimePercentile3 = global().responseTime().percentile3().lt(Integer.getInteger("responseTimePercentile3", 1000))

  // Minimum percentage of requests that should be successful for the test to pass
  protected val successfulRequestsPercentage = global().successfulRequests().percent().gt(Integer.getInteger("successfulRequestsPercentage", 95).toDouble())

  // Base HTTP configuration including default headers and base URL
  protected val httpProtocol = http.baseUrl(getenv("BASE_URL") ?: "http://localhost:8080")
    .acceptHeader("application/json")
    .acceptEncodingHeader("gzip, deflate")
    .contentTypeHeader("application/json")

  // Authorization header for API requests
  protected val authorisationHeader = mapOf("Authorization" to "Bearer #{authToken}")

  // Get token execution step
  protected val getToken = exec(
    exec { session -> getenv("AUTH_TOKEN")?.let { session.set("authToken", it) } ?: session },
  )
}
