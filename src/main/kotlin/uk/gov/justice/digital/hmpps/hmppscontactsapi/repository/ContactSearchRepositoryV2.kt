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
    order by c.lastName, c.firstName
    """,
  )
  fun findAllByDateOfBirthEquals(dateOfBirth: LocalDate, pageable: Pageable): Page<Long>

  @Query(
    """
    select c.contactId 
    from ContactEntity c 
    where c.dateOfBirth = :dateOfBirth and  
       (:lastName is null or c.lastName ilike %:lastName% escape '#') and
       (:firstName is null or c.firstName ilike %:firstName% escape '#') and
       (:middleNames is null or c.middleNames ilike %:middleNames% escape '#')
    order by c.lastName, c.firstName    
    """,
  )
  fun findAllByDateOfBirthAndNamesMatch(dateOfBirth: LocalDate, firstName: String?, middleNames: String?, lastName: String?, pageable: Pageable): Page<Long>

  @Query(
    """
    select c.contact_id 
    from contact c 
    where c.date_of_birth = :dateOfBirth and  
       (:lastName is null or c.last_name_soundex = soundex(:lastName)) and
       (:firstName is null or c.first_name_soundex = soundex(:firstName)) and
       (:middleNames is null or c.middle_names_soundex = soundex(:middleNames))
    order by c.last_name, c.first_name
    """,
    nativeQuery = true,
  )
  fun findAllByDateOfBirthAndNamesSoundLike(dateOfBirth: LocalDate, firstName: String?, middleNames: String?, lastName: String?, pageable: Pageable): Page<Long>

  @Query(
    """
    select c.contactId 
    from ContactEntity c 
    where (:lastName is null or c.lastName ilike %:lastName% escape '#') and
          (:firstName is null or c.firstName ilike %:firstName% escape '#') and
          (:middleNames is null or c.middleNames ilike %:middleNames% escape '#')
    order by c.lastName, c.firstName      
    """,
  )
  fun findAllByNamesMatch(firstName: String?, middleNames: String?, lastName: String?, pageable: Pageable): Page<Long>

  @Query(
    """
    select c.contact_id 
    from contact c 
    where (:lastName is null or c.last_name_soundex = soundex(:lastName)) and
          (:firstName is null or c.first_name_soundex = soundex(:firstName)) and
          (:middleNames is null or c.middle_names_soundex = soundex(:middleNames))
    order by c.last_name, c.first_name      
    """,
    nativeQuery = true,
  )
  fun findAllByNamesSoundLike(firstName: String?, middleNames: String?, lastName: String?, pageable: Pageable): Page<Long>

  @Query(
    """
      select c.contact_id
      from contact c where c.contact_id in (
        select distinct ca.contact_id 
        from contact_audit ca 
        where ca.rev_type in (0, 1) 
        and (:lastName is null or ca.last_name ilike %:lastName% escape '#') 
        and (:firstName is null or ca.first_name ilike %:firstName% escape '#') 
        and (:middleNames is null or ca.middle_names ilike %:middleNames% escape '#')
    )
    order by c.last_name, c.first_name      
    """,
    nativeQuery = true,
  )
  fun findAllByNamesMatchAndHistory(firstName: String?, middleNames: String?, lastName: String?, pageable: Pageable): Page<Long>

  @Query(
    """      
    select c.contact_id 
    from contact c
    where c.contact_id in (
      select distinct ca.contact_id
      from contact_audit ca
      where c.rev_type in (0, 1)
        and (
          (:lastName is null or ca.last_name_soundex = soundex(:lastName)) and
          (:firstName is null or ca.first_name_soundex = soundex(:firstName)) and
          (:middleNames is null or ca.middle_names_soundex = soundex(:middleNames))
        )
    )
    order by c.last_name, c.first_name      
    """,
    nativeQuery = true,
  )
  fun findAllByNamesSoundLikeAndHistory(firstName: String?, middleNames: String?, lastName: String?, pageable: Pageable): Page<Long>

  @Query(
    """
      select c.contact_id
      from contact c 
      where c.date_of_birth = :dateOfBirth
      and c.contact_id in (
        select distinct ca.contact_id 
        from contact_audit ca 
        where ca.rev_type in (0, 1) 
        and (:lastName is null or ca.last_name ilike %:lastName% escape '#') 
        and (:firstName is null or ca.first_name ilike %:firstName% escape '#') 
        and (:middleNames is null or ca.middle_names ilike %:middleNames% escape '#')
    )
    order by c.last_name, c.first_name      
    """,
    nativeQuery = true,
  )
  fun findAllByDateOfBirthAndNamesMatchAndHistory(dateOfBirth: LocalDate, firstName: String?, middleNames: String?, lastName: String?, pageable: Pageable): Page<Long>

  @Query(
    """      
    select c.contact_id 
    from contact c
    where c.date_of_birth = :dateOfBirth 
    and c.contact_id in (
      select distinct ca.contact_id
      from contact_audit ca
      where c.rev_type in (0, 1)
        and (
          (:lastName is null or ca.last_name_soundex = soundex(:lastName)) and
          (:firstName is null or ca.first_name_soundex = soundex(:firstName)) and
          (:middleNames is null or ca.middle_names_soundex = soundex(:middleNames))
        )
    )
    order by c.last_name, c.first_name      
    """,
    nativeQuery = true,
  )
  fun findAllByDateOfBirthAndNamesSoundLikeAndHistory(dateOfBirth: LocalDate, firstName: String?, middleNames: String?, lastName: String?, pageable: Pageable): Page<Long>
}
