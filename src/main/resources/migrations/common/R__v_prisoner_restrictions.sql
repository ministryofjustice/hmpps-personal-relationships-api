--
-- Creates a view over the prisoner_restriction and reference data tables to return a list of prisoner restrictions by
-- prisoner_number
-- Note: the view is only dropped if the checksum of this migration changes
-- Internal version to bump if you need to force recreation: 1
--
DROP VIEW IF EXISTS v_prisoner_restriction_details;
CREATE VIEW v_prisoner_restriction_details
AS
select
    pr.prisoner_restriction_id,
    pr.prisoner_number,
    pr.restriction_type,
    rc.description as restriction_type_description,
    pr.effective_date,
    pr.expiry_date,
    pr.comment_text,
    pr.current_term,
    pr.authorised_username,
    pr.created_by,
    pr.created_time,
    pr.updated_by,
    pr.updated_time
  from prisoner_restrictions pr
  left join reference_codes rc ON rc.group_code = 'RESTRICTION' and rc.code = pr.restriction_type;

-- End