package uk.gov.justice.digital.hmpps.hmppscontactsapi.simulations

import io.gatling.javaapi.core.Simulation
import io.gatling.javaapi.http.HttpDsl.status
import java.lang.System.getenv
import io.gatling.javaapi.core.CoreDsl.*
import io.gatling.javaapi.http.HttpDsl.http

/**
 * Base simulation class containing common configuration and utilities for all simulations.
 */
open class BaseSimulation : Simulation() {

    // Number of users to simulate
    protected val userCount = Integer.getInteger("userCount", 10)

    // Duration of the test in seconds
    protected val testDuration = Integer.getInteger("testDuration", 60)
    
    // Attach a pause computed randomly between 2 values – the pause minimum
    protected val testPauseRangeMin = Integer.getInteger("testPauseRangeMin", 3)

    // Attach a pause computed randomly between 2 values – the pause maximum
    protected val testPauseRangeMax = Integer.getInteger("testPauseRangeMax", 5)
    
    // Number of times test to repeat
    protected val testRepeat = Integer.getInteger("testRepeat", 5)

    // Environment to load test data
    protected val environment = getenv("environment") ?: "dev"

    // HTTP protocol configuration
    protected val httpProtocol = http.baseUrl(getenv("BASE_URL") ?: "http://localhost:8080")
        .acceptHeader("application/json")
        .acceptEncodingHeader("gzip, deflate")
        .contentTypeHeader("application/json")

    // Authorization header for API requests
    protected val authorisationHeader = mapOf("Authorization" to "Bearer #{authToken}")

    // Get token execution step
    protected val getToken = exec(
        exec { session -> getenv("AUTH_TOKEN")?.let { session.set("authToken", it) } ?: session }
            .doIf { it.getString("authToken").isNullOrBlank() }
            .then(
                http("Get Auth Token")
                    .post(getenv("AUTH_URL"))
                    .queryParam("grant_type", "client_credentials")
                    .basicAuth(getenv("CLIENT_ID"), getenv("CLIENT_SECRET"))
                    .check(status().shouldBe(200), jsonPath("$.access_token").exists().saveAs("authToken")),
            )
    )
}