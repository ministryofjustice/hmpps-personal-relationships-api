package uk.gov.justice.digital.hmpps.personalrelationships.simulations

import io.gatling.javaapi.core.CoreDsl.constantConcurrentUsers
import io.gatling.javaapi.core.CoreDsl.csv
import io.gatling.javaapi.core.CoreDsl.exec
import io.gatling.javaapi.core.CoreDsl.jsonPath
import io.gatling.javaapi.core.CoreDsl.scenario
import io.gatling.javaapi.http.HttpDsl.http
import io.gatling.javaapi.http.HttpDsl.status
import java.time.Duration.ofSeconds

class GetPrisonerContactSimulation : BaseSimulation() {

  private val prisonerNumbers = csv("data/prisoner-numbers-$environment.csv").random()

  private fun searchContacts() = exec(
    http("Get Prisoner Contacts")
      .get("/prisoner/#{prisonerNumber}/contact")
      .headers(authorisationHeader)
      .check(status().shouldBe(200))
      .check(jsonPath("$.page.size").exists().saveAs("pageSize")),
  )

  private fun getContactsCount() = exec(
    http("Get Prisoner Contacts Count")
      .get("/prisoner/#{prisonerNumber}/contact/count")
      .headers(authorisationHeader)
      .check(status().shouldBe(200))
      .check(jsonPath("$.social").exists()),
  )

  private val getContacts = scenario("Get Prisoner contacts paginated")
    .exec(getToken)
    .feed(prisonerNumbers)
    .repeat(testRepeat)
    .on(
      exec(
        getContactsCount(),
        searchContacts(),
      )
        .pause(ofSeconds(testPauseRangeMin.toLong()), ofSeconds(testPauseRangeMax.toLong())),
    )

  init {
    setUp(
      getContacts.injectClosed(
        constantConcurrentUsers(userCount.toInt()).during(testDuration.toLong()),
      ),
    ).protocols(httpProtocol)
      .assertions(
        responseTimePercentile3,
        successfulRequestsPercentage,
      )
  }
}
