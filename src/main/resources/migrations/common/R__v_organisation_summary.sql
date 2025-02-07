--
-- Creates a view over the organisation, organisation_address and other associated tables to create a summary of an
-- organisation. This is intended to be used in search results and other places where only high level information is required.
-- Internal version to bump if you need to force recreation: 2
--
DROP VIEW IF EXISTS v_organisation_summary;
CREATE VIEW v_organisation_summary
AS
SELECT o.organisation_id,
       o.organisation_name,
       o.active AS organisation_active,
       oa.flat,
       oa.property,
       oa.street,
       oa.area,
       oa.city_code,
       city_ref.description AS city_description,
       oa.county_code,
       county_ref.description AS county_description,
       oa.post_code,
       oa.country_code,
       country_ref.description AS country_description,
       op.phone_number AS business_phone_number,
       op.ext_number AS business_phone_number_extension
FROM organisation o
        LEFT JOIN organisation_address oa ON oa.organisation_id = o.organisation_id AND primary_address = TRUE
        LEFT JOIN organisation_phone op ON op.organisation_phone_id = (
            SELECT op1.organisation_phone_id FROM organisation_phone op1
            JOIN organisation_address_phone oap ON oap.organisation_address_id = oa.organisation_address_id AND oap.organisation_phone_id = op1.organisation_phone_id
            WHERE op.phone_type = 'BUS'
            LIMIT 1
        )
        LEFT JOIN reference_codes city_ref ON city_ref.group_code = 'CITY' AND city_ref.code = oa.city_code
        LEFT JOIN reference_codes county_ref ON county_ref.group_code = 'COUNTY' AND county_ref.code = oa.county_code
        LEFT JOIN reference_codes country_ref ON country_ref.group_code = 'COUNTRY' AND country_ref.code = oa.country_code
        ORDER BY o.created_time DESC;

-- End