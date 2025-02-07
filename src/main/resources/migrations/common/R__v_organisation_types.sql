--
-- Creates a view over the organisation_type and reference_codes tables to return a list of all org types with descriptions
-- for all codes.
-- Note: the view is only dropped if the checksum of this migration changes
-- Internal version to bump if you need to force recreation: 2
--
DROP VIEW IF EXISTS v_organisation_types;
CREATE VIEW v_organisation_types
AS
select
    ot.organisation_id,
    ot.organisation_type,
    rc.description as organisation_type_description,
    ot.created_by,
    ot.created_time,
    ot.updated_by,
    ot.updated_time
  from organisation_type ot
  left join reference_codes rc ON rc.group_code = 'ORGANISATION_TYPE' and rc.code = ot.organisation_type;

-- End