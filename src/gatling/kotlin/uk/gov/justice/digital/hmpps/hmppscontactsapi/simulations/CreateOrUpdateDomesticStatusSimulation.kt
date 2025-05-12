package uk.gov.justice.digital.hmpps.hmppscontactsapi.simulations

import io.gatling.javaapi.core.CoreDsl.StringBody
import io.gatling.javaapi.core.CoreDsl.constantUsersPerSec
import io.gatling.javaapi.core.CoreDsl.csv
import io.gatling.javaapi.core.CoreDsl.exec
import io.gatling.javaapi.core.CoreDsl.jsonPath
import io.gatling.javaapi.core.CoreDsl.scenario
import io.gatling.javaapi.http.HttpDsl.http
import io.gatling.javaapi.http.HttpDsl.status
import java.time.Duration.ofSeconds

class CreateOrUpdateDomesticStatusSimulation : BaseSimulation() {

  private val prisonerNumberFeeder = csv("data/prisoner-numbers-$environment.csv").random()

  private fun callGetDomesticStatus() = exec(
    http("Get Prisoner Domestic Status")
      .get("/prisoner/#{prisonerNumber}/domestic-status")
      .headers(authorisationHeader)
      .check(status().shouldBe(200))
      .check(jsonPath("$.id").exists().saveAs("id")),
  )

  private fun callCreateOrUpdateDomesticStatus() = exec(
    http("Create or Update Prisoner Domestic Status")
      .put("/prisoner/#{prisonerNumber}/domestic-status")
      .body(
        StringBody {
          """
          {
            "domesticStatus": "M"
          }
          """.trimIndent()
        },
      ).asJson()
      .headers(authorisationHeader)
      .check(status().shouldBe(200)),
  )

  private val domesticStatusScenario = scenario("Create or Update Prisoner Domestic Status")
    .exec(getToken)
    .feed(prisonerNumberFeeder.circular())
    .repeat(testRepeat)
    .on(
      exec(
        callCreateOrUpdateDomesticStatus(),
        callGetDomesticStatus(),
      )
        .pause(ofSeconds(testPauseRangeMin.toLong()), ofSeconds(testPauseRangeMax.toLong())),
    )

  // Initialize the simulation configuration
  init {
    setUp(
      domesticStatusScenario.injectOpen(
        constantUsersPerSec(userCount.toDouble()).during(ofSeconds(testDuration.toLong())),
      ),
    ).protocols(httpProtocol)
      .assertions(
        responseTimePercentile3,
        successfulRequestsPercentage,
      )
  }
}
