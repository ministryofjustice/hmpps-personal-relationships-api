--
-- Creates a view over the prisoner_contact_restriction and reference data tables to return a list of prisoner-contact restrictions by
-- contact_id
-- Note: the view is only dropped if the checksum of this migration changes
-- Internal version to bump if you need to force recreation: 2
--
DROP VIEW IF EXISTS v_prisoner_contact_restriction_details;
CREATE VIEW v_prisoner_contact_restriction_details
AS
select
    pcr.prisoner_contact_restriction_id,
    pcr.prisoner_contact_id,
    pcr.restriction_type,
    rc.description as restriction_type_description,
    pcr.start_date,
    pcr.expiry_date,
    pcr.comments,
    pcr.created_by,
    pcr.created_time,
    pcr.updated_by,
    pcr.updated_time
  from prisoner_contact_restriction pcr
  left join reference_codes rc ON rc.group_code = 'RESTRICTION' and rc.code = pcr.restriction_type;

-- End