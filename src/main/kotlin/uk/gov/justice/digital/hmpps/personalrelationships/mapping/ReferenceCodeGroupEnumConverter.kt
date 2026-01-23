package uk.gov.justice.digital.hmpps.personalrelationships.mapping

import org.springframework.core.convert.converter.Converter
import uk.gov.justice.digital.hmpps.personalrelationships.exception.InvalidReferenceCodeGroupException
import uk.gov.justice.digital.hmpps.personalrelationships.model.ReferenceCodeGroup

class ReferenceCodeGroupEnumConverter : Converter<String, ReferenceCodeGroup> {
  override fun convert(source: String): ReferenceCodeGroup = ReferenceCodeGroup.entries.find { it.name === source } ?: throw InvalidReferenceCodeGroupException(source)
}
