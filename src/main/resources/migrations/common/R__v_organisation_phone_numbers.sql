--
-- Creates a view over the organisation_phone and reference_codes tables to return a list of all phone numbers with descriptions
-- for all codes.
-- Note: the view is only dropped if the checksum of this migration changes
-- Internal version to bump if you need to force recreation: 2
--
DROP VIEW IF EXISTS v_organisation_phone_numbers;
CREATE VIEW v_organisation_phone_numbers
AS
select
    op.organisation_phone_id,
    op.organisation_id,
    op.phone_type,
    rc.description as phone_type_description,
    op.phone_number,
    op.ext_number,
    op.created_by,
    op.created_time,
    op.updated_by,
    op.updated_time
  from organisation_phone op
  left join reference_codes rc ON rc.group_code = 'PHONE_TYPE' and rc.code = op.phone_type;

-- End