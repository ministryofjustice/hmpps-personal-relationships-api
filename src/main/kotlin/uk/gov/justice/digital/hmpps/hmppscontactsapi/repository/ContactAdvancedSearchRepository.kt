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
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.ContactEntity
import uk.gov.justice.digital.hmpps.hmppscontactsapi.mapping.mapSortPropertiesOfContactSearch
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.AdvancedContactSearchRequest
import java.time.LocalDate

@Repository
class ContactAdvancedSearchRepository(
  @PersistenceContext
  private val entityManager: EntityManager,
) {
  fun likeSearchContacts(request: AdvancedContactSearchRequest, pageable: Pageable): Page<ContactEntity> {
    val cb = entityManager.criteriaBuilder
    val cq = cb.createQuery(ContactEntity::class.java)
    val contact = cq.from(ContactEntity::class.java)

    val predicates: List<Predicate> = buildLikeSearchNamesPredicates(request, cb, contact)

    cq.where(*predicates.toTypedArray())

    applyContactSorting(pageable, cq, cb, contact)

    val resultList = entityManager.createQuery(cq)
      .setFirstResult(pageable.offset.toInt())
      .setMaxResults(pageable.pageSize)
      .resultList

    val total = getLikeSearchTotalContactsCount(request)

    return PageImpl(resultList, pageable, total)
  }

  private fun getLikeSearchTotalContactsCount(
    request: AdvancedContactSearchRequest,
  ): Long {
    val cb = entityManager.criteriaBuilder
    val countQuery = cb.createQuery(Long::class.java)
    val contact = countQuery.from(ContactEntity::class.java)

    val predicates: List<Predicate> = buildLikeSearchNamesPredicates(request, cb, contact)

    countQuery.select(cb.count(contact)).where(*predicates.toTypedArray<Predicate>())
    return entityManager.createQuery(countQuery).singleResult
  }

  private fun buildLikeSearchNamesPredicates(
    request: AdvancedContactSearchRequest,
    cb: CriteriaBuilder,
    contact: Root<ContactEntity>,
  ): MutableList<Predicate> {
    val predicates: MutableList<Predicate> = mutableListOf()
    if (cb !is HibernateCriteriaBuilder) {
      throw IllegalStateException("Configuration issue. Expected HibernateCriteriaBuilder but received ${cb::class.qualifiedName ?: cb.javaClass.name}")
    }
    predicates.add(cb.ilike(contact.get("lastName"), "%${request.lastName}%", '#'))
    request.firstName?.let {
      predicates.add(cb.ilike(contact.get("firstName"), "%$it%", '#'))
    }
    request.middleNames?.let {
      predicates.add(cb.ilike(contact.get("middleNames"), "%$it%", '#'))
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

  fun phoneticSearchContacts(request: AdvancedContactSearchRequest, pageable: Pageable): Page<ContactEntity> {
    val cb = entityManager.criteriaBuilder
    val cq = cb.createQuery(ContactEntity::class.java)
    val contact = cq.from(ContactEntity::class.java)

    val predicates: List<Predicate> = buildPhoneticSearchPredicates(request, cb, contact)

    cq.where(*predicates.toTypedArray())

    applyContactSorting(pageable, cq, cb, contact)

    val resultList = entityManager.createQuery(cq)
      .setFirstResult(pageable.offset.toInt())
      .setMaxResults(pageable.pageSize)
      .resultList

    val total = getPhoneticSearchTotalCount(request)

    return PageImpl(resultList, pageable, total)
  }

  private fun getPhoneticSearchTotalCount(
    request: AdvancedContactSearchRequest,
  ): Long {
    val cb = entityManager.criteriaBuilder
    val countQuery = cb.createQuery(Long::class.java)
    val contact = countQuery.from(ContactEntity::class.java)

    val predicates: List<Predicate> = buildPhoneticSearchPredicates(request, cb, contact)

    countQuery.select(cb.count(contact)).where(*predicates.toTypedArray<Predicate>())
    return entityManager.createQuery(countQuery).singleResult
  }

  private fun buildPhoneticSearchPredicates(
    request: AdvancedContactSearchRequest,
    cb: CriteriaBuilder,
    contact: Root<ContactEntity>,
  ): MutableList<Predicate> {
    val predicates: MutableList<Predicate> = mutableListOf()
    if (cb !is HibernateCriteriaBuilder) {
      throw IllegalStateException("Configuration issue. Expected HibernateCriteriaBuilder but received ${cb::class.qualifiedName ?: cb.javaClass.name}")
    }
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

  private fun applyContactSorting(
    pageable: Pageable,
    cq: CriteriaQuery<ContactEntity>,
    cb: CriteriaBuilder,
    contact: Root<ContactEntity>,
  ) {
    if (pageable.sort.isSorted) {
      val order = pageable.sort.map {
        val property = mapSortPropertiesOfContactSearch(it.property)
        var orderExpression: Order = if (it.isAscending) {
          cb.asc(contact.get<String>(property))
        } else {
          cb.desc(contact.get<String>(property))
        }
        // order date of birth with nulls as if they are the eldest.
        if (property == ContactEntity::dateOfBirth.name && orderExpression is JpaOrder) {
          orderExpression = if (it.isAscending) {
            orderExpression.nullPrecedence(NullPrecedence.FIRST)
          } else {
            orderExpression.nullPrecedence(NullPrecedence.LAST)
          }
        }
        orderExpression
      }.toList()
      cq.orderBy(order)
    }
  }
}
