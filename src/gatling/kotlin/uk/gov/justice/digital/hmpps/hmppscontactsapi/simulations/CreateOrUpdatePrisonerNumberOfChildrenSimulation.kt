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

class CreateOrUpdatePrisonerNumberOfChildrenSimulation : BaseSimulation() {

  private val prisonerNumberFeeder = csv("data/prisoner-numbers-$environment.csv").random()

  private fun callGetNumberOfChildren() = exec(
    http("Get Prisoner Number of Children")
      .get("/prisoner/#{prisonerNumber}/number-of-children")
      .headers(authorisationHeader)
      .check(status().shouldBe(200))
      .check(jsonPath("$.id").exists().saveAs("id")),
  )
  private fun callCreateOrUpdateNumberOfChildren() = exec(
    http("Create or Update Prisoner Number of Children")
      .put("/prisoner/#{prisonerNumber}/number-of-children")
      .body(
        StringBody {
          """
          {
            "numberOfChildren": 2
          }
          """.trimIndent()
        },
      ).asJson()
      .headers(authorisationHeader)
      .check(status().`in`(200, 409)),
  )

  private val putNumberOfChildren = scenario("Create or Update Prisoner number of children")
    .exec(getToken)
    .feed(prisonerNumberFeeder.circular())
    .repeat(testRepeat)
    .on(
      exec(
        callCreateOrUpdateNumberOfChildren(),
        callGetNumberOfChildren(),
      )
        .pause(ofSeconds(testPauseRangeMin.toLong()), ofSeconds(testPauseRangeMax.toLong())),
    )

  init {
    setUp(
      putNumberOfChildren.injectOpen(
        constantUsersPerSec(userCount.toDouble()).during(testDuration.toLong()),
      ),
    ).protocols(httpProtocol)
      .assertions(
        responseTimePercentile3,
        successfulRequestsPercentage,
      )
  }
}
