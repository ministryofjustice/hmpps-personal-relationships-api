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
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.ContactAuditPk
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.ContactEntity
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.ContactEntityAudit
import uk.gov.justice.digital.hmpps.hmppscontactsapi.mapping.mapSortPropertiesOfContactSearch
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.AdvancedContactSearchRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.ContactSearchResultWrapper
import java.time.LocalDate

@Repository
class ContactAdvancedSearchRepository(
  @PersistenceContext
  private val entityManager: EntityManager,
) {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
    private const val MAX_CANDIDATE_IDS = 1000
    private const val HARD_CAP_THRESHOLD = 500
    private const val TOO_MANY_RESULTS = "Your search returned a large number of results. Only the top 500 are shown. Refine your search to narrow the results."
  }

  fun likeSearchContacts(
    request: AdvancedContactSearchRequest,
    pageable: Pageable,
  ): ContactSearchResultWrapper<ContactEntity> {
    val cb = entityManager.criteriaBuilder
    if (cb !is HibernateCriteriaBuilder) {
      throw IllegalStateException("Configuration issue. Expected HibernateCriteriaBuilder but received ${cb::class.qualifiedName ?: cb.javaClass.name}")
    }

    // 1) collect matching contactIds from the contact table
    val contactIdQuery = cb.createQuery(Long::class.java)
    val contactRootForIds = contactIdQuery.from(ContactEntity::class.java)
    val contactIdPreds = mutableListOf<Predicate>()
    contactIdPreds.add(cb.ilike(contactRootForIds.get("lastName"), "%${request.lastName}%", '#'))
    request.firstName?.let {
      contactIdPreds.add(cb.ilike(contactRootForIds.get("firstName"), "%$it%", '#'))
    }
    request.middleNames?.let {
      contactIdPreds.add(cb.ilike(contactRootForIds.get("middleNames"), "%$it%", '#'))
    }
    request.dateOfBirth?.let {
      contactIdPreds.add(cb.equal(contactRootForIds.get<LocalDate>("dateOfBirth"), it))
    }
    contactIdQuery.select(contactRootForIds.get("contactId")).where(*contactIdPreds.toTypedArray())
    val contactIdsFromContact = entityManager
      .createQuery(contactIdQuery)
      .setMaxResults(MAX_CANDIDATE_IDS)
      .resultList
    log.info("likeSearchContacts: found ${contactIdsFromContact.size} matching contact IDs from ContactEntity table")

    // 2) collect matching contactIds from the audit table (no EXISTS / JOIN)
    val auditIdQuery = cb.createQuery(Long::class.java)
    val auditRootForIds = auditIdQuery.from(ContactEntityAudit::class.java)
    val auditPreds = mutableListOf<Predicate>()
    auditPreds.add(cb.ilike(auditRootForIds.get("lastName"), "%${request.lastName}%", '#'))
    request.firstName?.let {
      auditPreds.add(cb.ilike(auditRootForIds.get("firstName"), "%$it%", '#'))
    }
    request.middleNames?.let {
      auditPreds.add(cb.ilike(auditRootForIds.get("middleNames"), "%$it%", '#'))
    }
    request.dateOfBirth?.let {
      auditPreds.add(cb.equal(auditRootForIds.get<LocalDate>("dateOfBirth"), it))
    }
    auditIdQuery.select(auditRootForIds.get<ContactAuditPk>("id").get<Long>("contactId"))
      .where(*auditPreds.toTypedArray())
    val contactIdsFromAudit = entityManager
      .createQuery(auditIdQuery)
      .setMaxResults(MAX_CANDIDATE_IDS)
      .resultList
    log.info("likeSearchContacts: found ${contactIdsFromAudit.size} matching contact IDs from ContactAuditEntity table")

    // 3) merge ids and apply optional DOB filter later against contacts
    var combinedIds = (contactIdsFromContact + contactIdsFromAudit).toSet().toList()
    if (combinedIds.isEmpty()) {
      return ContactSearchResultWrapper(
        page = PageImpl(emptyList(), pageable, 0),
        total = 0,
        truncated = false,
        message = null,
      )
    }

    var isTruncated = false
    var truncationMessage: String? = null
    if (combinedIds.size > HARD_CAP_THRESHOLD) {
      log.warn("likeSearchContacts: Large result set of ${combinedIds.size} contact IDs found. Applying hard cap of $HARD_CAP_THRESHOLD.'")
      isTruncated = true
      truncationMessage = TOO_MANY_RESULTS
      combinedIds = combinedIds.take(HARD_CAP_THRESHOLD)
    }

    // 4) final query: fetch contacts by id list, apply DOB filter and sorting, with paging
    val finalCb = entityManager.criteriaBuilder
    val finalCq = finalCb.createQuery(ContactEntity::class.java)
    val contact = finalCq.from(ContactEntity::class.java)

    val finalPreds = mutableListOf<Predicate>()
    finalPreds.add(contact.get<Long>("contactId").`in`(combinedIds))
    request.dateOfBirth?.let {
      finalPreds.add(finalCb.equal(contact.get<LocalDate>("dateOfBirth"), it))
    }

    finalCq.select(contact).distinct(true).where(*finalPreds.toTypedArray())

    applyContactSorting(pageable, finalCq, finalCb, contact)

    val query = entityManager.createQuery(finalCq)
      .setFirstResult(pageable.offset.toInt())
      .setMaxResults(pageable.pageSize)

    val resultList = query.resultList
    val total = combinedIds.size.toLong()
    val pageResult = PageImpl(resultList, pageable, total)

    return ContactSearchResultWrapper(
      page = pageResult,
      total = total,
      truncated = isTruncated,
      message = truncationMessage,
    )
  }

  fun phoneticSearchContacts(
    request: AdvancedContactSearchRequest,
    pageable: Pageable,
  ): ContactSearchResultWrapper<ContactEntity> {
    val cb = entityManager.criteriaBuilder
    if (cb !is HibernateCriteriaBuilder) {
      throw IllegalStateException("Configuration issue. Expected HibernateCriteriaBuilder but received ${cb::class.qualifiedName ?: cb.javaClass.name}")
    }

    // 1) collect matching contactIds from the contact table (phonetic)
    val contactIdQuery = cb.createQuery(Long::class.java)
    val contactRootForIds = contactIdQuery.from(ContactEntity::class.java)
    val contactIdPreds = buildPhoneticPredicates(request, cb, contactRootForIds)
    contactIdQuery.select(contactRootForIds.get("contactId")).where(*contactIdPreds.toTypedArray())
    val contactIdsFromContact = entityManager.createQuery(contactIdQuery).resultList

    // 2) collect matching contactIds from the audit table (phonetic, no EXISTS / JOIN)
    val auditIdQuery = cb.createQuery(Long::class.java)
    val auditRootForIds = auditIdQuery.from(ContactEntityAudit::class.java)
    val auditPreds = buildPhoneticPredicates(request, cb, auditRootForIds)
    auditIdQuery.select(auditRootForIds.get<ContactAuditPk>("id").get<Long>("contactId"))
      .where(*auditPreds.toTypedArray())
    val contactIdsFromAudit = entityManager.createQuery(auditIdQuery).resultList

    // 3) merge ids
    val combinedIds = (contactIdsFromContact + contactIdsFromAudit).toSet().toList()
    if (combinedIds.isEmpty()) {
      return ContactSearchResultWrapper(
        page = PageImpl(emptyList(), pageable, 0),
        total = 0,
        truncated = false,
        message = null,
      )
    }

    // 4) final query: fetch contacts by id list, apply DOB filter and sorting, with paging
    val finalCb = entityManager.criteriaBuilder
    val finalCq = finalCb.createQuery(ContactEntity::class.java)
    val contact = finalCq.from(ContactEntity::class.java)

    val finalPreds = mutableListOf<Predicate>()
    finalPreds.add(contact.get<Long>("contactId").`in`(combinedIds))
    request.dateOfBirth?.let {
      finalPreds.add(finalCb.equal(contact.get<LocalDate>("dateOfBirth"), it))
    }

    finalCq.select(contact).distinct(true).where(*finalPreds.toTypedArray())

    applyContactSorting(pageable, finalCq, finalCb, contact)

    val query = entityManager.createQuery(finalCq)
      .setFirstResult(pageable.offset.toInt())
      .setMaxResults(pageable.pageSize)

    val resultList = query.resultList
    val total = combinedIds.size.toLong()
    val pageResult = PageImpl(resultList, pageable, total)

    return ContactSearchResultWrapper(
      page = pageResult,
      total = total,
      truncated = false,
      message = null,
    )
  }

  private fun buildPhoneticPredicates(
    request: AdvancedContactSearchRequest,
    cb: CriteriaBuilder,
    root: Root<*>,
  ): MutableList<Predicate> {
    val predicates: MutableList<Predicate> = mutableListOf()
    if (cb !is HibernateCriteriaBuilder) {
      throw IllegalStateException("Configuration issue. Expected HibernateCriteriaBuilder but received ${cb::class.qualifiedName ?: cb.javaClass.name}")
    }

    // Soundex comparison for lastName (required)
    val lastNameSoundex = cb.function("soundex", String::class.java, root.get<String>("lastName"))
    val lastNameInputSoundex = cb.function("soundex", String::class.java, cb.literal(request.lastName))
    predicates.add(cb.equal(lastNameSoundex, lastNameInputSoundex))

    // Soundex comparison for firstName (optional)
    request.firstName?.let {
      val fnSoundex = cb.function("soundex", String::class.java, root.get<String>("firstName"))
      val fnInputSoundex = cb.function("soundex", String::class.java, cb.literal(it))
      predicates.add(cb.equal(fnSoundex, fnInputSoundex))
    }

    // Soundex comparison for middleNames (optional)
    request.middleNames?.let {
      val mnSoundex = cb.function("soundex", String::class.java, root.get<String>("middleNames"))
      val mnInputSoundex = cb.function("soundex", String::class.java, cb.literal(it))
      predicates.add(cb.equal(mnSoundex, mnInputSoundex))
    }

    request.dateOfBirth?.let {
      predicates.add(cb.equal(root.get<LocalDate>("dateOfBirth"), it))
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
