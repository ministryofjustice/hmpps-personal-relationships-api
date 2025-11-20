package uk.gov.justice.digital.hmpps.hmppscontactsapi.repository

import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.hibernate.envers.AuditReaderFactory
import org.hibernate.envers.RevisionType
import org.hibernate.envers.query.AuditEntity
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppscontactsapi.audit.RevisionMetadata
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.ContactEntity
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.ContactAuditEntry

@Repository
class ContactAuditHistoryRepository(
  @PersistenceContext
  private var entityManager: EntityManager,
) {
  fun getContactHistory(contactId: Long): List<ContactAuditEntry>? {
    val reader = AuditReaderFactory.get(entityManager)

    @Suppress("UNCHECKED_CAST")
    val results = reader.createQuery().forRevisionsOfEntity(ContactEntity::class.java, false, true)
      .add(AuditEntity.id().eq(contactId))
      .addOrder(AuditEntity.revisionNumber().asc())
      .resultList as List<Array<Any>>

    return results.map { tuple ->
      val entity = tuple[0] as ContactEntity
      val revision = tuple[1] as RevisionMetadata
      val revisionType = tuple[2] as RevisionType
      ContactAuditEntry(
        revisionId = revision.id!!,
        revisionType = when (revisionType) {
          RevisionType.ADD -> "ADD"
          RevisionType.MOD -> "MOD"
          RevisionType.DEL -> "DEL"
        },
        revisionTimestamp = revision.timestamp,
        username = revision.username,
        id = entity.id(),
        titleCode = entity.title,
        lastName = entity.lastName,
        firstName = entity.firstName,
        middleNames = entity.middleNames,
        dateOfBirth = entity.dateOfBirth,
        deceasedDate = entity.deceasedDate,
        isStaff = entity.staffFlag,
        isRemitter = entity.remitterFlag,
        genderCode = entity.gender,
        domesticStatusCode = entity.domesticStatus,
        languageCode = entity.languageCode,
        interpreterRequired = entity.interpreterRequired,
        createdBy = entity.createdBy,
        createdTime = entity.createdTime,
        updatedBy = entity.updatedBy,
        updatedTime = entity.updatedTime,
      )
    }
  }
}
