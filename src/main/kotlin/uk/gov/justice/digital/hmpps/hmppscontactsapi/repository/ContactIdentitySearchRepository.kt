package uk.gov.justice.digital.hmpps.hmppscontactsapi.repository

import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.Order
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import org.hibernate.query.NullPrecedence
import org.hibernate.query.criteria.JpaOrder
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.ContactEntity
import uk.gov.justice.digital.hmpps.hmppscontactsapi.mapping.mapSortPropertiesOfContactSearch
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

    applyContactSorting(pageable, cq, cb, contact)

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

  private fun applyContactSorting(
    pageable: Pageable,
    cq: CriteriaQuery<ContactEntity>,
    cb: CriteriaBuilder,
    contact: Root<ContactEntity>,
  ) {
    if (!pageable.sort.isSorted) return

    val orders: List<Order> = pageable.sort.map { sort ->
      val property = mapSortPropertiesOfContactSearch(sort.property)
      var order: Order =
        if (sort.isAscending) cb.asc(contact.get<String>(property)) else cb.desc(contact.get<String>(property))

      if (property == ContactEntity::dateOfBirth.name && order is JpaOrder) {
        order = if (sort.isAscending) {
          order.nullPrecedence(NullPrecedence.FIRST)
        } else {
          order.nullPrecedence(
            NullPrecedence.LAST,
          )
        }
      }

      order
    }.toList()

    cq.orderBy(orders)
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
    val predicates: MutableList<Predicate> = ArrayList()

    if (sanitizedContactId.isEmpty()) {
      // Always-false predicate to avoid accidental broad matches when input contains no digits
      predicates.add(cb.disjunction())
      return predicates
    }

    val contactIdAsString = contactIdAsString(cb, contact)
    predicates.add(cb.like(contactIdAsString, "%$sanitizedContactId%"))

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
