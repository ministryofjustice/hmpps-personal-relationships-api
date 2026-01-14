package uk.gov.justice.digital.hmpps.hmppscontactsapi.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.ContactEntity
import java.time.LocalDate

@Repository
interface ContactSearchRepositoryV2 : JpaRepository<ContactEntity, Long> {
  @Query(
    """
    select c.contactId 
    from ContactEntity c 
    where c.contactId = :contactId
    """,
  )
  fun findAllByContactIdEquals(contactId: Long, pageable: Pageable): Page<Long>

  @Query(
    """
    select c.contactId
    from ContactEntity c
    where c.dateOfBirth = :dateOfBirth
    """,
  )
  fun findAllByDateOfBirthEquals(dateOfBirth: LocalDate, pageable: Pageable): Page<Long>

  @Query(
    """
    select c.contactId
    from ContactEntity c
    where c.dateOfBirth = :dateOfBirth
      and (:lastName is null or c.lastName ilike %:lastName% escape '#')
      and (:firstName is null or c.firstName ilike %:firstName% escape '#')
      and (:middleNames is null or c.middleNames ilike %:middleNames% escape '#')
    """,
  )
  fun findAllByDateOfBirthAndNamesMatch(dateOfBirth: LocalDate, firstName: String?, middleNames: String?, lastName: String?, pageable: Pageable): Page<Long>

  @Query(
    """
    select c.contactId
    from ContactEntity c
    where c.dateOfBirth = :dateOfBirth
       and (:lastName is null or c.lastName ilike :lastName)
       and (:firstName is null or c.firstName ilike :firstName)
       and (:middleNames is null or c.middleNames ilike :middleNames)
    """,
  )
  fun findAllByDateOfBirthAndNamesExact(dateOfBirth: LocalDate, firstName: String?, middleNames: String?, lastName: String?, pageable: Pageable): Page<Long>

  @Query(
    """
    select c.contactId
    from ContactEntity c
    where c.dateOfBirth = :dateOfBirth
       and ( :lastName is null or c.lastNameSoundex = CAST(function('soundex', CAST(:lastName AS string)) AS char(4)))
       and ( :firstName is null or c.firstNameSoundex = CAST(function('soundex', CAST(:firstName AS string)) AS char(4)))
       and (:middleNames is null or c.middleNamesSoundex = CAST(function('soundex', CAST(:middleNames AS string)) AS char(4)))
    """,
  )
  fun findAllByDateOfBirthAndNamesSoundLike(dateOfBirth: LocalDate, firstName: String?, middleNames: String?, lastName: String?, pageable: Pageable): Page<Long>

  @Query(
    """
    select c.contactId
    from ContactEntity c
    where (:lastName is null or c.lastName ilike :lastName)
      and (:firstName is null or c.firstName ilike :firstName)
      and (:middleNames is null or c.middleNames ilike :middleNames)
    """,
  )
  fun findAllByNamesExact(firstName: String?, middleNames: String?, lastName: String?, pageable: Pageable): Page<Long>

  @Query(
    """
    select c.contactId
    from ContactEntity c
    where (:lastName is null or c.lastName ilike %:lastName% escape '#')
      and (:firstName is null or c.firstName ilike %:firstName% escape '#')
      and (:middleNames is null or c.middleNames ilike %:middleNames% escape '#')
    """,
  )
  fun findAllByNamesMatch(firstName: String?, middleNames: String?, lastName: String?, pageable: Pageable): Page<Long>

  @Query(
    """
    select c.contactId
    from ContactEntity c
    where (:lastName is null or c.lastNameSoundex = CAST(function('soundex', CAST(:lastName AS string)) AS char(4)))
      and (:firstName is null or c.firstNameSoundex = CAST(function('soundex', CAST(:firstName AS string)) AS char(4)))
      and (:middleNames is null or c.middleNamesSoundex = CAST(function('soundex', CAST(:middleNames AS string)) AS char(4)))
    """,
  )
  fun findAllByNamesSoundLike(firstName: String?, middleNames: String?, lastName: String?, pageable: Pageable): Page<Long>

  @Query(
    """
      select c.contactId
      from ContactEntity c where c.contactId in (
        select distinct ca.contactId
        from ContactAuditEntity ca
        where ca.revType in (0, 1)
        and (:lastName is null or ca.lastName ilike :lastName)
        and (:firstName is null or ca.firstName ilike :firstName)
        and (:middleNames is null or ca.middleNames ilike :middleNames)
    ) 
    """,
  )
  fun findAllByNamesExactAndHistory(firstName: String?, middleNames: String?, lastName: String?, pageable: Pageable): Page<Long>

  @Query(
    """
      select c.contactId
      from ContactEntity c where c.contactId in (
        select distinct ca.contactId
        from ContactAuditEntity ca
        where ca.revType in (0, 1)
        and (:lastName is null or ca.lastName ilike %:lastName% escape '#')
        and (:firstName is null or ca.firstName ilike %:firstName% escape '#')
        and (:middleNames is null or ca.middleNames ilike %:middleNames% escape '#')
    ) 
    """,
  )
  fun findAllByNamesMatchAndHistory(firstName: String?, middleNames: String?, lastName: String?, pageable: Pageable): Page<Long>

  @Query(
    """      
    select c.contactId
    from ContactEntity c
    where c.contactId in (
      select distinct ca.contactId
      from ContactAuditEntity ca
      where (:lastName is null or ca.lastNameSoundex = CAST(function('soundex', CAST(:lastName AS string)) AS char(4)))
        and (:firstName is null or ca.firstNameSoundex = CAST(function('soundex', CAST(:firstName AS string)) AS char(4)))
        and (:middleNames is null or ca.middleNamesSoundex = CAST(function('soundex', CAST(:middleNames AS string)) AS char(4)))
        and ca.revType in (0, 1)
    ) 
    """,
  )
  fun findAllByNamesSoundLikeAndHistory(firstName: String?, middleNames: String?, lastName: String?, pageable: Pageable): Page<Long>

  @Query(
    """
      select c.contactId
      from ContactEntity c
      where c.dateOfBirth = :dateOfBirth
      and c.contactId in (
        select distinct ca.contactId
        from ContactAuditEntity ca
        where (:lastName is null or ca.lastName ilike :lastName)
        and (:firstName is null or ca.firstName ilike :firstName)
        and (:middleNames is null or ca.middleNames ilike :middleNames)
        and ca.revType in (0, 1)
    )
    """,
  )
  fun findAllByDateOfBirthAndNamesExactAndHistory(dateOfBirth: LocalDate, firstName: String?, middleNames: String?, lastName: String?, pageable: Pageable): Page<Long>

  @Query(
    """
      select c.contactId
      from ContactEntity c
      where c.dateOfBirth = :dateOfBirth
      and c.contactId in (
        select distinct ca.contactId
        from ContactAuditEntity ca
        where (:lastName is null or ca.lastName ilike %:lastName% escape '#')
        and (:firstName is null or ca.firstName ilike %:firstName% escape '#')
        and (:middleNames is null or ca.middleNames ilike %:middleNames% escape '#')
        and ca.revType in (0, 1)
    )    
    """,
  )
  fun findAllByDateOfBirthAndNamesMatchAndHistory(dateOfBirth: LocalDate, firstName: String?, middleNames: String?, lastName: String?, pageable: Pageable): Page<Long>

  @Query(
    """      
    select c.contactId
    from ContactEntity c
    where c.dateOfBirth = :dateOfBirth
    and c.contactId in (
      select distinct ca.contactId
      from ContactAuditEntity ca
      where (:lastName is null or ca.lastNameSoundex =  CAST(function('soundex', CAST(:lastName AS string)) AS char(4)))
        and (:firstName is null or ca.firstNameSoundex = CAST(function('soundex', CAST(:firstName AS string)) AS char(4)))
        and (:middleNames is null or ca.middleNamesSoundex = CAST(function('soundex', CAST(:middleNames as string)) AS char(4)))
        and ca.revType in (0, 1)
    )
    """,
  )
  fun findAllByDateOfBirthAndNamesSoundLikeAndHistory(dateOfBirth: LocalDate, firstName: String?, middleNames: String?, lastName: String?, pageable: Pageable): Page<Long>
}
