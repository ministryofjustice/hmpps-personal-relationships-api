package uk.gov.justice.digital.hmpps.hmppscontactsapi.simulations

import io.gatling.javaapi.core.CoreDsl.*
import io.gatling.javaapi.http.HttpDsl.*
import java.time.Duration.ofSeconds

class PrisonerNumberOfChildrenSimulation : BaseSimulation() {


    private val personIdentifiers = csv("data/person-identifiers-${environment}.csv").random()

    private fun searchContacts() = exec(
        http("Get Prisoner Number of Children")
            .get("/prisoner/#{personIdentifier}/number-of-children")
            .headers(authorisationHeader)
            .check(status().shouldBe(200))
            .check(jsonPath("$.id").exists().saveAs("id")),
    )

    private val getContacts = scenario("Get Prisoner number of children paginated")
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