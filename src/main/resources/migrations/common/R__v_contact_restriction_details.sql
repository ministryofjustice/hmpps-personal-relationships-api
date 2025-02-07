--
-- Creates a view over the contact_restriction and reference data tables to return a list of contact global restrictions by
-- contact_id
-- Note: the view is only dropped if the checksum of this migration changes
-- Internal version to bump if you need to force recreation: 2
--
DROP VIEW IF EXISTS v_contact_restriction_details;
CREATE VIEW v_contact_restriction_details
AS
select
    cr.contact_restriction_id,
    cr.contact_id,
    cr.restriction_type,
    rc.description as restriction_type_description,
    cr.start_date,
    cr.expiry_date,
    cr.comments,
    cr.created_by,
    cr.created_time,
    cr.updated_by,
    cr.updated_time
  from contact_restriction cr
  left join reference_codes rc ON rc.group_code = 'RESTRICTION' and rc.code = cr.restriction_type;

-- End