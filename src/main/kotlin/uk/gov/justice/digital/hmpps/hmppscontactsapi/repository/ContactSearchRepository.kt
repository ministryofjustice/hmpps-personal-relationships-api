package uk.gov.justice.digital.hmpps.hmppscontactsapi.repository

import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.Order
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import org.hibernate.query.NullPrecedence
import org.hibernate.query.criteria.HibernateCriteriaBuilder
import org.hibernate.query.criteria.JpaOrder
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.ContactWithAddressEntity
import uk.gov.justice.digital.hmpps.hmppscontactsapi.mapping.mapSortPropertiesOfContactSearch
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.ContactSearchRequest
import java.time.LocalDate

@Repository
class ContactSearchRepository(
  @PersistenceContext
  private var entityManager: EntityManager,
) {
  fun searchContacts(request: ContactSearchRequest, pageable: Pageable): Page<ContactWithAddressEntity> {
    val cb = entityManager.criteriaBuilder
    val cq = cb.createQuery(ContactWithAddressEntity::class.java)
    val contact = cq.from(ContactWithAddressEntity::class.java)

    val predicates: List<Predicate> = buildPredicates(request, cb, contact)

    cq.where(*predicates.toTypedArray())

    applySorting(pageable, cq, cb, contact)

    val resultList = entityManager.createQuery(cq)
      .setFirstResult(pageable.offset.toInt())
      .setMaxResults(pageable.pageSize)
      .resultList

    val total = getTotalCount(request)

    return PageImpl(resultList, pageable, total)
  }

  private fun getTotalCount(
    request: ContactSearchRequest,
  ): Long {
    val cb = entityManager.criteriaBuilder
    val countQuery = cb.createQuery(Long::class.java)
    val contact = countQuery.from(ContactWithAddressEntity::class.java)

    val predicates: List<Predicate> = buildPredicates(request, cb, contact)

    countQuery.select(cb.count(contact)).where(*predicates.toTypedArray<Predicate>())
    return entityManager.createQuery(countQuery).singleResult
  }

  private fun applySorting(
    pageable: Pageable,
    cq: CriteriaQuery<ContactWithAddressEntity>,
    cb: CriteriaBuilder,
    contact: Root<ContactWithAddressEntity>,
  ) {
    if (pageable.sort.isSorted) {
      val order = pageable.sort.map {
        val property = mapSortPropertiesOfContactSearch(it.property)
        var order: Order = if (it.isAscending) {
          cb.asc(contact.get<String>(property))
        } else {
          cb.desc(contact.get<String>(property))
        }
        // order date of birth with nulls as if they are the eldest.
        if (property == ContactWithAddressEntity::dateOfBirth.name && order is JpaOrder) {
          order = if (it.isAscending) {
            order.nullPrecedence(NullPrecedence.FIRST)
          } else {
            order.nullPrecedence(NullPrecedence.LAST)
          }
        }
        order
      }.toList()
      cq.orderBy(order)
    }
  }

  private fun buildPredicates(
    request: ContactSearchRequest,
    cb: CriteriaBuilder,
    contact: Root<ContactWithAddressEntity>,
  ): MutableList<Predicate> {
    val predicates: MutableList<Predicate> = ArrayList()
    if (cb !is HibernateCriteriaBuilder) {
      throw RuntimeException("Configuration issue. Cannot do ilike unless using hibernate.")
    }
    if (request.soundsLike) {
      val lastNameSoundex = cb.function("soundex", String::class.java, contact.get<String>("lastName"))
      val lastNameInputSoundex = cb.function("soundex", String::class.java, cb.literal(request.lastName))
      predicates.add(cb.equal(lastNameSoundex, lastNameInputSoundex))

      request.firstName?.let {
        val fnSoundex = cb.function("soundex", String::class.java, contact.get<String>("firstName"))
        val fnInputSoundex = cb.function("soundex", String::class.java, cb.literal(it))
        predicates.add(cb.equal(fnSoundex, fnInputSoundex))
      }
      request.middleNames?.let {
        val mnSoundex = cb.function("soundex", String::class.java, contact.get<String>("middleNames"))
        val mnInputSoundex = cb.function("soundex", String::class.java, cb.literal(it))
        predicates.add(cb.equal(mnSoundex, mnInputSoundex))
      }
    } else {
      predicates.add(cb.ilike(contact.get("lastName"), "%${request.lastName}%", '#'))
      request.firstName?.let {
        predicates.add(cb.ilike(contact.get("firstName"), "%$it%", '#'))
      }
      request.middleNames?.let {
        predicates.add(cb.ilike(contact.get("middleNames"), "%$it%", '#'))
      }
    }
    // partial match on contactId by converting to string
    request.contactId?.let {
      val contactIdAsString = cb.function("str", String::class.java, contact.get<Long>("contactId"))
      predicates.add(cb.like(contactIdAsString, "%$it%"))
    }
    request.dateOfBirth?.let {
      predicates.add(
        cb.equal(
          contact.get<LocalDate>("dateOfBirth"),
          request.dateOfBirth,
        ),
      )
    }

    return predicates
  }
}
