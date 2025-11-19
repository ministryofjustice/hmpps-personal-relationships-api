package uk.gov.justice.digital.hmpps.hmppscontactsapi.audit

import org.hibernate.envers.RevisionListener
import org.springframework.security.core.context.SecurityContextHolder

/**
 * Populates the username field in the custom revision entity. Falls back to 'system' if no authentication.
 */
class UsernameRevisionListener : RevisionListener {
  override fun newRevision(revisionEntity: Any?) {
    val metadata = revisionEntity as? RevisionMetadata ?: return
    val auth = SecurityContextHolder.getContext().authentication
    metadata.username = auth?.name ?: "system"
  }
}
