--
-- Creates a view over the contact_restriction, prisoner_contact_restriction and reference data tables to return a count
-- of restrictions by type and if they have expired. Intended to be used against prisoner_contact_ids for a summary of
-- restrictions.
-- Note: the view is only dropped if the checksum of this migration changes
-- Internal version to bump if you need to force recreation: 1
--
DROP VIEW IF EXISTS v_restriction_counts;
CREATE VIEW v_restriction_counts
AS
SELECT summed_restrictions.prisoner_contact_id,
       summed_restrictions.restriction_type,
       rc.description AS restriction_type_description,
       summed_restrictions.expired,
       summed_restrictions.number_of_restrictions
FROM (SELECT combined_restrictions.prisoner_contact_id,
             combined_restrictions.restriction_type,
             combined_restrictions.expired,
             count(1) AS number_of_restrictions
      FROM (SELECT pcr.prisoner_contact_id,
                   pcr.restriction_type,
                   CASE
                       WHEN pcr.expiry_date IS NOT NULL AND pcr.expiry_date < current_date
                           THEN TRUE
                       ELSE FALSE END AS expired
            FROM prisoner_contact_restriction pcr
            UNION ALL
            SELECT pc.prisoner_contact_id,
                   cr.restriction_type,
                   CASE
                       WHEN cr.expiry_date IS NOT NULL AND cr.expiry_date < current_date
                           THEN TRUE
                       ELSE FALSE END AS expired
            FROM contact_restriction cr,
                 prisoner_contact pc
            WHERE pc.contact_id = cr.contact_id) AS combined_restrictions
      GROUP BY prisoner_contact_id, restriction_type, expired) AS summed_restrictions
         LEFT JOIN reference_codes rc
                   ON rc.group_code = 'RESTRICTION' and rc.code = summed_restrictions.restriction_type;

-- End