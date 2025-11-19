package uk.gov.justice.digital.hmpps.hmppscontactsapi.audit

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.envers.RevisionEntity
import org.hibernate.envers.RevisionNumber
import org.hibernate.envers.RevisionTimestamp
import java.time.LocalDateTime

/**
 * Custom revision entity used by Hibernate Envers to store extra metadata (e.g. username) for each revision.
 * The table name will be used as the base revision table; audited entities will reference the revision id.
 */
@Entity
@Table(name = "rev_info")
@RevisionEntity(UsernameRevisionListener::class)
class RevisionMetadata(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @RevisionNumber
  val id: Long? = null,

  @RevisionTimestamp
  val timestamp: LocalDateTime = LocalDateTime.now(),

  @Column(name = "username")
  var username: String? = null,
)
