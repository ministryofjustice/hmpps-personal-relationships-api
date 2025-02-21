--
-- Creates a view over the contact_identity and reference_codes tables to return a list of all identities with descriptions
-- for all codes.
-- Note: the view is only dropped if the checksum of this migration changes
-- Internal version to bump if you need to force recreation: 3
--
DROP VIEW IF EXISTS v_contact_identities;
CREATE VIEW v_contact_identities
AS
select
    ci.contact_identity_id,
    ci.contact_id,
    ci.identity_type,
    rc.description as identity_type_description,
    rc.is_active as identity_type_is_active,
    ci.identity_value,
    ci.issuing_authority,
    ci.created_by,
    ci.created_time,
    ci.updated_by,
    ci.updated_time
  from contact_identity ci
  left join reference_codes rc ON rc.group_code = 'ID_TYPE' and rc.code = ci.identity_type;

-- End