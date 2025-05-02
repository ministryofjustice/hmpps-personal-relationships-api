package uk.gov.justice.digital.hmpps.hmppscontactsapi.simulations

import io.gatling.javaapi.core.CoreDsl.*
import io.gatling.javaapi.http.HttpDsl.http
import io.gatling.javaapi.http.HttpDsl.status
import java.time.Duration.ofSeconds

class GetPrisonerContactSimulation : BaseSimulation() {

    private val personIdentifiers = csv("data/person-identifiers-${environment}.csv").random()

    private fun searchContacts() = exec(
        http("Get Prisoner Contacts")
            .get("/prisoner/#{personIdentifier}/contact")
            .headers(authorisationHeader)
            .check(status().shouldBe(200))
            .check(jsonPath("$.page.size").exists().saveAs("pageSize")),
    )

    private val getContacts = scenario("Get Prisoner contacts paginated")
        .exec(getToken)
        .feed(personIdentifiers)
        .repeat(testRepeat)
        .on(
            exec(searchContacts())
                .pause(ofSeconds(testPauseRangeMin.toLong()), ofSeconds(testPauseRangeMax.toLong()))
        )

    init {
        setUp(
            getContacts.injectClosed(
                constantConcurrentUsers(userCount.toInt()).during(testDuration.toLong())
            )
        ).protocols(httpProtocol)
            .assertions(
                global().responseTime().percentile3().lt(1000),
                global().successfulRequests().percent().gt(95.0)
            )
    }
}