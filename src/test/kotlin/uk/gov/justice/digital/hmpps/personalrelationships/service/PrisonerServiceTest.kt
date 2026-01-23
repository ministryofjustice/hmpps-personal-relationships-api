package uk.gov.justice.digital.hmpps.personalrelationships.service

import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.personalrelationships.client.prisonersearch.Prisoner
import uk.gov.justice.digital.hmpps.personalrelationships.client.prisonersearch.PrisonerSearchClient

class PrisonerServiceTest {
  private val prisonerSearchClient: PrisonerSearchClient = mock()
  private val service = PrisonerService(prisonerSearchClient)
  private val prisonerNumber = "A1234BC"
  private val prisoner = Prisoner(prisonerNumber, "BXI", "Brixton", "Last", "First", "Middle Names")

  @Test
  fun `check should do nothing if prisoner exists`() {
    whenever(prisonerSearchClient.getPrisoner(prisonerNumber)).thenReturn(prisoner)

    service.checkPrisonerExists(prisonerNumber)

    verify(prisonerSearchClient).getPrisoner(prisonerNumber)
  }

  @Test
  fun `check should throw exception if prisoner not found`() {
    whenever(prisonerSearchClient.getPrisoner(prisonerNumber)).thenReturn(null)

    val exception = assertThrows<EntityNotFoundException> {
      service.checkPrisonerExists(prisonerNumber)
    }

    assertThat(exception.message).isEqualTo("Prisoner not found (A1234BC)")
    verify(prisonerSearchClient).getPrisoner(prisonerNumber)
  }

  @Test
  fun `get prisoner should return a prisoner`() {
    whenever(prisonerSearchClient.getPrisoner(prisonerNumber)).thenReturn(prisoner)

    val result = service.getPrisoner(prisonerNumber)

    assertThat(result).isEqualTo(prisoner)
    verify(prisonerSearchClient).getPrisoner(prisonerNumber)
  }

  @Test
  fun `get prisoner should return null if prisoner not found`() {
    whenever(prisonerSearchClient.getPrisoner(prisonerNumber)).thenReturn(null)

    val result = service.getPrisoner(prisonerNumber)

    assertThat(result).isNull()
    verify(prisonerSearchClient).getPrisoner(prisonerNumber)
  }

  @Test
  fun `should return all prisoners`() {
    val anotherPrisonerNumber = "Z1234YX"
    val anotherPrisoner = Prisoner(anotherPrisonerNumber, "BXI", "Brixton", "Last", "First", "Middle Names")
    whenever(prisonerSearchClient.getPrisoners(setOf(prisonerNumber, anotherPrisonerNumber))).thenReturn(listOf(prisoner, anotherPrisoner))

    val result = service.getPrisoners(setOf(prisonerNumber, anotherPrisonerNumber))

    assertThat(result).isEqualTo(listOf(prisoner, anotherPrisoner))
    verify(prisonerSearchClient).getPrisoners(setOf(prisonerNumber, anotherPrisonerNumber))
  }
}
