package uk.gov.justice.digital.hmpps.personalrelationships.config

import org.springframework.context.annotation.Configuration
import org.springframework.format.FormatterRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import uk.gov.justice.digital.hmpps.personalrelationships.mapping.ReferenceCodeGroupEnumConverter

@Configuration
class EnumMapperConfig : WebMvcConfigurer {

  override fun addFormatters(registry: FormatterRegistry) {
    registry.addConverter(ReferenceCodeGroupEnumConverter())
  }
}
