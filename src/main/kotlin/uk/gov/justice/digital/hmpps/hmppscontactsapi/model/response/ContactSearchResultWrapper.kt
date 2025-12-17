package uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response

import org.springframework.data.domain.Page

data class ContactSearchResultWrapper<T>(
  val page: Page<T>,
  val total: Long,
  val truncated: Boolean,
  val message: String? = null,
)
