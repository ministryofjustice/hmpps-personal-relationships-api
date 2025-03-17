package uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.EmploymentEntity
import uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.SecureAPIIntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.CreateContactRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.employment.Employment
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.employment.PatchEmploymentsRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.employment.PatchEmploymentsUpdateEmployment
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.EmploymentRepository
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.EmploymentInfo
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.PersonReference
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.Source
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.time.LocalDateTime

class PatchEmploymentsIntegrationTest : SecureAPIIntegrationTestBase() {
  private var savedContactId = 0L

  override val allowedRoles: Set<String> = setOf("ROLE_CONTACTS_ADMIN", "ROLE_CONTACTS__RW")

  @Autowired
  private lateinit var employmentRepository: EmploymentRepository

  private lateinit var employmentToRemainUntouched: EmploymentEntity
  private lateinit var employmentToBeUpdated: EmploymentEntity
  private lateinit var employmentToBeDeleted: EmploymentEntity

  @BeforeEach
  fun setUp() {
    savedContactId = testAPIClient.createAContact(
      CreateContactRequest(
        lastName = "employments",
        firstName = "has",
        createdBy = "created",
      ),
    ).id
    stubOrganisationSummary(123)
    stubOrganisationSummary(456)
    stubOrganisationSummary(789)
    stubOrganisationSummary(555)
    stubOrganisationSummary(999)
    employmentToRemainUntouched = employmentRepository.saveAndFlush(
      EmploymentEntity(
        employmentId = 0,
        organisationId = 123,
        contactId = savedContactId,
        active = true,
        createdBy = "UNTOUCHABLE",
        createdTime = LocalDateTime.now(),
        updatedBy = null,
        updatedTime = null,
      ),
    )
    employmentToBeUpdated = employmentRepository.saveAndFlush(
      EmploymentEntity(
        employmentId = 0,
        organisationId = 456,
        contactId = savedContactId,
        active = true,
        createdBy = "TO_BE_UPDATED",
        createdTime = LocalDateTime.now(),
        updatedBy = null,
        updatedTime = null,
      ),
    )
    employmentToBeDeleted = employmentRepository.saveAndFlush(
      EmploymentEntity(
        employmentId = 0,
        organisationId = 789,
        contactId = savedContactId,
        active = true,
        createdBy = "TO_BE_DELETED",
        createdTime = LocalDateTime.now(),
        updatedBy = null,
        updatedTime = null,
      ),
    )
  }

  override fun baseRequestBuilder(): WebTestClient.RequestHeadersSpec<*> = webTestClient.patch()
    .uri("/contact/123456/employment")
    .accept(MediaType.APPLICATION_JSON)
    .contentType(MediaType.APPLICATION_JSON)
    .bodyValue(aMinimalRequest())

  @Test
  fun `should return bad request when request is empty`() {
    webTestClient.patch()
      .uri("/contact/$savedContactId/employment")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CONTACTS_ADMIN")))
      .bodyValue(
        """{
                  }""",
      )
      .exchange()
      .expectStatus()
      .isBadRequest
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody!!
  }

  @Test
  fun `should return 404 when contact does not exist`() {
    webTestClient.patch()
      .uri("/contact/-999/employment")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CONTACTS_ADMIN")))
      .bodyValue(aMinimalRequest())
      .exchange()
      .expectStatus()
      .isNotFound
  }

  @Test
  fun `Should leave employments untouched if no changes requested`() {
    val request = aMinimalRequest()
    val employmentsAfterUpdate = testAPIClient.patchEmployments(savedContactId, request)
    assertThat(employmentsAfterUpdate).hasSize(3)
    assertThat(employmentsAfterUpdate).extracting("employmentId").containsExactlyInAnyOrder(
      employmentToRemainUntouched.employmentId,
      employmentToBeUpdated.employmentId,
      employmentToBeDeleted.employmentId,
    )
    stubEvents.assertHasNoEvents(event = OutboundEvent.EMPLOYMENT_CREATED)
    stubEvents.assertHasNoEvents(event = OutboundEvent.EMPLOYMENT_UPDATED)
    stubEvents.assertHasNoEvents(event = OutboundEvent.EMPLOYMENT_DELETED)
  }

  @ParameterizedTest
  @CsvSource(
    value = [
      "requestedBy must not be null;{\"createEmployments\":[], \"updateEmployments\":[], \"deleteEmployments\":[], \"requestedBy\": null}",
      "createEmployments must not be null;{\"createEmployments\":null, \"updateEmployments\":[], \"deleteEmployments\":[], \"requestedBy\": \"USER\"}",
      "updateEmployments must not be null;{\"createEmployments\":[], \"updateEmployments\":null, \"deleteEmployments\":[], \"requestedBy\": \"USER\"}",
      "deleteEmployments must not be null;{\"createEmployments\":[], \"updateEmployments\":[], \"deleteEmployments\":null, \"requestedBy\": \"USER\"}",
      "createEmployments[0].organisationId must not be null;{\"createEmployments\":[{\"organisationId\": null, \"isActive\": true}], \"updateEmployments\":[], \"deleteEmployments\":[], \"requestedBy\": \"USER\"}",
      "createEmployments[0].isActive must not be null;{\"createEmployments\":[{\"organisationId\": 99, \"isActive\": null}], \"updateEmployments\":[], \"deleteEmployments\":[], \"requestedBy\": \"USER\"}",
      "updateEmployments[0].employmentId must not be null;{\"updateEmployments\":[{\"employmentId\": null, \"organisationId\": 99, \"isActive\": true}], \"createEmployments\":[], \"deleteEmployments\":[], \"requestedBy\": \"USER\"}",
      "updateEmployments[0].organisationId must not be null;{\"updateEmployments\":[{\"employmentId\": 123, \"organisationId\": null, \"isActive\": true}], \"createEmployments\":[], \"deleteEmployments\":[], \"requestedBy\": \"USER\"}",
      "updateEmployments[0].isActive must not be null;{\"updateEmployments\":[{\"employmentId\": 123, \"organisationId\": 99, \"isActive\": null}], \"createEmployments\":[], \"deleteEmployments\":[], \"requestedBy\": \"USER\"}",
    ],
    delimiter = ';',
  )
  fun `should return bad request when required fields are missing is empty`(expectedMessage: String, bodyValue: String) {
    val error = webTestClient.patch()
      .uri("/contact/$savedContactId/employment")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CONTACTS_ADMIN")))
      .bodyValue(bodyValue)
      .exchange()
      .expectStatus()
      .isBadRequest
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody!!
    assertThat(error.userMessage).isEqualTo("Validation failure: $expectedMessage")
  }

  @Test
  fun `Should be able to add, edit and delete employments in one go`() {
    val request = aMinimalRequest().copy(
      createEmployments = listOf(Employment(organisationId = 999, isActive = true)),
      updateEmployments = listOf(PatchEmploymentsUpdateEmployment(employmentId = employmentToBeUpdated.employmentId, organisationId = 555, isActive = false)),
      deleteEmployments = listOf(employmentToBeDeleted.employmentId),
    )
    val employmentsAfterUpdate = testAPIClient.patchEmployments(savedContactId, request)
    assertThat(employmentsAfterUpdate).hasSize(3)
    assertThat(employmentsAfterUpdate).extracting("employmentId").containsAll(
      listOf(
        employmentToRemainUntouched.employmentId,
        employmentToBeUpdated.employmentId,
      ),
    )
    with(employmentsAfterUpdate.find { it.employmentId == employmentToRemainUntouched.employmentId }!!) {
      assertThat(employer.organisationId).isEqualTo(123)
      assertThat(createdBy).isEqualTo("UNTOUCHABLE")
      assertThat(isActive).isTrue()
      assertThat(updatedBy).isNull()
      assertThat(updatedTime).isNull()
    }
    with(employmentsAfterUpdate.find { it.employmentId == employmentToBeUpdated.employmentId }!!) {
      assertThat(employer.organisationId).isEqualTo(555)
      assertThat(createdBy).isEqualTo("TO_BE_UPDATED")
      assertThat(isActive).isFalse()
      assertThat(updatedBy).isEqualTo("REQUESTED")
      assertThat(updatedTime).isNotNull()
    }
    val newEmployment = employmentsAfterUpdate.find { it.employer.organisationId == 999L }!!
    with(newEmployment) {
      assertThat(employer.organisationId).isEqualTo(999)
      assertThat(isActive).isTrue()
      assertThat(createdBy).isEqualTo("REQUESTED")
      assertThat(createdTime).isNotNull()
      assertThat(updatedBy).isNull()
      assertThat(updatedTime).isNull()
    }
    stubEvents.assertHasEvent(
      event = OutboundEvent.EMPLOYMENT_CREATED,
      additionalInfo = EmploymentInfo(newEmployment.employmentId, source = Source.DPS),
      personReference = PersonReference(dpsContactId = savedContactId),
    )
    stubEvents.assertHasEvent(
      event = OutboundEvent.EMPLOYMENT_UPDATED,
      additionalInfo = EmploymentInfo(employmentToBeUpdated.employmentId, source = Source.DPS),
      personReference = PersonReference(dpsContactId = savedContactId),
    )
    stubEvents.assertHasEvent(
      event = OutboundEvent.EMPLOYMENT_DELETED,
      additionalInfo = EmploymentInfo(employmentToBeDeleted.employmentId, source = Source.DPS),
      personReference = PersonReference(dpsContactId = savedContactId),
    )
    stubEvents.assertHasNoEvents(
      event = OutboundEvent.EMPLOYMENT_UPDATED,
      additionalInfo = EmploymentInfo(employmentToRemainUntouched.employmentId, source = Source.DPS),
    )
  }

  @Test
  fun `Should be able to add inactive employments`() {
    val request = aMinimalRequest().copy(
      createEmployments = listOf(Employment(organisationId = 999, isActive = false)),
    )
    val employmentsAfterUpdate = testAPIClient.patchEmployments(savedContactId, request)
    with(employmentsAfterUpdate.find { it.employer.organisationId == 999L }!!) {
      assertThat(employer.organisationId).isEqualTo(999)
      assertThat(isActive).isFalse()
      assertThat(createdBy).isEqualTo("REQUESTED")
      assertThat(createdTime).isNotNull()
      assertThat(updatedBy).isNull()
      assertThat(updatedTime).isNull()
    }
  }

  @Test
  fun `Should not be able to add employment for org that doesn't exist`() {
    stubOrganisationSummaryNotFound(987654321)
    val request = aMinimalRequest().copy(
      createEmployments = listOf(Employment(organisationId = 987654321, isActive = false)),
    )
    val error = webTestClient.patch()
      .uri("/contact/$savedContactId/employment")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CONTACTS_ADMIN")))
      .bodyValue(request)
      .exchange()
      .expectStatus()
      .isNotFound
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody!!
    assertThat(error.developerMessage).isEqualTo("Organisation with id 987654321 not found")
  }

  @Test
  fun `Should not be able to update employment to org that doesn't exist`() {
    val request = aMinimalRequest().copy(
      updateEmployments = listOf(PatchEmploymentsUpdateEmployment(employmentId = employmentToBeUpdated.employmentId, organisationId = 987654321, isActive = false)),
    )
    val error = webTestClient.patch()
      .uri("/contact/$savedContactId/employment")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CONTACTS_ADMIN")))
      .bodyValue(request)
      .exchange()
      .expectStatus()
      .isNotFound
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody!!
    assertThat(error.developerMessage).isEqualTo("Organisation with id 987654321 not found")
  }

  @Test
  fun `Should be atomic so if delete fails then no other updates are applied`() {
    val request = aMinimalRequest().copy(
      createEmployments = listOf(Employment(organisationId = 999, isActive = true)),
      updateEmployments = listOf(
        PatchEmploymentsUpdateEmployment(
          employmentId = employmentToBeUpdated.employmentId,
          organisationId = 555,
          isActive = false,
        ),
      ),
      deleteEmployments = listOf(-456),
    )
    // employment id -456 doesn't exist so get an error
    webTestClient.patch()
      .uri("/contact/$savedContactId/employment")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CONTACTS_ADMIN")))
      .bodyValue(request)
      .exchange()
      .expectStatus()
      .isNotFound

    val contact = testAPIClient.getContact(savedContactId)
    val employmentsAfterUpdate = contact.employments
    assertThat(employmentsAfterUpdate).hasSize(3)
    assertThat(employmentsAfterUpdate).extracting("employmentId").containsAll(
      listOf(
        employmentToRemainUntouched.employmentId,
        employmentToBeUpdated.employmentId,
        employmentToBeDeleted.employmentId,
      ),
    )
    with(employmentsAfterUpdate.find { it.employmentId == employmentToBeUpdated.employmentId }!!) {
      assertThat(employer.organisationId).isEqualTo(456)
      assertThat(createdBy).isEqualTo("TO_BE_UPDATED")
      assertThat(isActive).isTrue()
      assertThat(updatedBy).isNull()
      assertThat(updatedTime).isNull()
    }

    stubEvents.assertHasNoEvents(event = OutboundEvent.EMPLOYMENT_CREATED)
    stubEvents.assertHasNoEvents(event = OutboundEvent.EMPLOYMENT_UPDATED)
    stubEvents.assertHasNoEvents(event = OutboundEvent.EMPLOYMENT_DELETED)
  }

  private fun aMinimalRequest() = PatchEmploymentsRequest(
    createEmployments = emptyList(),
    updateEmployments = emptyList(),
    deleteEmployments = emptyList(),
    requestedBy = "REQUESTED",
  )
}
