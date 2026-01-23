package uk.gov.justice.digital.hmpps.personalrelationships

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class HmppsPersonalRelationshipsApi

fun main(args: Array<String>) {
  runApplication<HmppsPersonalRelationshipsApi>(*args)
}
