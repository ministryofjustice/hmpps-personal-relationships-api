--
-- Creates a view over the organisation_address and reference data tables to return a list of all addresses with descriptions
-- for all codes.
-- Note: the view is only dropped if the checksum of this migration changes
-- Internal version to bump if you need to force recreation: 2
--
DROP VIEW IF EXISTS v_organisation_addresses;
CREATE VIEW v_organisation_addresses
AS
SELECT
    oa.organisation_address_id,
    oa.organisation_id,
    oa.address_type,
    atrc.description AS address_type_description,
    oa.primary_address,
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
    oa.mail_address,
    oa.service_address,
    oa.start_date,
    oa.end_date,
    oa.no_fixed_address,
    oa.comments,
    oa.special_needs_code,
    snrc.description AS special_needs_code_description,
    oa.contact_person_name,
    oa.business_hours,
    oa.created_by,
    oa.created_time,
    oa.updated_by,
    oa.updated_time
  FROM organisation_address oa
  LEFT JOIN reference_codes city_ref ON city_ref.group_code = 'CITY' AND city_ref.code = oa.city_code
  LEFT JOIN reference_codes county_ref ON county_ref.group_code = 'COUNTY' AND county_ref.code = oa.county_code
  LEFT JOIN reference_codes country_ref ON country_ref.group_code = 'COUNTRY' AND country_ref.code = oa.country_code
  LEFT JOIN reference_codes atrc ON atrc.group_code = 'ADDRESS_TYPE' AND atrc.code = oa.address_type
  LEFT JOIN reference_codes snrc ON snrc.group_code = 'ORG_ADDRESS_SPECIAL_NEEDS' AND snrc.code = oa.special_needs_code;

-- End