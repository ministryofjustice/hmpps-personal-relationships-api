package uk.gov.justice.digital.hmpps.hmppscontactsapi.simulations

import io.gatling.javaapi.core.CoreDsl.constantConcurrentUsers
import io.gatling.javaapi.core.CoreDsl.csv
import io.gatling.javaapi.core.CoreDsl.exec
import io.gatling.javaapi.core.CoreDsl.jsonPath
import io.gatling.javaapi.core.CoreDsl.scenario
import io.gatling.javaapi.http.HttpDsl.http
import io.gatling.javaapi.http.HttpDsl.status
import java.time.Duration.ofSeconds

class PrisonerNumberOfChildrenSimulation : BaseSimulation() {

  private val prisonerNumbers = csv("data/prisoner-numbers-$environment.csv").random()

  private fun searchContacts() = exec(
    http("Get Prisoner Number of Children")
      .get("/prisoner/#{prisonerNumber}/number-of-children")
      .headers(authorisationHeader)
      .check(status().shouldBe(200))
      .check(jsonPath("$.id").exists().saveAs("id")),
  )

  private val getNumberOfChildren = scenario("Get Prisoner number of children")
    .exec(getToken)
    .feed(prisonerNumbers)
    .repeat(testRepeat)
    .on(
      exec(searchContacts())
        .pause(ofSeconds(testPauseRangeMin.toLong()), ofSeconds(testPauseRangeMax.toLong())),
    )

  init {
    setUp(
      getNumberOfChildren.injectClosed(
        constantConcurrentUsers(userCount.toInt()).during(testDuration.toLong()),
      ),
    ).protocols(httpProtocol)
      .assertions(
        responseTimePercentile3,
        successfulRequestsPercentage,
      )
  }
}
