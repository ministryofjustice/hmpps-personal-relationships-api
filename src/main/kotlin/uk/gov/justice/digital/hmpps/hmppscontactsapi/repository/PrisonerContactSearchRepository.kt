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
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.PrisonerContactSummaryEntity
import uk.gov.justice.digital.hmpps.hmppscontactsapi.mapping.mapSortPropertiesOfPrisonerContactSearch
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.internal.PrisonerContactSearchParams

@Repository
class PrisonerContactSearchRepository(
  @PersistenceContext
  private var entityManager: EntityManager,
) {

  fun searchPrisonerContacts(params: PrisonerContactSearchParams): Page<PrisonerContactSummaryEntity> {
    val cb = entityManager.criteriaBuilder
    val cq = cb.createQuery(PrisonerContactSummaryEntity::class.java)
    val contact = cq.from(PrisonerContactSummaryEntity::class.java)

    val predicates: List<Predicate> = buildPredicates(params, cb, contact)
    val pageable = params.pageable

    cq.where(*predicates.toTypedArray())

    applySorting(pageable, cq, cb, contact)

    val resultList = entityManager.createQuery(cq)
      .setFirstResult(pageable.offset.toInt())
      .setMaxResults(pageable.pageSize)
      .resultList

    val total = getTotalCount(params)

    return PageImpl(resultList, pageable, total)
  }

  private fun getTotalCount(
    request: PrisonerContactSearchParams,
  ): Long {
    val cb = entityManager.criteriaBuilder
    val countQuery = cb.createQuery(Long::class.java)
    val contact = countQuery.from(PrisonerContactSummaryEntity::class.java)

    val predicates: List<Predicate> = buildPredicates(request, cb, contact)

    countQuery.select(cb.count(contact)).where(*predicates.toTypedArray<Predicate>())
    return entityManager.createQuery(countQuery).singleResult
  }

  private fun applySorting(
    pageable: Pageable,
    cq: CriteriaQuery<PrisonerContactSummaryEntity>,
    cb: CriteriaBuilder,
    contact: Root<PrisonerContactSummaryEntity>,
  ) {
    if (pageable.sort.isSorted) {
      val order = pageable.sort.map {
        val property = mapSortPropertiesOfPrisonerContactSearch(it.property)
        var order: Order = if (it.isAscending) {
          cb.asc(contact.get<String>(property))
        } else {
          cb.desc(contact.get<String>(property))
        }
        // order date of birth with nulls as if they are the eldest.
        if (property == PrisonerContactSummaryEntity::dateOfBirth.name && order is JpaOrder) {
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
    params: PrisonerContactSearchParams,
    cb: CriteriaBuilder,
    contact: Root<PrisonerContactSummaryEntity>,
  ): MutableList<Predicate> {
    val predicates: MutableList<Predicate> = ArrayList()
    predicates.add(cb.equal(contact.get<String>(PrisonerContactSummaryEntity::prisonerNumber.name), params.prisonerNumber))
    if (params.active != null) {
      predicates.add(cb.equal(contact.get<Boolean>(PrisonerContactSummaryEntity::active.name), params.active))
    }
    if (params.emergencyContact != null) {
      predicates.add(cb.equal(contact.get<Boolean>(PrisonerContactSummaryEntity::emergencyContact.name), params.emergencyContact))
    }
    if (params.nextOfKin != null) {
      predicates.add(cb.equal(contact.get<Boolean>(PrisonerContactSummaryEntity::nextOfKin.name), params.nextOfKin))
    }
    if (params.emergencyContactOrNextOfKin != null) {
      if (params.emergencyContactOrNextOfKin) {
        predicates.add(
          cb.or(
            cb.equal(contact.get<Boolean>(PrisonerContactSummaryEntity::emergencyContact.name), true),
            cb.equal(contact.get<Boolean>(PrisonerContactSummaryEntity::nextOfKin.name), true),
          ),
        )
      } else {
        predicates.add(
          cb.and(
            cb.equal(contact.get<Boolean>(PrisonerContactSummaryEntity::emergencyContact.name), false),
            cb.equal(contact.get<Boolean>(PrisonerContactSummaryEntity::nextOfKin.name), false),
          ),
        )
      }
    }
    params.relationshipType?.let {
      predicates.add(cb.equal(contact.get<String>(PrisonerContactSummaryEntity::relationshipType.name), params.relationshipType))
    }
    return predicates
  }
}
