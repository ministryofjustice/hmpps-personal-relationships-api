--
-- Creates a view over the contact, contact_address and prisoner_contact tables
-- to return a list of active or inactive contacts, and their primary addresses,
-- for a prisoner, where the current_term is true (latest booking only).
-- Note: the view is only dropped if the checksum of this migration changes
-- Internal version to bump if you need to force recreation: 3
--
DROP VIEW IF EXISTS v_prisoner_contacts;
CREATE VIEW v_prisoner_contacts
AS
  select
      c.contact_id,
      c.title,
      rc1.description as title_description,
      c.first_name,
      c.middle_names,
      c.last_name,
      c.date_of_birth,
      c.deceased_date,
      ca.contact_address_id,
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
      ca.primary_address,
      ca.mail_flag,
      cp.contact_phone_id,
      cp.phone_type,
      rc2.description as phone_type_description,
      cp.phone_number,
      cp.ext_number,
      ce.contact_email_id,
      ce.email_address,
      pc.prisoner_contact_id,
      pc.relationship_type,
      rc5.description as relationship_type_description,
      pc.prisoner_number,
      pc.relationship_to_prisoner,
      case
          when pc.relationship_type = 'S' then rc3.description
          when pc.relationship_type = 'O' then rc4.description
          else 'Unknown relationship type'
      end as relationship_to_prisoner_description,
      pc.active,
      pc.approved_visitor,
      pc.current_term,
      pc.next_of_kin,
      pc.emergency_contact,
      pc.comments
  from contact c
  join prisoner_contact pc ON pc.contact_id = c.contact_id
  left join contact_address ca ON ca.contact_address_id = (
      select contact_address_id from contact_address ca1
      where ca1.contact_id = c.contact_id
      and end_date is null
      order by ca1.primary_address desc, ca1.mail_flag desc, ca1.start_date desc nulls last, ca1.created_time desc
      limit 1
  )
  left join contact_phone cp ON cp.contact_phone_id = (
      select contact_phone_id from contact_phone cp1
      where cp1.contact_id = c.contact_id
      order by cp1.created_time desc
      limit 1
  )
  left join contact_email ce ON ce.contact_email_id = (
      select contact_email_id from contact_email ce1
      where ce1.contact_id = c.contact_id
      order by ce1.created_time desc
      limit 1
  )
  left join reference_codes rc1 ON rc1.group_code = 'TITLE' and rc1.code = c.title
  left join reference_codes rc2 ON rc2.group_code = 'PHONE_TYPE' and rc2.code = cp.phone_type
  left join reference_codes rc3 ON rc3.group_code = 'SOCIAL_RELATIONSHIP' and rc3.code = pc.relationship_to_prisoner
  left join reference_codes rc4 ON rc4.group_code = 'OFFICIAL_RELATIONSHIP' and rc4.code = pc.relationship_to_prisoner
  left join reference_codes rc5 ON rc5.group_code = 'RELATIONSHIP_TYPE' and rc5.code = pc.relationship_type
  left join reference_codes city_ref ON city_ref.group_code = 'CITY' and city_ref.code = ca.city_code
  left join reference_codes county_ref ON county_ref.group_code = 'COUNTY' and county_ref.code = ca.county_code
  left join reference_codes country_ref ON country_ref.group_code = 'COUNTRY' and country_ref.code = ca.country_code
  where pc.contact_id = c.contact_id
    and pc.current_term = true
  order by pc.created_time desc;

-- End