package uk.gov.justice.digital.hmpps.personalrelationships.simulations

import io.gatling.javaapi.core.CoreDsl.StringBody
import io.gatling.javaapi.core.CoreDsl.constantUsersPerSec
import io.gatling.javaapi.core.CoreDsl.csv
import io.gatling.javaapi.core.CoreDsl.exec
import io.gatling.javaapi.core.CoreDsl.jsonPath
import io.gatling.javaapi.core.CoreDsl.scenario
import io.gatling.javaapi.http.HttpDsl.http
import io.gatling.javaapi.http.HttpDsl.status
import java.time.Duration.ofSeconds

class AddContactRelationshipSimulation : BaseSimulation() {

  private val feeder = csv("data/prisoner-numbers-$environment.csv").random()

  // create a contact with minimal details
  private fun createContact() = exec(
    http("Create Contact")
      .post("/contact")
      .headers(authorisationHeader)
      .body(
        StringBody(
          """
                {
                  "lastName": "Doe",
                  "firstName": "John"
                }
          """.trimIndent(),
        ),
      ).asJson()
      .check(status().`is`(201))
      .check(jsonPath("$.createdContact.id").saveAs("contactId")),
  )

  private fun postContactRelationship() = exec(
    http("Add Contact Relationship")
      .post("/prisoner-contact")
      .headers(authorisationHeader)
      .body(
        StringBody(
          """
                {
                  "contactId": #{contactId},
                  "relationship": {
                    "prisonerNumber": "#{prisonerNumber}",
                    "relationshipToPrisonerCode": "MOT",
                    "isNextOfKin": true,
                    "relationshipTypeCode": "S",
                    "isEmergencyContact": true,
                    "isApprovedVisitor": false
                  }
                }
          """.trimIndent(),
        ),
      ).asJson()
      .check(status().`is`(201)),
  )

  private fun getNextOfKinContact() = exec(
    http("Get Next Of Kin Contact")
      .get("/prisoner/#{prisonerNumber}/contact?nextOfKin=true")
      .headers(authorisationHeader)
      .check(status().`is`(200)),
  )

  private val scn = scenario("Add and Read Contact Relationship")
    .exec(getToken)
    .feed(feeder)
    .repeat(testRepeat)
    .on(
      exec(
        createContact(),
        postContactRelationship(),
        getNextOfKinContact(),
      )
        .pause(ofSeconds(testPauseRangeMin.toLong()), ofSeconds(testPauseRangeMax.toLong())),
    )

  init {
    setUp(
      scn.injectOpen(
        constantUsersPerSec(userCount.toDouble())
          .during(ofSeconds(testDuration.toLong()))
          .randomized(),
      ),
    ).protocols(httpProtocol)
      .assertions(
        responseTimePercentile3,
        successfulRequestsPercentage,
      )
  }
}
