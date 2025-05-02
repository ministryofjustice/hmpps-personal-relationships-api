package uk.gov.justice.digital.hmpps.hmppscontactsapi.simulations

import io.gatling.javaapi.core.CoreDsl.*
import io.gatling.javaapi.http.HttpDsl.*
import java.time.Duration.ofSeconds

class PrisonerDomesticStatusSimulation : BaseSimulation() {


    private val personIdentifiers = csv("data/person-identifiers-${environment}.csv").random()

    private fun searchContacts() = exec(
        http("Get Prisoner Domestic Status")
            .get("/prisoner/#{personIdentifier}/domestic-status")
            .headers(authorisationHeader)
            .check(status().shouldBe(200))
            .check(jsonPath("$.id").exists().saveAs("id")),
    )

    private val getContacts = scenario("Get Prisoner Domestic Status paginated")
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