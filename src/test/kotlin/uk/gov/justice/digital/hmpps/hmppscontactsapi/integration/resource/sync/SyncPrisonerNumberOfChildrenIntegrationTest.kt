package uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.resource.sync

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.NullSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.PostgresIntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync.SyncUpdatePrisonerNumberOfChildrenRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.sync.SyncPrisonerNumberOfChildrenResponse
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.PrisonerNumberOfChildrenRepository
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.PersonReference
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.PrisonerNumberOfChildren
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.Source
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.time.LocalDateTime

class SyncPrisonerNumberOfChildrenIntegrationTest : PostgresIntegrationTestBase() {

  private val prisonerNumber = "A1234BC"

  @Autowired
  private lateinit var numberOfChildrenRepository: PrisonerNumberOfChildrenRepository

  @BeforeEach
  fun setUp() {
    numberOfChildrenRepository.deleteAll()
  }

  @Test
  fun `Sync endpoints should return unauthorized if no token provided`() {
    webTestClient.get()
      .uri("/sync/$prisonerNumber/number-of-children")
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus()
      .isUnauthorized

    webTestClient.put()
      .uri("/sync/$prisonerNumber/number-of-children")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(aMinimalRequest())
      .exchange()
      .expectStatus()
      .isUnauthorized

    webTestClient.post()
      .uri("/sync/$prisonerNumber/number-of-children")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(aMinimalRequest())
      .exchange()
      .expectStatus()
      .isUnauthorized

    webTestClient.delete()
      .uri("/sync/$prisonerNumber/number-of-children")
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @ParameterizedTest
  @ValueSource(strings = ["ROLE_CONTACTS_ADMIN", "ROLE_CONTACTS__R", "ROLE_CONTACTS__RW"])
  fun `Sync endpoints should return forbidden without authorized role`(role: String) {
    webTestClient.get()
      .uri("/sync/$prisonerNumber/number-of-children")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf(role)))
      .exchange()
      .expectStatus()
      .isForbidden

    webTestClient.put()
      .uri("/sync/$prisonerNumber/number-of-children")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(aMinimalRequest())
      .headers(setAuthorisation(roles = listOf(role)))
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `should get an existing prisoner number of children`() {
    val numberOfChildrenToSync = SyncUpdatePrisonerNumberOfChildrenRequest(

      numberOfChildren = "1",
      createdBy = "user",
      createdTime = LocalDateTime.now(),
    )

    // When
    webTestClient.put()
      .uri("/sync/$prisonerNumber/number-of-children")
      .headers(setAuthorisation(roles = listOf("PERSONAL_RELATIONSHIPS_MIGRATION")))
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(numberOfChildrenToSync)
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(SyncPrisonerNumberOfChildrenResponse::class.java)
      .returnResult().responseBody

    val numberOfChildrenResponse = webTestClient.get()
      .uri("/sync/$prisonerNumber/number-of-children")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("PERSONAL_RELATIONSHIPS_MIGRATION")))
      .exchange()
      .expectStatus()
      .isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(SyncPrisonerNumberOfChildrenResponse::class.java)
      .returnResult().responseBody!!

    assertThat(numberOfChildrenResponse.id).isGreaterThan(0)
    assertThat(numberOfChildrenResponse.numberOfChildren).isEqualTo("1")
    assertThat(numberOfChildrenResponse.createdBy).isEqualTo("user")
    assertThat(numberOfChildrenResponse.createdTime).isNotNull
    assertThat(numberOfChildrenResponse.active).isTrue
  }

  @ParameterizedTest
  @ValueSource(strings = ["1"])
  @NullSource
  fun `should sync number of children record`(numberOfChildren: String?) {
    // Given
    val numberOfChildrenToSync = SyncUpdatePrisonerNumberOfChildrenRequest(
      numberOfChildren = numberOfChildren,
      createdBy = "user",
      createdTime = LocalDateTime.now(),
    )

    // When
    val response = webTestClient.put()
      .uri("/sync/$prisonerNumber/number-of-children")
      .headers(setAuthorisation(roles = listOf("PERSONAL_RELATIONSHIPS_MIGRATION")))
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(numberOfChildrenToSync)
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(SyncPrisonerNumberOfChildrenResponse::class.java)
      .returnResult().responseBody

    assertThat(response).isNotNull
    assertThat(response).usingRecursiveComparison()
      .ignoringFields("id", "createdTime")
      .isEqualTo(
        SyncPrisonerNumberOfChildrenResponse(
          id = 1L,
          numberOfChildren = numberOfChildren,
          createdBy = "user",
          createdTime = LocalDateTime.now(),
          active = true,
        ),
      )

    // Verify database state
    val savedNumberOfChildren = webTestClient.get()
      .uri("/sync/$prisonerNumber/number-of-children")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("PERSONAL_RELATIONSHIPS_MIGRATION")))
      .exchange()
      .expectStatus()
      .isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(SyncPrisonerNumberOfChildrenResponse::class.java)
      .returnResult().responseBody!!

    assertThat(savedNumberOfChildren.id).isGreaterThan(0)
    assertThat(savedNumberOfChildren.numberOfChildren).isEqualTo(numberOfChildren)
    assertThat(savedNumberOfChildren.createdBy).isEqualTo("user")
    assertThat(savedNumberOfChildren.createdTime).isNotNull
    assertThat(savedNumberOfChildren.active).isTrue

    // Removed duplicate assertions

    stubEvents.assertHasEvent(
      event = OutboundEvent.PRISONER_NUMBER_OF_CHILDREN_CREATED,
      additionalInfo = PrisonerNumberOfChildren(
        savedNumberOfChildren.id,
        Source.NOMIS,
      ),
      personReference = PersonReference(nomsNumber = prisonerNumber),
    )
  }

  @Test
  fun `should update existing record as inactive and create new record`() {
    // Given
    val existingNumberOfChildren = SyncUpdatePrisonerNumberOfChildrenRequest(
      numberOfChildren = "1",
      createdBy = "user",
      createdTime = LocalDateTime.now(),
    )
    val existingResponse = webTestClient.put()
      .uri("/sync/$prisonerNumber/number-of-children")
      .headers(setAuthorisation(roles = listOf("PERSONAL_RELATIONSHIPS_MIGRATION")))
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(existingNumberOfChildren)
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(SyncPrisonerNumberOfChildrenResponse::class.java)
      .returnResult().responseBody

    assertThat(existingResponse).isNotNull

    val updatedNumberOfChildren = SyncUpdatePrisonerNumberOfChildrenRequest(
      numberOfChildren = "1",
      createdBy = "user",
      createdTime = LocalDateTime.now(),
    )

    // When
    val response = webTestClient.put()
      .uri("/sync/$prisonerNumber/number-of-children")
      .headers(setAuthorisation(roles = listOf("PERSONAL_RELATIONSHIPS_MIGRATION")))
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(updatedNumberOfChildren)
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(SyncPrisonerNumberOfChildrenResponse::class.java)
      .returnResult().responseBody

    assertThat(response).isNotNull
    assertThat(response).usingRecursiveComparison()
      .ignoringFields("id", "createdBy", "createdTime")
      .isEqualTo(
        SyncPrisonerNumberOfChildrenResponse(
          id = 0,
          numberOfChildren = "1",
          createdBy = "User",
          createdTime = LocalDateTime.now(),
          active = true,
        ),
      )

    val numberOfChildren = webTestClient.get()
      .uri("/sync/$prisonerNumber/number-of-children")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("PERSONAL_RELATIONSHIPS_MIGRATION")))
      .exchange()
      .expectStatus()
      .isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(SyncPrisonerNumberOfChildrenResponse::class.java)
      .returnResult().responseBody!!

    assertThat(numberOfChildren.id).isGreaterThan(0)
    assertThat(numberOfChildren.numberOfChildren).isEqualTo("1")
    assertThat(numberOfChildren.createdBy).isEqualTo("user")
    assertThat(numberOfChildren.createdTime).isNotNull
    assertThat(numberOfChildren.active).isTrue

    val historicalRecord = numberOfChildrenRepository.findByPrisonerNumberAndActiveFalse(prisonerNumber)
    assertThat(historicalRecord[0].numberOfChildren).isEqualTo("1")
    assertThat(historicalRecord[0].createdBy).isEqualTo("user")
    stubEvents.assertHasEvent(
      event = OutboundEvent.PRISONER_NUMBER_OF_CHILDREN_UPDATED,
      additionalInfo = PrisonerNumberOfChildren(historicalRecord[0].prisonerNumberOfChildrenId, Source.NOMIS),
      personReference = PersonReference(nomsNumber = prisonerNumber),
    )

    stubEvents.assertHasEvent(
      event = OutboundEvent.PRISONER_NUMBER_OF_CHILDREN_CREATED,
      additionalInfo = PrisonerNumberOfChildren(numberOfChildren.id, Source.NOMIS),
      personReference = PersonReference(nomsNumber = prisonerNumber),
    )
  }

  @Test
  fun `sync number of children - bad request when invalid data`() {
    // Given
    val numberOfChildrenToSync = SyncUpdatePrisonerNumberOfChildrenRequest(
      numberOfChildren = "THIS TEXT IS VERY VERY LONG, LONGER THAN 50 CHARACTERS",
      createdBy = "user",
      createdTime = LocalDateTime.now(),
    )

    // When
    val response = webTestClient.put()
      .uri("/sync/$prisonerNumber/number-of-children")
      .headers(setAuthorisation(roles = listOf("PERSONAL_RELATIONSHIPS_MIGRATION")))
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(numberOfChildrenToSync)
      .exchange()
      .expectStatus().isBadRequest
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    assertThat(response!!.developerMessage).contains("numberOfChildren must be less than or equal to 50 characters")
  }

  @Test
  fun `sync number of children - success`() {
    val numberOfChildrenToSync = SyncUpdatePrisonerNumberOfChildrenRequest(
      numberOfChildren = "1",
      createdBy = "user",
      createdTime = LocalDateTime.now(),
    )

    // When
    val response = webTestClient.put()
      .uri("/sync/$prisonerNumber/number-of-children")
      .headers(setAuthorisation(roles = listOf("PERSONAL_RELATIONSHIPS_MIGRATION")))
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(numberOfChildrenToSync)
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(SyncPrisonerNumberOfChildrenResponse::class.java)
      .returnResult().responseBody

    assertThat(response).isNotNull
    assertThat(response).usingRecursiveComparison()
      .ignoringFields("id", "createdBy", "createdTime")
      .isEqualTo(
        SyncPrisonerNumberOfChildrenResponse(
          id = 0,
          numberOfChildren = "1",
          createdBy = "User",
          createdTime = LocalDateTime.now(),
          active = true,
        ),
      )

    // Verify database state
    val numberOfChildrenResponse = webTestClient.get()
      .uri("/sync/$prisonerNumber/number-of-children")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("PERSONAL_RELATIONSHIPS_MIGRATION")))
      .exchange()
      .expectStatus()
      .isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(SyncPrisonerNumberOfChildrenResponse::class.java)
      .returnResult().responseBody!!

    assertThat(numberOfChildrenResponse.id).isGreaterThan(0)
    assertThat(numberOfChildrenResponse.numberOfChildren).isEqualTo("1")
    assertThat(numberOfChildrenResponse.createdBy).isEqualTo("user")
    assertThat(numberOfChildrenResponse.createdTime).isNotNull
    assertThat(numberOfChildrenResponse.active).isTrue
  }

  private fun aMinimalRequest() = SyncUpdatePrisonerNumberOfChildrenRequest(
    numberOfChildren = "1",
    createdBy = "user",
    createdTime = LocalDateTime.now(),
  )
}
