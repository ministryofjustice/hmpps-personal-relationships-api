--
-- Creates a view over the contact_address and reference data tables to return a list of all addresses with descriptions
-- for all codes.
-- Note: the view is only dropped if the checksum of this migration changes
-- Internal version to bump if you need to force recreation: 4
DROP VIEW IF EXISTS v_contact_addresses;
CREATE VIEW v_contact_addresses
AS
select
    ca.contact_address_id,
    ca.contact_id,
    ca.address_type,
    rc.description as address_type_description,
    ca.primary_address,
    ca.flat,
    ca.property,
    ca.street,
    ca.area,
    ca.city_code,
    city_ref.description as city_description,
    ca.county_code,
    county_ref.description as county_description,
    ca.post_code,
    ca.country_code,
    country_ref.description as country_description,
    ca.verified,
    ca.verified_by,
    ca.verified_time,
    ca.mail_flag,
    ca.start_date,
    ca.end_date,
    ca.no_fixed_address,
    ca.comments,
    ca.created_by,
    ca.created_time,
    ca.updated_by,
    ca.updated_time
  from contact_address ca
  left join reference_codes city_ref ON city_ref.group_code = 'CITY' and city_ref.code = ca.city_code
  left join reference_codes county_ref ON county_ref.group_code = 'COUNTY' and county_ref.code = ca.county_code
  left join reference_codes country_ref ON country_ref.group_code = 'COUNTRY' and country_ref.code = ca.country_code
  left join reference_codes rc ON rc.group_code = 'ADDRESS_TYPE' and rc.code = ca.address_type;

-- End