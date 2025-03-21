package uk.gov.justice.digital.hmpps.hmppscontactsapi.mapping

import jakarta.validation.ValidationException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.ContactWithAddressEntity
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.PrisonerContactSummaryEntity
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.ContactSearchResultItem
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.PrisonerContactSummary
import kotlin.reflect.full.memberProperties

class MapSortPropertiesKtTest {

  @Test
  fun `can map contact search result item fields to contact with address entity fields`() {
    ContactSearchResultItem::class.memberProperties.forEach { property ->
      val mapped = mapSortPropertiesOfContactSearch(property.name)
      assertThat(mapped).isNotNull()
      assertThat(ContactWithAddressEntity::class.memberProperties.find { it.name == mapped }).isNotNull()
    }
  }

  @Test
  fun `attempting to sort on an invalid field for contact search gives an error`() {
    val expected = assertThrows<ValidationException> {
      mapSortPropertiesOfContactSearch("foo")
    }
    assertThat(expected.message).isEqualTo("Unable to sort on foo")
  }

  @Test
  fun `can map prisoner contact search result item fields to entity fields`() {
    PrisonerContactSummary::class.memberProperties.forEach { property ->
      val mapped = mapSortPropertiesOfPrisonerContactSearch(property.name)
      assertThat(mapped).isNotNull()
      assertThat(PrisonerContactSummaryEntity::class.memberProperties.find { it.name == mapped }).isNotNull()
    }
  }

  @Test
  fun `attempting to sort on an invalid field for prisoner contact search gives an error`() {
    val expected = assertThrows<ValidationException> {
      mapSortPropertiesOfPrisonerContactSearch("foo")
    }
    assertThat(expected.message).isEqualTo("Unable to sort on foo")
  }
}
