package uk.gov.justice.digital.hmpps.personalrelationships.service

import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.personalrelationships.client.organisations.OrganisationsApiClient
import uk.gov.justice.digital.hmpps.personalrelationships.helpers.createOrganisationSummary

class OrganisationServiceTest {
  private val organisationsApiClient: OrganisationsApiClient = mock()
  private val service = OrganisationService(organisationsApiClient)

  @Test
  fun `should return the org if found in org api`() {
    val expected = createOrganisationSummary(1)
    whenever(organisationsApiClient.getOrganisationSummary(1)).thenReturn(expected)

    val result = service.getOrganisationSummaryById(1)

    assertThat(result).isEqualTo(expected)
  }

  @Test
  fun `should throw an exception if the org does not exist in org api`() {
    whenever(organisationsApiClient.getOrganisationSummary(1)).thenReturn(null)

    val exception = assertThrows<EntityNotFoundException> {
      service.getOrganisationSummaryById(1)
    }

    assertThat(exception.message).isEqualTo("Organisation with id 1 not found")
  }
}
