package uk.gov.justice.digital.hmpps.hmppscontactsapi.resource.migrate

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.migrate.DomesticStatusDetailsRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.migrate.MigratePrisonerDomesticStatusRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.migrate.DomesticStatusDetailsResponse
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.migrate.PrisonerDomesticStatusMigrationResponse
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.migrate.PrisonerDomesticStatusMigrationService
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
class MigratePrisonerDomesticStatusControllerTest {

  @Mock
  private lateinit var migrationService: PrisonerDomesticStatusMigrationService

  private lateinit var controller: MigratePrisonerDomesticStatusController

  @BeforeEach
  fun setUp() {
    controller = MigratePrisonerDomesticStatusController(migrationService)
  }

  @Nested
  @DisplayName("migrateDomesticStatus")
  inner class MigrateDomesticStatus {

    @Test
    fun `should call migration service with request and return response`() {
      // Given
      val request = MigratePrisonerDomesticStatusRequest(
        prisonerNumber = "A1234BC",
        current = DomesticStatusDetailsRequest(
          domesticStatusCode = "MARRIED",
          createdBy = "XXXXX",
          createdTime = LocalDateTime.now(),
        ),
        history = listOf(
          DomesticStatusDetailsRequest(
            domesticStatusCode = "DIVORCED",
            createdBy = "XXXXX",
            createdTime = LocalDateTime.now(),
          ),
        ),

      )

      val expectedResponse = PrisonerDomesticStatusMigrationResponse(
        prisonerNumber = "A1234BC",
        current = DomesticStatusDetailsResponse(
          id = 1,
          domesticStatusCode = "MARRIED",
          createdBy = "XXXXX",
          createdTime = LocalDateTime.now(),
        ),
        history = listOf(
          DomesticStatusDetailsResponse(
            id = 1,
            domesticStatusCode = "DIVORCED",
            createdBy = "XXXXX",
            createdTime = LocalDateTime.now(),
          ),
        ),

      )

      whenever(migrationService.migrateDomesticStatus(request)).thenReturn(expectedResponse)

      // When
      val result = controller.migrateDomesticStatus(request)

      // Then
      verify(migrationService).migrateDomesticStatus(request)
      assertThat(result).isEqualTo(expectedResponse)
    }

    @Test
    fun `should pass through service response when migration is unsuccessful`() {
      // Given
      val request = MigratePrisonerDomesticStatusRequest(
        prisonerNumber = "A1234BC",
        current = DomesticStatusDetailsRequest(
          domesticStatusCode = "MARRIED",
          createdBy = "XXXXX",
          createdTime = LocalDateTime.now(),
        ),
        history = listOf(
          DomesticStatusDetailsRequest(
            domesticStatusCode = "DIVORCED",
            createdBy = "XXXXX",
            createdTime = LocalDateTime.now(),
          ),
        ),

      )

      val expectedResponse = PrisonerDomesticStatusMigrationResponse(
        prisonerNumber = "A1234BC",
        current = DomesticStatusDetailsResponse(
          id = 1,
          domesticStatusCode = "MARRIED",
          createdBy = "XXXXX",
          createdTime = LocalDateTime.now(),
        ),
        history = listOf(
          DomesticStatusDetailsResponse(
            id = 1,
            domesticStatusCode = "DIVORCED",
            createdBy = "XXXXX",
            createdTime = LocalDateTime.now(),
          ),
        ),

      )

      whenever(migrationService.migrateDomesticStatus(request)).thenReturn(expectedResponse)

      // When
      val result = controller.migrateDomesticStatus(request)

      // Then
      verify(migrationService).migrateDomesticStatus(request)
      assertThat(result).isEqualTo(expectedResponse)
    }
  }
}
