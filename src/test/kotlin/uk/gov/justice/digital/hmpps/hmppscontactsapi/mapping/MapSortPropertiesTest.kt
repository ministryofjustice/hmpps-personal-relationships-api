package uk.gov.justice.digital.hmpps.hmppscontactsapi.mapping

import jakarta.validation.ValidationException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.ContactWithAddressEntity
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.ContactSearchResultItem
import kotlin.reflect.full.memberProperties

class MapSortPropertiesTest {

  @Test
  fun `can map contact search result item fields to contact with address entity fields`() {
    ContactSearchResultItem::class.memberProperties.forEach { property ->
      val mapped = mapSortPropertiesOfContactSearch(property.name)
      assertThat(mapped).isNotNull()
      assertThat(ContactWithAddressEntity::class.memberProperties.find { it.name == mapped }).isNotNull()
    }
  }

  @Test
  fun `attempting to sort on an invalid field gives an error`() {
    val expected = assertThrows<ValidationException> {
      mapSortPropertiesOfContactSearch("foo")
    }
    assertThat(expected.message).isEqualTo("Unable to sort on foo")
  }
}
