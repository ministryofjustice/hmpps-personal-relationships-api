--
-- Creates a view over the contact_phone and reference_codes tables to return a list of all phone numbers with descriptions
-- for all codes.
-- Note: the view is only dropped if the checksum of this migration changes
-- Internal version to bump if you need to force recreation: 2
--
DROP VIEW IF EXISTS v_contact_phone_numbers;
CREATE VIEW v_contact_phone_numbers
AS
select
    cp.contact_phone_id,
    cp.contact_id,
    cp.phone_type,
    rc.description as phone_type_description,
    cp.phone_number,
    cp.ext_number,
    cp.created_by,
    cp.created_time,
    cp.updated_by,
    cp.updated_time
  from contact_phone cp
  left join reference_codes rc ON rc.group_code = 'PHONE_TYPE' and rc.code = cp.phone_type;

-- End