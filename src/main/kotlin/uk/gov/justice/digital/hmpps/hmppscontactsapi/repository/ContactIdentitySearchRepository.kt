package uk.gov.justice.digital.hmpps.hmppscontactsapi.repository

import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import org.hibernate.query.criteria.HibernateCriteriaBuilder
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.ContactEntity
import java.time.LocalDate

@Repository
class ContactIdentitySearchRepository(
  @PersistenceContext
  private var entityManager: EntityManager,
) {
  companion object {
    private const val MAX_CONTACT_ID_LENGTH = 20
  }

  fun searchContactsByIdPartialMatch(
    contactId: String,
    dateOfBirth: LocalDate?,
    pageable: Pageable,
  ): Page<ContactEntity> {
    val cb = entityManager.criteriaBuilder
    val cq = cb.createQuery(ContactEntity::class.java)
    val contact = cq.from(ContactEntity::class.java)

    val sanitized = sanitizeContactId(contactId)
    val predicates = buildPredicatesForContactId(sanitized, dateOfBirth, cb, contact)

    cq.where(*predicates.toTypedArray())

    val results = entityManager.createQuery(cq)
      .setFirstResult(pageable.offset.toInt())
      .setMaxResults(pageable.pageSize)
      .resultList

    val total = countBySanitizedContactId(sanitized, dateOfBirth)
    return PageImpl(results, pageable, total)
  }

  private fun countBySanitizedContactId(sanitizedContactId: String, dateOfBirth: LocalDate?): Long {
    val cb = entityManager.criteriaBuilder
    val countQuery = cb.createQuery(Long::class.java)
    val contact = countQuery.from(ContactEntity::class.java)

    val predicates = buildPredicatesForContactId(sanitizedContactId, dateOfBirth, cb, contact)
    countQuery.select(cb.count(contact)).where(*predicates.toTypedArray())
    return entityManager.createQuery(countQuery).singleResult
  }

  // Helper: centralises sanitisation logic and length cap
  private fun sanitizeContactId(contactId: String): String = contactId.filter(Char::isDigit).take(MAX_CONTACT_ID_LENGTH)

  // Helper: builds predicates for a sanitized contact id (or an always-false predicate if empty)
  private fun buildPredicatesForContactId(
    sanitizedContactId: String,
    dateOfBirth: LocalDate?,
    cb: CriteriaBuilder,
    contact: Root<ContactEntity>,
  ): MutableList<Predicate> {
    if (cb !is HibernateCriteriaBuilder) {
      throw IllegalStateException("Configuration issue. Expected HibernateCriteriaBuilder but received ${cb::class.qualifiedName ?: cb.javaClass.name}")
    }
    val predicates: MutableList<Predicate> = ArrayList()

    if (sanitizedContactId.isEmpty()) {
      // Always-false predicate to avoid accidental broad matches when input contains no digits
      predicates.add(cb.disjunction())
      return predicates
    }

    val contactIdAsString = contactIdAsString(cb, contact)
    predicates.add(cb.ilike(contactIdAsString, "%$sanitizedContactId%", '#'))

    dateOfBirth?.let {
      predicates.add(
        cb.equal(
          contact.get<LocalDate>("dateOfBirth"),
          dateOfBirth,
        ),
      )
    }

    return predicates
  }

  // Helper: explicit conversion of contactId to string expression - kept as a small helper for clarity
  private fun contactIdAsString(cb: CriteriaBuilder, contact: Root<ContactEntity>) = cb.function("str", String::class.java, contact.get<Long>("contactId"))
}
